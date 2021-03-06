package net.minecraftcenter.townywars.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraftcenter.townywars.TownyWars;
import net.minecraftcenter.townywars.interfaces.Attackable;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

// some extra resident fields needed to properly record deaths
public class TownyWarsResident extends TownyWarsObject {

	private static Map<UUID, TownyWarsResident>	    allTownyWarsResidents	        = new HashMap<UUID, TownyWarsResident>();
	private static Map<Resident, TownyWarsResident>	residentToTownyWarsResidentHash	= new HashMap<Resident, TownyWarsResident>();

	private Resident	                            resident;
	private Player	                                player;
	private long	                                lastHitTime	                    = 0;
	private UUID	                                lastAttackerUUID	            = null;
	private long	                                lastLoginTime	                = 0;
	private long	                                lastLogoutTime	                = 0;
	private long	                                totalPlayTime	                = 0;
	private Set<War>	                            activeWars	                    = new HashSet<War>();
	// private TownyWarsNation lastNation=null;
	private List<Attackable>	                    orgs	                        = new ArrayList<Attackable>();

	public TownyWarsResident(Player player, Resident resident) {
		super(player.getUniqueId());
		newTownyWarsResident(player, resident, null, 0, null, 0, 0, 0);
	}

	public TownyWarsResident(Player player, Resident resident, TownyWarsNation nation) {
		super(player.getUniqueId());
		newTownyWarsResident(player, resident, nation, 0, null, 0, 0, 0);
	}

	public TownyWarsResident(Player player, Resident resident, TownyWarsNation nation, long lastHitTime, UUID lastAttackerUUID, long lastLoginTime,
	        long lastLogoutTime, long totalPlayTime) {
		super(player.getUniqueId());
		newTownyWarsResident(player, resident, nation, lastHitTime, lastAttackerUUID, lastLoginTime, lastLogoutTime, totalPlayTime);
	}

	private void newTownyWarsResident(Player player, Resident resident, TownyWarsNation nation, long lastHitTime, UUID lastAttackerUUID, long lastLoginTime,
	        long lastLogoutTime, long totalPlayTime) {
		this.setName(player.getName());
		this.player = player;
		this.resident = resident;
		// this.lastNation=nation;
		this.lastHitTime = lastHitTime;
		this.lastAttackerUUID = lastAttackerUUID;
		this.lastLoginTime = lastLoginTime;
		this.lastLogoutTime = lastLogoutTime;
		this.totalPlayTime = totalPlayTime;
	}

	public Resident getResident() {
		return this.resident;
	}

	public List<Attackable> getOrgs() {
		return this.orgs;
	}

	public void addOrg(Attackable org) {
		this.orgs.add(org);
		org.addResident(this);
	}

	public void removeOrg(Attackable org) {
		this.orgs.remove(org);
		org.removeResident(this);
	}

	public boolean inOrg(Attackable org) {
		return this.orgs.contains(org);
	}

	public boolean inAnyOrg() {
		return !this.orgs.isEmpty();
	}

	public void clearOrgs() {
		clearorgs(true);
	}

	public void clearOrgs(boolean andorg) {
		clearorgs(andorg);
	}

	public void clearorgs(boolean andorg) {
		if (andorg) {
			for (Attackable org : this.getOrgs()) {
				org.removeResident(this);
			}
		}
		this.orgs.clear();
	}

	public long getLastHitTime() {
		return this.lastHitTime;
	}

