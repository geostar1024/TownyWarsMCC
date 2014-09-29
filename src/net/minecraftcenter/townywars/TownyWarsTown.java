package net.minecraftcenter.townywars;

//import java.util.HashMap;
//import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.palmergames.bukkit.towny.object.Town;

// some extra fields
public class TownyWarsTown{
	
	//private static long warTimeout=7*24*3600*1000;
	
	private static Map<Town,TownyWarsTown> townToTownyWarsTownHash = new HashMap<Town,TownyWarsTown>();
	private static Map<UUID,TownyWarsTown> allTownyWarsTowns = new HashMap<UUID,TownyWarsTown>();
	
	private UUID uuid;
	private Town town=null;
	private double dp=0;
	private double maxdp=0;
	private int deaths=0;
	private int conquered=0;
	private double mindpfactor=.1;
	//private Map<TownyWarsNation,Long> previousEnemies = new HashMap<TownyWarsNation,Long>();
	
	public TownyWarsTown(Town town){
		newTownyWarsTown(town,UUID.randomUUID(),0,null,0,null);
	}
	
	public TownyWarsTown(Town town, UUID uuid, int deaths, Double dp, int conquered, Double mindpfactor){
		newTownyWarsTown(town,uuid,deaths,dp,conquered,mindpfactor);
	}
	
	private void newTownyWarsTown(Town town, UUID uuid, int deaths, Double dp, int conquered, Double mindpfactor) {
		this.uuid=uuid;
		this.town=town;
		this.conquered=conquered;
		this.maxdp=calculateMaxDP();
		if (dp==null) {
			this.setDP(this.maxdp/(Math.pow(2,this.conquered)));
		}
		else {
			this.dp=dp;
		}
		this.deaths=deaths;
		//this.name=this.town.getName();
		if (mindpfactor==null) {
			this.mindpfactor=.1;
		}
	}
	
	public UUID getUUID(){
		return this.uuid;
	}
	
	public Town getTown(){
		return this.town;
	}
	
	public String getName() {
		return this.getTown().getName();
	}
	
	public void setDP(double newDP){
		this.dp=newDP;
		if (this.dp>this.maxdp) this.dp=this.maxdp;
		
		// DP is allowed to go some percent negative
		if (this.dp<-this.maxdp*this.mindpfactor) this.dp=0;
	}
	
	public double getDP(){
		return this.dp;
	}
	
	public double resetDP(){
		this.setDP(this.maxdp/(Math.pow(2,this.conquered)));
		return this.dp;
	}
	
	public double getMinDPFactor() {
		return this.mindpfactor;
	}
	
	public double modifyDP(double addedDP){
		setDP(this.dp+addedDP);
		return this.dp;
	}
	
	public double calculateMaxDP(){
		this.maxdp=(50-50*Math.pow(Math.E, (-0.04605*this.town.getNumResidents()))) + (60-60*Math.pow(Math.E, (-0.00203*this.town.getTownBlocks().size())));
		return this.maxdp;
	}
	
	public double getMaxDP(){
		return this.maxdp;
	}
	
	public void setDeaths(int newDeaths) {
		this.deaths=newDeaths;
	}
	
	public void addDeath(){
		this.deaths++;
	}
	
	public int getDeaths(){
		return this.deaths;
	}
	
	public void setConquered(int newConquered) {
		this.conquered=newConquered;
	}
	
	// the only time this method should be called is when the town has been conquered and is moving to a new nation
	public void addConquered(){
		this.conquered++;
		this.resetDP();
	}
	
	public int getConquered(){
		return this.conquered;
	}
	
	public static TownyWarsTown getTown(UUID uuid) {
		return TownyWarsTown.allTownyWarsTowns.get(uuid);
	}
	
	public static TownyWarsTown getTown(Town town) {
		return TownyWarsTown.townToTownyWarsTownHash.get(town);
	}
	
	public static Set<TownyWarsTown> getAllTowns() {
		Set<TownyWarsTown> allTowns=new HashSet<TownyWarsTown>();
		for (UUID uuid : TownyWarsTown.allTownyWarsTowns.keySet()) {
			allTowns.add(TownyWarsTown.allTownyWarsTowns.get(uuid));
		}
		return allTowns;
	}
	
	public static boolean putTown(Town town) {
		return puttown(town,null);
	}
	
	public static boolean putTown(Town town, TownyWarsTown newTown) {
		return puttown(town,newTown);
	}
	
	private static boolean puttown(Town town, TownyWarsTown newTown) {
		
		// evidently the TownyWarsTown object wasn't specified, so a new one is needed
		if (newTown==null) {
			newTown=new TownyWarsTown(town);
		}
		TownyWarsTown.allTownyWarsTowns.put(newTown.getUUID(),newTown);
		TownyWarsTown.townToTownyWarsTownHash.put(town, newTown);
		return TownyWars.database.insertTown(newTown, true);
	}
	
	public static boolean removeTown(UUID uuid) {
		return removetown(uuid);
	}
	
	public static boolean removeTown(Town town) {
		return removetown(TownyWarsTown.getTown(town).getUUID());
	}
	
	public static boolean removeTown(TownyWarsTown town) {
		return removetown(town.getUUID());
	}
	
	private static boolean removetown(UUID uuid) {
		TownyWarsTown town = TownyWarsTown.getTown(uuid);
		boolean saveSuccess=TownyWars.database.insertTown(town, false);
		TownyWarsTown.townToTownyWarsTownHash.remove(town.getTown());
		TownyWarsTown.allTownyWarsTowns.remove(uuid);
		return saveSuccess;
	}
	
	public static void printtowns(){
		  for (Map.Entry<UUID,TownyWarsTown> entry : TownyWarsTown.allTownyWarsTowns.entrySet()) {
			  System.out.println(entry.getValue());
		  }
	  }
	

}