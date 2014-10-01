package net.minecraftcenter.townywars.object;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
//import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraftcenter.townywars.TownyWars;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;

public class TownyWarsOrg extends TownyWarsObject {
	
	private static Map<TownyEconomyObject,TownyWarsOrg> nationToTownyWarsNationHash = new HashMap<TownyEconomyObject,TownyWarsOrg>();
	private static Map<UUID,TownyWarsOrg> allTownyWarsNations = new HashMap<UUID,TownyWarsOrg>();
	
	// by default, a nation can't be attacked by the same nation within 7 days of the last war ending
	//private static long warTimeout=7*24*3600*1000;
	
	private Nation nation=null;
	private int deaths=0;
	private Set<War> wars=new HashSet<War>();
	//private Map<TownyWarsNation,Long> previousEnemies = new HashMap<TownyWarsNation,Long>();
	private List<TownyWarsTown> capitalPriority = new ArrayList<TownyWarsTown>();
	

	public TownyWarsOrg(Nation nation, UUID uuid){
		super(uuid);
		newTownyWarsNation(nation);
	}
	
	public TownyWarsOrg(Nation nation){
		super(UUID.randomUUID());
		newTownyWarsNation(nation);
	}
	
	private void newTownyWarsNation(Nation nation) {
		this.nation=nation;
		setName(this.nation.getName());
	}
	
	public Nation getNation(){
		return this.nation;
	}
	
	public double getDP(){
		double totalDP=0;
		for (Town town : this.nation.getTowns()) {
			TownyWarsTown townyWarsTown=TownyWarsTown.getTown(town);
			if (townyWarsTown!=null){
				totalDP+=townyWarsTown.getDP();
			}
		}
		return totalDP;
	}
	
	public double getMaxDP(){
		double maxDP=0;
		for (Town town : this.nation.getTowns()) {
			TownyWarsTown townyWarsTown=TownyWarsTown.getTown(town);
			if (townyWarsTown!=null){
				maxDP+=townyWarsTown.getMaxDP();
			}
		}
		return maxDP;
	}
	
	public int getDeaths(){
		return this.deaths;
	}
	
	public void setDeaths(int deaths) {
		this.deaths=deaths;
	}
	
	public void addDeath() {
		this.deaths++;
	}
	
	public Set<War> getWars(){
		return this.wars;
	}
	
	public War getWarWithNation(TownyWarsOrg nation) {
		for (War war : this.wars) {
			if (war.getEnemy(this)==nation) {
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
	
	// this is substantially slower than the other getTown methods since it has to loop over all existing towns
	public static TownyWarsOrg getNation(String nationName) {
		for (Map.Entry<UUID,TownyWarsOrg> entry : TownyWarsOrg.allTownyWarsNations.entrySet()) {
			if (entry.getValue().getName().compareTo(nationName)==0) {
				return entry.getValue();
			}
		}
		
		// we didn't find an object with this name
		return null;
	}
	
	public static TownyWarsOrg getNation(UUID uuid) {
		return TownyWarsOrg.allTownyWarsNations.get(uuid);
	}
	
	public static TownyWarsOrg getNation(Nation nation) {
		return TownyWarsOrg.nationToTownyWarsNationHash.get(nation);
	}
	
	public static Set<TownyWarsOrg> getAllNations() {
		Set<TownyWarsOrg> allNations=new HashSet<TownyWarsOrg>();
		for (UUID uuid : TownyWarsOrg.allTownyWarsNations.keySet()) {
			allNations.add(TownyWarsOrg.allTownyWarsNations.get(uuid));
		}
		return allNations;
	}
	
	public static boolean putNation(Nation nation) {
		return putnation(nation,null);
	}
	
	public static boolean putNation(Nation nation, TownyWarsOrg newNation) {
		return putnation(nation,newNation);
	}
	
	private static boolean putnation(Nation nation, TownyWarsOrg newNation) {
		
		// evidently the TownyWarsNation object wasn't specified, so a new one is needed
		if (newNation==null) {
			newNation=new TownyWarsOrg(nation);
		}
		TownyWarsOrg.allTownyWarsNations.put(newNation.getUUID(),newNation);
		TownyWarsOrg.nationToTownyWarsNationHash.put(nation, newNation);
		return TownyWars.database.saveNation(newNation, true);
	}
	
	public static boolean removeNation(UUID uuid) {
		return removenation(uuid);
	}
	
	public static boolean removeNation(Nation nation) {
		return removenation(TownyWarsOrg.getNation(nation).getUUID());
	}
	
	public static boolean removeNation(TownyWarsOrg nation) {
		return removenation(nation.getUUID());
	}
	
	private static boolean removenation(UUID uuid) {
		TownyWarsOrg nation = TownyWarsOrg.getNation(uuid);
		boolean saveSuccess=TownyWars.database.saveNation(nation, false);
		TownyWarsOrg.nationToTownyWarsNationHash.remove(nation.getNation());
		TownyWarsOrg.allTownyWarsNations.remove(uuid);
		return saveSuccess;
	}
	
	public static void printnations(){
		  for (Map.Entry<UUID,TownyWarsOrg> entry : TownyWarsOrg.allTownyWarsNations.entrySet()) {
			  System.out.println(entry.getValue());
		  }
	  }
	
}