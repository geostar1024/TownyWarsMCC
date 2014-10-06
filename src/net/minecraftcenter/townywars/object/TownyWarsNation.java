package net.minecraftcenter.townywars.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public class TownyWarsNation extends TownyWarsOrg {

	private static Map<Nation, TownyWarsNation>	nationToTownyWarsNationHash	= new HashMap<Nation, TownyWarsNation>();

	public static Set<TownyWarsNation> getAllNations() {
		Set<TownyWarsNation> allNations=new HashSet<TownyWarsNation>();
		for (Nation nation : TownyWarsNation.nationToTownyWarsNationHash.keySet()) {
			allNations.add(TownyWarsNation.nationToTownyWarsNationHash.get(nation));
		}
		return allNations;
	}

	public static TownyWarsNation getNation(final Nation nation) {
		return TownyWarsNation.nationToTownyWarsNationHash.get(nation);
	}

	// this is substantially slower than the other getTown methods since it has to loop over all existing towns
	public static TownyWarsNation getNation(final String nationName) {
		for (final TownyWarsNation nation : TownyWarsNation.nationToTownyWarsNationHash.values()) {
			if (nation.getName().compareTo(nationName) == 0) {
				return nation;
			}
		}
		return null;
	}

	public static TownyWarsNation getNation(final UUID uuid) {
		if (TownyWarsOrg.getOrg(uuid) instanceof TownyWarsNation) {
			return (TownyWarsNation) TownyWarsOrg.getOrg(uuid);
		}
		return null;
	}
	
	public static void putNation(final TownyWarsNation newNation) {
		TownyWarsNation.nationToTownyWarsNationHash.put(newNation.getNation(), newNation);
		TownyWarsOrg.add(newNation);
	}

	private Nation	                  nation	        = null;

	private final List<TownyWarsTown>	capitalPriority	= new ArrayList<TownyWarsTown>();

	public TownyWarsNation(final Nation nation) {
		super(UUID.randomUUID());
		townyWarsNation(nation);
	}

	public TownyWarsNation(final Nation nation, final UUID uuid) {
		super(uuid);
		townyWarsNation(nation);
	}

	public void clearCapitalPriority() {
		this.capitalPriority.clear();
	}

	public List<TownyWarsTown> getCapitalPriority() {
		return this.capitalPriority;
	}

	public int getCapitalPriorityForTown(final TownyWarsTown town) {
		return this.capitalPriority.indexOf(town);
	}

	@Override
	public double getDP() {
		double totalDP = 0;
		for (final Town town : getNation().getTowns()) {
			final TownyWarsTown townyWarsTown = TownyWarsTown.getTown(town);
			if (townyWarsTown != null) {
				totalDP += townyWarsTown.getDP();
			}
		}
		return totalDP;
	}

	@Override
	public double getMaxDP() {
		double maxDP = 0;
		for (final Town town : getNation().getTowns()) {
			final TownyWarsTown townyWarsTown = TownyWarsTown.getTown(town);
			if (townyWarsTown != null) {
				maxDP += townyWarsTown.getMaxDP();
			}
		}
		return maxDP;
	}

	public Nation getNation() {
		return this.nation;
	}

	public TownyWarsTown getNextCapital() {
		return this.capitalPriority.get(0);
	}

	public boolean remove() {
		TownyWarsNation.nationToTownyWarsNationHash.remove(this);
		TownyWarsOrg.removeTownyWarsObject(this);
		return this.save(false);
	}

	public void removeCapitalPriorityForTown(final TownyWarsTown town) {
		this.capitalPriority.remove(town);
	}

	// moves the town to the specified location in the priority list
	public void setCapitalPriorityForTown(final TownyWarsTown town, final int priority) {
		this.capitalPriority.remove(town);
		this.capitalPriority.add(priority, town);
	}

	private void townyWarsNation(final Nation nation) {
		this.nation = nation;
		setName(getNation().getName());
		this.save();
	}

}