	public Player getPlayer() {
		return this.player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public void setResident(Resident resident) {
		this.resident = resident;
	}

	public void setLastHitTime(long newHitTime) {
		this.lastHitTime = newHitTime;
	}

	public UUID getLastAttackerUUID() {
		return this.lastAttackerUUID;
	}

	public void setLastAttackerUUID(UUID attackerUUID) {
		this.lastAttackerUUID = attackerUUID;
	}

	public long getLastLoginTime() {
		return this.lastLoginTime;
	}

	public void setLastLoginTime(long loginTime) {
		this.lastLoginTime = loginTime;
	}

	public long getLastLogoutTime() {
		return this.lastLogoutTime;
	}

	public void setLastLogoutTime(long logoutTime) {
		this.lastLogoutTime = logoutTime;
	}

	public long getTotalPlayTime() {
		return this.totalPlayTime;
	}

	public void setTotalPlayTime(long totalTime) {
		this.totalPlayTime = totalTime;
	}

	public void updateTotalPlayTime() {
		this.totalPlayTime += this.lastLogoutTime - this.lastLoginTime;
	}

	public void removeOldWar(War war) {
		this.activeWars.remove(war);
	}

	public Set<War> getActiveWars() {
		return this.activeWars;
	}

	public void addActiveWar(War war) {
		this.activeWars.add(war);
	}

	public boolean isInActiveWar(War war) {
		return this.activeWars.contains(war);
	}

	// public boolean isInActiveWar(){
	// if (this.lastNation==null) {
	// return false;
	// }
	// return this.lastNation.isInWar();
	// }

	public boolean save() {
		return TownyWars.database.saveResident(this);
	}

	// public TownyWarsNation getLastNation() {
	// return this.lastNation;
	// }

	// public void setLastNation(TownyWarsNation nation) {
	// this.lastNation=nation;
	// }

	public static Set<TownyWarsResident> getAllResidents() {
		Set<TownyWarsResident> allResidents = new HashSet<TownyWarsResident>();
		for (UUID uuid : TownyWarsResident.allTownyWarsResidents.keySet()) {
			allResidents.add(TownyWarsResident.allTownyWarsResidents.get(uuid));
		}
		return allResidents;
	}

	// can retrieve the TownyWarsResident object either by UUID or by the corresponding Towny Resident object
	// having two hashmaps makes the lookup faster, though technically it could be done with just the UUID hashmap,
	// searching the entries for the one that has an object with the proper Resident object
	public static TownyWarsResident getResident(UUID uuid) {
		return TownyWarsResident.allTownyWarsResidents.get(uuid);
	}

	public static TownyWarsResident getResident(Resident resident) {
		return TownyWarsResident.residentToTownyWarsResidentHash.get(resident);
	}

	public static TownyWarsResident putResident(Player player, boolean checkDatabase) {
		return putresident(player, checkDatabase);
	}

	public static TownyWarsResident putResident(Player player) {
		return putresident(player, true);
	}

	public static TownyWarsResident putresident(Player player, boolean checkDatabase) {
		Resident resident = null;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
		} catch (NotRegisteredException e) {
			// this is really bad; this should never happen
			System.out.println("[TownyWars] player not registered!");
			e.printStackTrace();
			return null;
		}
		// resident should not be null from this point

		// check if an entry for this UUID already exists
		TownyWarsResident newResident = TownyWarsResident.getResident(player.getUniqueId());

		if (newResident == null) {

			// we still need to check the database, because the player may not be loaded yet
			boolean result = false;
			if (checkDatabase) {
				result = TownyWars.database.loadResident(player);
			}
			if (!result) {

				// evidently the player wasn't found the database either, so we need a completely new object
				newResident = new TownyWarsResident(player, resident);
			} else {
				newResident = TownyWarsResident.getResident(player.getUniqueId());
			}
		}

		// if the UUID entry already exists, update the links to the Player and Resident objects
		else {
			newResident.setPlayer(player);
			newResident.setResident(resident);
		}
		TownyWarsResident.allTownyWarsResidents.put(player.getUniqueId(), newResident);
		TownyWarsResident.residentToTownyWarsResidentHash.put(resident, newResident);
		//System.out.println(allTownyWarsResidents.get(player.getUniqueId()));

		// add the appropriate orgs to the resident and the resident to its orgs
		TownyWarsResident.updateOrgs(newResident);

		// save the player immediately to the database
		if (!TownyWars.database.saveResident(newResident)) {
			System.out.println("[TownyWars] database error during new resident insertion!");
		}
		return newResident;
	}

	public static void updateOrgs(TownyWarsResident resident) {
		resident.clearOrgs();
		try {
			TownyWarsTown town = TownyWarsTown.getTown(resident.getResident().getTown());
			if (town==null) {
				return;
			}
			resident.addOrg(town);
		} catch (final NotRegisteredException e) {
			return;
		}

		try {
			TownyWarsNation nation = TownyWarsNation.getNation(resident.getResident().getTown().getNation());
			if (nation==null) {
				return;
			}
			resident.addOrg(nation);
		} catch (final NotRegisteredException e) {
			return;
		}

		// TODO: add check for Coalitions here
	}

}