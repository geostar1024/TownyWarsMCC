package net.minecraftcenter.townywars;

import net.minecraftcenter.townywars.interfaces.Attackable;
import net.minecraftcenter.townywars.object.TownyWarsNation;
import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;
import net.minecraftcenter.townywars.object.War.WarStatus;
import net.minecraftcenter.townywars.object.War.WarType;

import org.bukkit.ChatColor;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class WarManager {

	//private static Set<War>	activeWars	= new HashSet<War>();

	// try to end the war of the given name
	// returns true if successful
	public static boolean endWar(final String warName) {
		if (warName == null) {
			return false;
		}
		for (final War war : War.getAllWars()) {
			if ((war.getName().compareTo(warName) == 0) && (war.getWarStatus() != WarStatus.ENDED)) {
				war.end();
				return true;
			}
		}
		return false;
	}

	// find out if two nations are in a war together
	public static War getSharedWar(final Attackable declarer, final Attackable target) {
		for (final War war : declarer.getWars()) {
			if (war.hasMember(target)) {
				return war;
			}
		}
		return null;
	}

	// handles the nitty gritty of conquest
	// this will trigger the Towny event NationRemoveTownEvent
	public static void moveTown(final TownyWarsTown town, final TownyWarsNation newNation) {
		TownyWarsNation oldNation = null;
		try {
			oldNation = TownyWarsNation.getNation(town.getTown().getNation());
		} catch (final NotRegisteredException e) {
			System.out.println("[TownyWars] moveTown: getting the town to move failed catastrophically!");
			e.printStackTrace();
		}

		// we'll need to know later if the town that's being moved was a capital
		final boolean wasCapital = town.getTown().isCapital();
		try {
			oldNation.getNation().removeTown(town.getTown());
			newNation.getNation().addTown(town.getTown());
		} catch (final Exception e) {
			System.out.println("[TownyWars] moveTown: moving the town failed catastrophically!");
			e.printStackTrace();
		}
		if (wasCapital) {
			// the old capital is no longer part of the town
			oldNation.removeCapitalPriorityForTown(town);

			// set the new capital to the first item in the capitalPriority list
			oldNation.getNation().setCapital(oldNation.getNextCapital().getTown());
		}

		// now increment the town's conquered state and reinitialize its DP
		town.addConquered();
	}

	public static boolean quickRaid(final Attackable declarer, final Attackable target) {

		if (WarManager.getSharedWar(declarer, target) != null) {
			return false;
		}

		// make a default name using the declarer's name plus the last 4 digits of the current millisecond time
		// ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
		final String random = Long.toString(System.currentTimeMillis());
		final String name = declarer.getName() + "-" + random.substring(random.length() - 4, random.length());

		final War war = new War(name, declarer);
		war.setTarget(target);
		war.setWarType(WarType.RAID);
		return war.execute();
	}

	public static boolean quickRebellion(final String name, final Attackable rebelTown, final Attackable parent) {

		// first make sure the parent nation isn't itself in a rebellion
		// it is *not* rebellions all the way down . . .
		for (final War war : parent.getWars()) {
			if (war.getWarType() == WarType.REBELLION) {
				return false;
			}
		}

		// also check that there is more than one town in the parent nation
		if (((TownyWarsNation) parent).getNation().getTowns().size() < 2) {
			return false;
		}

		// make a default name for the war using the rebel town's name plus the current millisecond time
		// ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
		final String random = Long.toString(System.currentTimeMillis());
		final String warName = rebelTown.getName() + "-" + random.substring(random.length() - 4, random.length());

		Attackable rebelNation = null;

		// try to make a new nation . . .
		try {
			TownyUniverse.getDataSource().newNation(name);
			rebelNation = TownyWarsNation.getNation(TownyUniverse.getDataSource().getNation(name));
		} catch (final Exception e) {
			System.out.println("[TownyWars] quick rebellion creation failed catastrophically!");
			e.printStackTrace();
			return false;
		}

		// . . .and move the town
		try {
			((TownyWarsNation) parent).getNation().removeTown(((TownyWarsTown) rebelTown).getTown());
			((TownyWarsNation) rebelNation).getNation().addTown(((TownyWarsTown) rebelTown).getTown());
		} catch (final Exception e) {
			System.out.println("[TownyWars] quick rebellion town movement failed catastrophically!");
			e.printStackTrace();
			return false;
		}

		final War war = new War(warName, rebelNation);

		// the only other member of the war is the parent nation, of course
		war.setTarget(parent);

		// set the type
		war.setWarType(WarType.REBELLION);

		// to war!
		return war.execute();
	}

	public static boolean quickWar(final Attackable declarer, final Attackable target) {

		if (WarManager.getSharedWar(declarer, target) != null) {
			return false;
		}

		// make a default name using the declarer's name plus the last 6 digits of the current millisecond time
		// ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
		final String random = Long.toString(System.currentTimeMillis());
		final String name = declarer.getName() + "-" + random.substring(random.length() - 4, random.length());

		final War war = new War(name, declarer);

		// add all the nations that were specified
		war.setTarget(target);

		// set the type
		war.setWarType(WarType.NORMAL);

		// to war!
		return war.execute();
	}

	// sets a generic peace request for the given nation in the given war
	// if everyone wants peace, the war gets ended
	public static void requestPeace(final War war, final Attackable org) {
		war.setPeaceOffer(org, "peace");

		final Attackable enemy = war.getEnemy(org);
		
		if (enemy instanceof TownyWarsTown) {
			for (final Resident re : ((TownyWarsTown) enemy).getTown().getResidents()) {
				TownyWarsResident resident=TownyWarsResident.getResident(re);
				// players could be offline, so getResident would return null
				if (resident!=null) {
					resident.getPlayer().sendMessage(ChatColor.GREEN + org.getName() + " has requested peace in war "+war.getName()+"!");
				}
			}
		}
		
		if (enemy instanceof TownyWarsNation) {
			for (final Resident resident : ((TownyWarsNation) enemy).getNation().getResidents()) {
				TownyWarsResident.getResident(resident).getPlayer().sendMessage(ChatColor.GREEN + org.getName() + " has requested peace in war "+war.getName()+"!");
			}
		}
		
		// TODO: add check for Coalitions here
		
		if (war.allPeaceOffered()) {
			war.end();
		}
		war.save();
	}
}
