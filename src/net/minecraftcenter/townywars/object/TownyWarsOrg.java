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

import com.palmergames.bukkit.towny.object.TownyEconomyObject;

public class TownyWarsOrg extends TownyWarsObject implements Attackable {

	private static Map<UUID, Attackable>	allAttackables	= new HashMap<UUID, Attackable>();

	// by default, a nation can't be attacked by the same nation within 7 days of the last war ending
	private static long	                 warTimeout	       = 7 * 24 * 3600 * 1000;

	public static Attackable[] getAllOrgs() {
		return (Attackable[]) TownyWarsOrg.allAttackables.values().toArray();
	}

	// this is substantially slower than the other get methods since it has to loop over all existing objects and check their names
	public static List<Attackable> getOrg(final String orgName) {
		final List<Attackable> orgs = new ArrayList<Attackable>();
		for (final Map.Entry<UUID, Attackable> entry : TownyWarsOrg.allAttackables.entrySet()) {
			System.out.println(entry.getValue());
			if (entry.getValue().getName().compareTo(orgName) == 0) {
				orgs.add(entry.getValue());
			}
		}

		// this list could contain nothing, so let the caller know. . .
		if (orgs.isEmpty()) {
			return null;
		}
		return orgs;
	}

	public static Attackable getOrg(final UUID uuid) {
		return TownyWarsOrg.allAttackables.get(uuid);
	}

	public static void printorgs() {
		for (final Map.Entry<UUID, Attackable> entry : TownyWarsOrg.allAttackables.entrySet()) {
			System.out.println(entry.getValue());
		}
	}

	public static void add(final Attackable newOrg) {
		TownyWarsOrg.allAttackables.put(newOrg.getUUID(), newOrg);
	}

	private static void remove(final UUID uuid) {
		TownyWarsOrg.allAttackables.remove(uuid);
	}

	public static void removeTownyWarsObject(final Attackable org) {
		TownyWarsOrg.remove(org.getUUID());
	}

	public static void removeTownyWarsObject(final UUID uuid) {
		TownyWarsOrg.remove(uuid);
	}

	private final TownyEconomyObject	  object	        = null;

	private int	                          deaths	        = 0;

	private double	                      dp	            = 0;
	private double	                      maxdp	            = 0;

	private final Set<War>	              wars	            = new HashSet<War>();
	private final Set<War>	              storedwars	    = new HashSet<War>();

	private final Set<TownyWarsResident>	residents	    = new HashSet<TownyWarsResident>();

	private final Map<TownyWarsOrg, Long>	previousEnemies	= new HashMap<TownyWarsOrg, Long>();

	public TownyWarsOrg() {
		super(UUID.randomUUID());
		newTownyWarsOrg(0);
	}

	public TownyWarsOrg(final UUID uuid) {
		super(uuid);
		newTownyWarsOrg(0);
	}

	public TownyWarsOrg(final UUID uuid, final int deaths) {
		super(uuid);
		newTownyWarsOrg(deaths);
	}

	public void addDeath() {
		this.deaths++;
	}

	public void addPreviousEnemy(final TownyWarsOrg org, final long time) {
		this.previousEnemies.put(org, time);
	}

	public void addWar(final War newWar) {
		this.wars.add(newWar);
	}

	public boolean canBeWarred(final TownyWarsOrg org) {
		return (this.getTimeLastFought(org) - System.currentTimeMillis()) > TownyWarsOrg.warTimeout;
	}

	public int getDeaths() {
		return this.deaths;
	}

	public double getDP() {
		return this.dp;
	}

	public double getMaxDP() {
		return this.maxdp;
	}

	public TownyEconomyObject getObject() {
		return this.object;
	}

	public long getTimeLastFought(final TownyWarsOrg org) {
		return this.previousEnemies.get(org);
	}

	public Set<War> getWars() {
		return this.wars;
	}

	public boolean isInWar() {
		return !this.wars.isEmpty();
	}

	private void newTownyWarsOrg(final int deaths) {
		this.deaths = deaths;
		TownyWarsOrg.allAttackables.put(getUUID(), this);
	}

	public void removePreviousEnemy(final TownyWarsOrg org) {
		this.previousEnemies.remove(org);
	}

	public void removeWar(final War oldWar) {
		this.wars.remove(oldWar);
	}

	public void setDeaths(final int deaths) {
		this.deaths = deaths;
	}

	public void setDP(final double newDP) {
		this.dp = newDP;
		if (this.dp > this.maxdp) {
			this.dp = this.maxdp;
		}
	}

	public void setMaxDP(final double maxdp) {
		this.maxdp = maxdp;
	}

	public Set<War> getStoredWars() {
		return this.storedwars;
	}

	public void addStoredWar(War war) {
		this.storedwars.add(war);
	}

	public void removeStoredWar(War war) {
		this.storedwars.remove(war);
	}

	public War getWar(String warName) {
		for (War war : this.getWars()) {
			if (war.getName().equals(warName)) {
				return war;
			}
		}
		return null;
	}

	public War getStoredWar(String warName) {
		for (War war : this.getStoredWars()) {
			if (war.getName().equals(warName)) {
				return war;
			}
		}
		return null;
	}

	public boolean hasWar(War war) {
		for (War war1 : this.getWars()) {
			if (war1.equals(war)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasStoredWar(War war) {
		for (War war1 : this.getStoredWars()) {
			if (war1.equals(war)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasResident(TownyWarsResident resident) {
		return this.residents.contains(resident);
	}

	public Set<TownyWarsResident> getResidents() {
		return this.residents;
	}

	public void addResident(TownyWarsResident resident) {
		this.residents.add(resident);
	}

	public void removeResident(TownyWarsResident resident) {
		this.residents.remove(resident);
	}

	public void clearStoredWars() {
		this.storedwars.clear();
	}

	public boolean save(boolean active) {
		return TownyWars.database.saveOrg(this, active);
	}

	public boolean save() {
		return TownyWars.database.saveOrg(this, true);
	}

}