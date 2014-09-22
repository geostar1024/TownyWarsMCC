package net.minecraftcenter.townywars;

//import java.util.HashMap;
//import java.util.Map;
import java.util.UUID;

import com.palmergames.bukkit.towny.object.Town;

// some extra fields
public class TownyWarsTown{
	
	//private static long warTimeout=7*24*3600*1000;
	
	private String name=null;
	private UUID uuid;
	private Town town=null;
	private double dp=0;
	private double maxdp=0;
	private int deaths=0;
	private int conquered=0;
	//private Map<TownyWarsNation,Long> previousEnemies = new HashMap<TownyWarsNation,Long>();
	
	public TownyWarsTown(Town town){
		newTownyWarsTown(town,UUID.randomUUID(),0,0);
	}
	
	public TownyWarsTown(Town town, UUID uuid, int deaths, int conquered){
		newTownyWarsTown(town,uuid,deaths,conquered);
	}
	
	public void newTownyWarsTown(Town town, UUID uuid, int deaths, int conquered) {
		this.uuid=uuid;
		this.town=town;
		this.maxdp=calculateMaxDP();
		this.dp=this.maxdp;
		this.deaths=deaths;
		this.conquered=conquered;
		this.name=this.town.getName();
	}
	
	public UUID getUUID(){
		return this.uuid;
	}
	
	public Town getTown(){
		return this.town;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name=name;
	}
	
	public void setDP(double newDP){
		this.dp=newDP;
		if (this.dp>this.maxdp) this.dp=this.maxdp;
		// DP is allowed to go 10% negative
		if (this.dp<-this.maxdp/10) this.dp=0;
	}
	
	public double getDP(){
		return this.dp;
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
		this.setDP(this.maxdp/(Math.pow(2,this.conquered)));
	}
	
	public int getConquered(){
		return this.conquered;
	}

}