package net.minecraftcenter.townywars;

import net.minecraftcenter.townywars.interfaces.Attackable;
import net.minecraftcenter.townywars.object.TownyWarsNation;
import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;
import net.minecraftcenter.townywars.object.War.WarStatus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

public class TownyWarsListener implements Listener {

	@EventHandler
	public void onNationAdd(final NationAddTownEvent event) {
		final TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
		TownyWarsNation nation = TownyWarsNation.getNation(event.getNation());
		// be careful, because if this is a new nation, it may not exist yet in the hashmap
		// if it doesn't, we'll put it in now
		if (nation == null) {
			TownyWarsNation.putNation(new TownyWarsNation(event.getNation()));
		}
		nation = TownyWarsNation.getNation(event.getNation());
		// derate the town's starting DP based on the number of times it's been conquered so far in the wars it's been in
		town.resetDP();
		for (TownyWarsResident resident : town.getResidents()) {
			resident.addOrg(nation);
			resident.save();
		}
	}

	@EventHandler
	public void onNationDelete(final DeleteNationEvent event) {
		final TownyWarsNation nation = TownyWarsNation.getNation(event.getNationName());

		for (final TownyWarsResident resident : nation.getResidents()) {
			resident.clearOrgs(false);
		}

		// this nation needs to get removed from any wars it was in
		for (final War war : nation.getWars()) {
			war.end();
		}
		for (final War war : nation.getStoredWars()) {
			War.removeWar(war);
		}
		nation.clearStoredWars();

		// save and remove the nation
		nation.remove();

	}

	// update the TownyWarsNation object if the linked Nation object's name changes
	@EventHandler
	public void onNationNameChange(final RenameNationEvent event) {
		TownyWarsNation.getNation(event.getNation()).setName(event.getNation().getName());
	}

	@EventHandler
	public void onNationRemove(final NationRemoveTownEvent event) {
		TownyWarsNation nation = TownyWarsNation.getNation(event.getNation());
		TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
		for (TownyWarsResident resident : town.getResidents()) {
			resident.removeOrg(nation);
		}
	}

	// TODO: currently never executes; awaiting fix from Towny developer

	/*
	 * @EventHandler public void onNationCreation(NewNationEvent event) { TownyWarsNation.putNation(event.getNation()); }
	 */

	@EventHandler
	public void onResidentAdd(final TownAddResidentEvent event) {
		// first we need to check if the town is in a nation that is in a war that the player has already been a part of
		// if so, remove the player from the town
		final TownyWarsResident resident = TownyWarsResident.getResident(event.getResident());
		TownyWarsResident.updateOrgs(resident);

		// iterate through the resident's current orgs (including the new town)
		// iterate through the wars that the resident's orgs are in, and see if the resident has been in those wars
		for (Attackable org : resident.getOrgs()) {
			for (War war : resident.getActiveWars()) {

				// the resident's activeWars field isn't updated when wars end for performance reasons, so get rid of the war now if it ended
				if (war.getWarStatus().equals(WarStatus.ENDED)) {
					resident.removeOldWar(war);
					continue;
				}
				if (org.hasWar(war)) {
					try {
						event.getTown().removeResident(event.getResident());
					} catch (NotRegisteredException | EmptyTownException e) {
						// weird, but shouldn't ever happen
						System.out.println("you shouldn't see this!");
					}
					resident.getPlayer().sendMessage(ChatColor.RED + "You cannot join a town/nation who is in a war you have already participated in!");
					return;
				}
			}
		}

		// be careful, because if this is a new town, it may not exist yet in the hashmap
		// if it doesn't, we'll put it in now
		if (TownyWarsTown.getTown(event.getTown()) == null) {
			TownyWarsTown.putTown(new TownyWarsTown(event.getTown()));
		}
		TownyWarsResident.updateOrgs(resident);
		final TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
		final double oldMaxDP = town.getMaxDP();
		final double newMaxDP = town.calculateMaxDP();
		town.modifyDP(newMaxDP - oldMaxDP);
		town.save();
		resident.save();
	}

	@EventHandler
	public void onResidentLeave(final TownRemoveResidentEvent event) {
		final TownyWarsResident resident = TownyWarsResident.getResident(event.getResident());
		final TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
		resident.removeOrg(town);
		try {
			final TownyWarsNation nation = TownyWarsNation.getNation(event.getTown().getNation());
			resident.removeOrg(nation);
			if (nation.isInWar()) {

				town.calculateMaxDP();
				final String message = ChatColor.RED + "Reminder: You cannot join another nation that is currently at war with the nation you've left!";
				for (final Resident re : town.getTown().getResidents()) {
					final Player plr = Bukkit.getPlayer(re.getName());
					if (plr != null) {
						plr.sendMessage(message);
					}
				}
				// check to see if this town ceasing to exist would cause any in-progress wars to stop; if so, end them
				if (event.getTown().getResidents().size() == 0) {
					for (final War war : town.getWars()) {
						war.end();
					}
					for (final War war : town.getStoredWars()) {
						War.removeWar(war);
					}
					town.clearStoredWars();
				}
			}
		} catch (final NotRegisteredException ex) {
			// do nothing; evidently the town the resident left was not in a nation
		}
		final double oldMaxDP = town.getMaxDP();
		final double newMaxDP = town.calculateMaxDP();
		town.modifyDP(newMaxDP - oldMaxDP);
		resident.save();
		town.save();
	}

	@EventHandler
	public void onTownDelete(final DeleteTownEvent event) {
		TownyWarsTown town = TownyWarsTown.getTown(event.getTownName());

		for (final TownyWarsResident resident : town.getResidents()) {
			resident.clearOrgs(false);
		}

		// this town needs to get removed from any wars it was in
		for (final War war : town.getWars()) {
			war.end();
		}
		for (final War war : town.getStoredWars()) {
			War.removeWar(war);
		}
		town.clearStoredWars();

		town.remove();
	}

	// update the TownyWarsTown object if the linked Town object's name changes
	@EventHandler
	public void onTownNameChange(final RenameTownEvent event) {
		TownyWarsTown.getTown(event.getTown()).setName(event.getTown().getName());
	}

}
