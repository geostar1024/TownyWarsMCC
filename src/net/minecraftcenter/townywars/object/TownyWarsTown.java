package net.minecraftcenter.townywars.object;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraftcenter.townywars.TownyWars;

import com.palmergames.bukkit.towny.object.Town;

public class TownyWarsTown extends TownyWarsOrg {

	private static Map<Town, TownyWarsTown>	townToTownyWarsTownHash	= new HashMap<Town, TownyWarsTown>();

	public static TownyWarsTown[] getAllTowns() {
		return (TownyWarsTown[]) TownyWarsTown.townToTownyWarsTownHash.values().toArray();
	}

	// this is substantially slower than the other getTown methods since it has to loop over all existing towns
	public static TownyWarsTown getTown(final String townName) {
		for (final TownyWarsTown town : TownyWarsTown.townToTownyWarsTownHash.values()) {
			if (town.getName().compareTo(townName) == 0) {
				return town;
			}
		}
		return null;
	}

	public static TownyWarsTown getTown(final Town town) {
		return TownyWarsTown.townToTownyWarsTownHash.get(town);
	}

	public static TownyWarsTown getTown(final UUID uuid) {
		if (TownyWarsOrg.getOrg(uuid) instanceof TownyWarsTown) {
			return (TownyWarsTown) TownyWarsOrg.getOrg(uuid);
		}
		return null;
	}

	public static void putTown(final TownyWarsTown newTown) {
		TownyWarsTown.townToTownyWarsTownHash.put(newTown.getTown(), newTown);
		TownyWarsOrg.add(newTown);
	}

	private Town	town	    = null;

	private int	   conquered	= 0;

	private double	mindpfactor	= .1;

	public TownyWarsTown(final Town town) {
		super(UUID.randomUUID());
		townyWarsTown(town, null, 0, 0);
	}

	public TownyWarsTown(final Town town, final UUID uuid, final int deaths, final Double dp, final int conquered, final double mindpfactor) {
		super(uuid, deaths);
		townyWarsTown(town, dp, conquered, mindpfactor);

	}

	// the only time this method should be called is when the town has been conquered and is moving to a new nation
	public void addConquered() {
		this.conquered++;
		this.resetDP();
	}

	public double calculateMaxDP() {
		setMaxDP((50 - (50 * Math.pow(Math.E, (-0.04605 * getTown().getNumResidents()))))
		        + (60 - (60 * Math.pow(Math.E, (-0.00203 * getTown().getTownBlocks().size())))));
		return getMaxDP();
	}

	public int getConquered() {
		return this.conquered;
	}

	public double getMinDPFactor() {
		return this.mindpfactor;
	}

	public Town getTown() {
		return this.town;
	}

	public double modifyDP(final double addedDP) {
		setDP(getDP() + addedDP);
		if (getDP() < (-getMaxDP() * getMinDPFactor())) {
			setDP(-getMaxDP() * getMinDPFactor());
		}
		return getDP();
	}

	public boolean remove() {
		TownyWarsTown.townToTownyWarsTownHash.remove(this);
		TownyWarsOrg.removeTownyWarsObject(this);
		return TownyWars.database.saveTown(this, false);
	}

	public double resetDP() {
		this.setDP(getMaxDP() / (Math.pow(2, this.conquered)));
		return getDP();
	}

	public void setConquered(final int conquered) {
		this.conquered = conquered;
	}

	private void setMinDPFactor(final double mindpfactor) {
		this.mindpfactor = mindpfactor;
	}

	private void townyWarsTown(final Town town, final Double dp, final int conquered, final double mindpfactor) {
		this.town = town;
		setName(town.getName());
		calculateMaxDP();
		setConquered(conquered);
		if (dp == null) {
			resetDP();
		} else {
			setDP(dp);
		}
		setMinDPFactor(mindpfactor);
		TownyWars.database.saveTown(this, true);
	}

}