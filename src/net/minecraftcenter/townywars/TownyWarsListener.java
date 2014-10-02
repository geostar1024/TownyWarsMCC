package net.minecraftcenter.townywars;

import net.minecraftcenter.townywars.object.TownyWarsNation;
import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;

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

		// derate the town's starting DP based on the number of times it's been conquered so far in the wars it's been in
		town.resetDP();
	}

	@EventHandler
	public void onNationDelete(final DeleteNationEvent event) {
		final TownyWarsNation nation = TownyWarsNation.getNation(event.getNationName());

		// this nation needs to get removed from any wars it was in
		for (final War war : nation.getWars()) {
			war.end();
		}

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
		// we need to see if this was the last town removed, in which case, the nation ceases to exist
		if (event.getNation().getTowns().isEmpty()) {
			System.out.println("nation getting deleted!");
			// update all the lastNation field for all players of this shortly-nonexistent nation
			for (final Resident resident : event.getNation().getResidents()) {
				TownyWarsResident.getResident(resident).setLastNation(null);
			}
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

		try {
			final TownyWarsNation nation = TownyWarsNation.getNation(event.getTown().getNation());

			final TownyWarsNation lastNation = resident.getLastNation();

			// if lastNation is null, we can skip checking wars
			if (lastNation != null) {

				// it's ok to rejoin the nation the player was last a part of
				if (nation != lastNation) {
					if (WarManager.getSharedWar(nation, lastNation) != null) {
						event.getTown().removeResident(event.getResident());
						resident.getPlayer().sendMessage(ChatColor.RED + "You cannot join a nation who is in a war you have already participated in!");
						return;
					}
				}

				// the player was allowed to join the new nation so update accordingly
				resident.setLastNation(nation);
			}
		} catch (final NotRegisteredException e) {
			// not a problem, the town isn't in a nation
		} catch (final EmptyTownException e) {
			System.out.println("odd. . .");
			e.printStackTrace();
		}
		// in the case that the player joined a town not in a nation, don't update lastNation for the player

		// be careful, because if this is a new town, it may not exist yet in the hashmap
		// if it doesn't, we'll put it in now
		if (TownyWarsTown.getTown(event.getTown()) == null) {
			TownyWarsTown.putTown(new TownyWarsTown(event.getTown()));
		}
		final TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
		final double oldMaxDP = town.getMaxDP();
		final double newMaxDP = town.calculateMaxDP();
		town.modifyDP(newMaxDP - oldMaxDP);
	}

	@EventHandler
	public void onResidentLeave(final TownRemoveResidentEvent event) {
		try {
			final TownyWarsNation nation = TownyWarsNation.getNation(event.getTown().getNation());
			if (nation.isInWar()) {
				final TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
				town.calculateMaxDP();
				final String message = ChatColor.RED + "Reminder: You cannot join another nation that is currently at war with the nation you've left!";
				for (final Resident resident : town.getTown().getResidents()) {
					final Player plr = Bukkit.getPlayer(resident.getName());
					if (plr != null) {
						plr.sendMessage(message);
					}
				}
				// check to see if this town ceasing to exist would cause any in-progress wars to stop; if so, end them
				if (event.getTown().getResidents().size() == 0) {
					for (final War war : town.getWars()) {
						war.end();
					}
				}
			}
		} catch (final NotRegisteredException ex) {
			// do nothing; evidently the town the resident left was not in a nation
		}
	}

	@EventHandler
	public void onTownDelete(final DeleteTownEvent event) {
		TownyWarsTown.getTown(event.getTownName()).remove();
	}

	// update the TownyWarsTown object if the linked Town object's name changes
	@EventHandler
	public void onTownNameChange(final RenameTownEvent event) {
		TownyWarsTown.getTown(event.getTown()).setName(event.getTown().getName());
	}

}
