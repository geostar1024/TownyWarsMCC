package net.minecraftcenter.townywars;

import java.util.ArrayList;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public class TownyWarsNation{
	
	// by default, a nation can't be attacked by the same nation within 7 days of the last war ending
	//private static long warTimeout=7*24*3600*1000;
	
	private UUID uuid=null;
	private Nation nation=null;
	private Set<War> wars=new HashSet<War>();
	//private Map<TownyWarsNation,Long> previousEnemies = new HashMap<TownyWarsNation,Long>();
	private List<TownyWarsTown> capitalPriority = new ArrayList<TownyWarsTown>();
	
	public TownyWarsNation(Nation nation, UUID uuid){
		this.uuid=uuid;
		this.nation=nation;
	}
	
	public TownyWarsNation(Nation nation){
		this.uuid=UUID.randomUUID();
		this.nation=nation;
	}
	
	public Nation getNation(){
		return this.nation;
	}
	
	public double getDP(){
		double totalDP=0;
		for (Town town : this.nation.getTowns()) {
			TownyWarsTown townyWarsTown=TownyWars.townToTownyWarsTownHash.get(town);
			if (townyWarsTown!=null){
				totalDP+=townyWarsTown.getDP();
			}
		}
		return totalDP;
	}
	
	public double getMaxDP(){
		double maxDP=0;
		for (Town town : this.nation.getTowns()) {
			TownyWarsTown townyWarsTown=TownyWars.townToTownyWarsTownHash.get(town);
			if (townyWarsTown!=null){
				maxDP+=townyWarsTown.getMaxDP();
			}
		}
		return maxDP;
	}
	
	public int getDeaths(){
		int totalDeaths=0;
		for (Town town : this.nation.getTowns()) {
			TownyWarsTown townyWarsTown=TownyWars.townToTownyWarsTownHash.get(town);
			if (townyWarsTown!=null){
				totalDeaths+=townyWarsTown.getDeaths();
			}
		}
		return totalDeaths;
	}
	
	public UUID getUUID() {
		return this.uuid;
	}
	
	public Set<War> getWars(){
		return this.wars;
	}
	
	public War getWarWithNation(TownyWarsNation nation) {
		for (War war : this.wars) {
			if (war.getEnemies(this).contains(nation)) {
				return war;
			}
		}
		return null;
	}
	
	/*public boolean canBeWarred(TownyWarsNation nation) {
		return this.getTimeLastFought(nation)-System.currentTimeMillis()>warTimeout;
	}
	
	public long getTimeLastFought(TownyWarsNation nation) {
		return this.previousEnemies.get(nation);
	}
	
	public void addPreviousEnemy(TownyWarsNation nation, long time) {
		this.previousEnemies.put(nation, time);
	}*/
	
	public void removeWar(War oldWar){
		this.wars.remove(oldWar);
	}
	
	public void addWar(War newWar) {
		this.wars.add(newWar);
	}
	
	public boolean isInWar() {
		return !this.wars.isEmpty();
	}
	
	public List<TownyWarsTown> getCapitalPriority(){
		return this.capitalPriority;
	}
	
	public void clearCapitalPriority() {
		this.capitalPriority.clear();
	}
	
	// moves the town to the specified location in the priority list
	public void setCapitalPriorityForTown(TownyWarsTown town, int priority) {
		this.capitalPriority.remove(town);
		this.capitalPriority.add(priority,town);
	}
	
	public void removeCapitalPriorityForTown(TownyWarsTown town) {
		this.capitalPriority.remove(town);
	}
	
	public int getCapitalPriorityForTown(TownyWarsTown town) {
		return this.capitalPriority.indexOf(town);
	}
	
	public TownyWarsTown getNextCapital() {
		return this.capitalPriority.get(0);
	}
	
}