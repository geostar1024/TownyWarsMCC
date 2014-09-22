package net.minecraftcenter.townywars;

//import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
//import com.palmergames.bukkit.towny.object.Town;
//import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.List;
import java.util.Map;
import java.util.Set;
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
import java.util.UUID;
//import java.util.logging.Level;
//import java.util.logging.Logger;

//import main.java.com.danielrharris.townywars.War.MutableInteger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
//import org.bukkit.ChatColor;
import org.bukkit.entity.Player;



public class War {
	
	public static enum WarType {
		NORMAL,
		REBELLION,
		FLAG,
	}
	
	public static enum WarStatus {
		PREPARE,
		PREWAR,
		CURRENT,
		ENDED,
	}
	
	private static final double threshold=.3;
	private String name=null;
	private UUID uuid=null;
	private long startTime=0;
	private long endTime=0;
	private WarType type=null;
	private WarStatus status=null;
	private Set<TownyWarsTown> targets=new HashSet<TownyWarsTown>();
	private TownyWarsNation declarer=null;
	private long prewarTime=0;
	
	private Map<TownyWarsNation,String> peaceOffers = new HashMap<TownyWarsNation,String>();
	//private Map<TownyWarsNation,Double> requestedMoney = new HashMap<TownyWarsNation,Double>();
	private Map<TownyWarsNation,Integer> deaths = new HashMap<TownyWarsNation,Integer>();
	//private Map<TownyWarsNation,Set<TownyWarsNation>> peaceAccepted = new HashMap<TownyWarsNation,Set<TownyWarsNation>>();
	
	public War(String name, TownyWarsNation declarer, UUID uuid) {
		newWar(name,declarer,uuid);
	}
	
	// use a random UUID to uniquely identify this war
	public War(String name, TownyWarsNation declarer) {
		newWar(name,declarer,UUID.randomUUID());
	}
	
	// everything needed to start setting up a new war
	// minimum information is a name for the war, the nation that declared, and a UUID for the war object
	private void newWar(String name, TownyWarsNation declarer, UUID uuid) {
		this.name=name;
		
		this.uuid=uuid;
		
		// by default, the prewar period is 24 hours
		this.prewarTime=24*3600*1000;
		
		// by default, all wars start in the preparation phase
		this.status=WarStatus.PREPARE;
		
		// the creator of the war is by default the declarer
		this.declarer=declarer;
		
		this.deaths.put(declarer, 0);
	}
	
	
	// time to start the war!
	public boolean execute() {
		
		// make sure the war is sufficiently set up
		if (!checkWar()) {
			return false;
		}
		
		// record the war start time and set the status to CURRENT
		this.startTime=System.currentTimeMillis();
		this.status=WarStatus.CURRENT;
		
		// store the reference to this war in each of the TownyWarNation objects
		for (TownyWarsNation nation : this.deaths.keySet()) {
			nation.addWar(this);
		}
		
		// make all nations in this war enemies!
		for (TownyWarsNation nation1 : this.deaths.keySet()) {
			for (TownyWarsNation nation2 : this.deaths.keySet()) {
				if (nation1!=nation2) {
					try {
						// if the nations are already allies, don't set them to be enemies!
						if (!nation1.getNation().hasAlly(nation2.getNation())) {
							nation1.getNation().addEnemy(nation2.getNation());
						}
					} catch (AlreadyRegisteredException e) {
						// ignore if a nation is already the enemy of another nation; could happen if two nations happen to be in multiple wars
					}
				}
			}
		}
		
		// turn on pvp for all the towns in all the nations at war!
		if (this.type!=WarType.FLAG) {
			for (TownyWarsNation nation : this.deaths.keySet()) {
				for (Town town : nation.getNation().getTowns()) {
					town.setPVP(true);
				}
			}
		}
		// but if it's a flag war, only turn on pvp for the aggressor and the target town(s)
		else {
			for (Town town : this.declarer.getNation().getTowns()) {
				town.setPVP(true);
			}
			for (TownyWarsTown town : this.targets) {
				town.getTown().setPVP(true);
			}
		}
		
		// save the stuff we've been doing
		TownyUniverse.getDataSource().saveTowns();
	    TownyUniverse.getDataSource().saveNations();
	    
	    informPlayers(true);
	    return true;
	}
	
	// determine if enough war parameters are set
	boolean checkWar() {
		if (this.type==null || (this.targets.isEmpty() && this.type==WarType.FLAG) || this.deaths.keySet().size()<2) {
			return false;
		}
		return true;
	}
	
	public String getName(){
		return this.name;
	}
	
	public UUID getUUID(){
		return this.uuid;
	}
	
	public TownyWarsNation getDeclarer(){
		return this.declarer;
	}
	
	public Set<TownyWarsNation> getNations(){
		return this.deaths.keySet();
	}
	
	public WarType getWarType() {
		return this.type;
	}
	
	public boolean setWarType(WarType type) {
		// only allow this modification before the war is executed
		if (this.status!=WarStatus.PREPARE) {
			return false;
		}
		this.type=type;
		return true;
	}
	
	public Set<TownyWarsTown> getTarget() {
		return this.targets;
	}
	
	public void addTarget(TownyWarsTown town) {
		this.targets.add(town);
		TownyWarsNation nation=null;
		
		// attempt to add this town's parent nation to the war
		try {
			nation=TownyWars.nationToTownyWarsNationHash.get(town.getTown().getNation());	
		} catch (NotRegisteredException e) {
			
			// not a problem if the nation isn't registered
			return;
		}
		this.addMember(nation);
	}
	
	public void removeTarget(TownyWarsTown town) {
		this.targets.remove(town);
	}
	
	public void clearTargets() {
		this.targets.clear();
	}
	
	public long getStartTime(){
		return this.startTime;
	}
	
	public long getEndTime(){
		return this.endTime;
	}
	
	public void setEndTime(long time) {
		this.endTime=time;
	}
	
	public int getDeaths(TownyWarsNation nation){
		return this.deaths.get(nation);
	}
	
	public void setDeaths(TownyWarsNation nation, int deaths) {
		this.deaths.put(nation,deaths);
	}
	
	public String getPeaceOffer(TownyWarsNation nation) {
		return this.peaceOffers.get(nation);
	}
	
	public boolean offeredPeace(TownyWarsNation nation) {
		return (this.peaceOffers.get(nation)!=null);
	}
	
	public void setPeaceOffer(TownyWarsNation nation, String peaceOffer) {
		this.peaceOffers.put(nation, peaceOffer);
	}
	
	public boolean allPeace(){
		for (TownyWarsNation nation : this.getNations()) {
			if (this.peaceOffers.get(nation)==null) {
				return false;
			}
		}
		return true;
	}
	
	public TownyWarsNation checkWin(){
		switch(this.type) {
		case FLAG:
			int subjugatedTowns=0;
			for (TownyWarsTown target : this.targets) {
				// check if all the target towns have been subjugated
				if (target.getDP()/target.getMaxDP()<threshold) {
					subjugatedTowns++;
				}
			}
			if (subjugatedTowns==this.targets.size()) {
				return this.declarer;
			}
			// note that if there's only one town targeted, this will always return the nation of the town that was targeted
			if (this.declarer.getDP()/this.declarer.getMaxDP()<threshold) {
				List<TownyWarsNation> allTargets = new ArrayList<TownyWarsNation>(this.getNations());
				return allTargets.get(0);
			}
			break;
		case NORMAL:
			if (this.getNations().size()==1) {
				for (TownyWarsNation nation : this.getNations()) {
					return nation;
				}
			}
			break;
		case REBELLION:
			for (TownyWarsNation parent : this.getNations()) {
				// check if the parent nation has been brought to its knees
				if (parent!=this.declarer) {
					if (parent.getDP()/parent.getMaxDP()<threshold) {
						return this.declarer;
					}
				}
			}
			// note that if there's only two nations in the war, this will always return the nation that is not the declarer
			if (this.declarer.getDP()/this.declarer.getMaxDP()<threshold) {
				List<TownyWarsNation> allButDeclarer = new ArrayList<TownyWarsNation>(this.getNations());
				allButDeclarer.remove(this.declarer);
				return allButDeclarer.get(0);
			}
			// no nation has won yet
			break;
		default:
			break;
		}
		return null;
	}
	
	/*public void acceptPeaceOffer(TownyWarsNation nation, TownyWarsNation enemy) {
		if (this.peaceAccepted.get(nation)==null) {
			Set<TownyWarsNation> acceptances=new HashSet<TownyWarsNation>();
			acceptances.add(enemy);
			this.peaceAccepted.put(nation,acceptances);
			return;
		}
		this.peaceAccepted.get(nation).add(enemy);
	}*/
	
	
	// checks to see if everyone has accepted everyone else's peace terms
	/*public boolean checkPeace(){
		for (TownyWarsNation nation : this.deaths.keySet()) {
			if (this.peaceAccepted.get(nation)==null) {
				return false;
			}
			if (!this.peaceAccepted.get(nation).equals(this.getEnemies(nation))) {
				return false;
			}
		}
		return true;
	}*/
	
	public Set<TownyWarsNation> getEnemies(TownyWarsNation nation){
		Set<TownyWarsNation> enemies=this.peaceOffers.keySet();
		enemies.remove(nation);
		return enemies;
	}
	
	public boolean isMember(TownyWarsNation nation) {
		return (this.deaths.get(nation)!=null);
	}
	
	public boolean addMember(TownyWarsNation nation) {
		// first, check on the status of the war; if it isn't in progress yet, the nation can be added
		if (this.status==WarStatus.PREPARE) {
			return false;
		}
		this.deaths.put(nation,0);
		return true;
	}
	
	public void removeMember(TownyWarsNation nation) {
		// first, check on the status of the war; if it isn't in progress yet, just silently remove the nation
		if (this.status==WarStatus.PREPARE) {
			this.deaths.remove(nation);
			this.peaceOffers.remove(nation);
			return;
		}
		
		// see if there had been only two nations in this war and stop the war if so
		if (this.deaths.keySet().size()<3) {
			for (TownyWarsNation nation1 : this.deaths.keySet()) {
				endWarForNation(nation1);
			}
		}
		else {
			endWarForNation(nation);
		}
	}
	
	// ends the war for the current nation
	// should be the last thing that happens since it modifies the War
	public void endWarForNation(TownyWarsNation nation) {
		
		nation.removeWar(this);
		this.deaths.remove(nation);
		this.peaceOffers.remove(nation);
		
		// make this nation neutral (remove from enemies lists)
		// and remove the other nations in the war from its enemies list
		for (TownyWarsNation nation1 : this.deaths.keySet()) {
			if (nation!=nation1) {
				nation1.getNation().getEnemies().remove(nation);
				nation.getNation().getEnemies().remove(nation1);
			}
		}
		
		// turn off pvp for all the towns in this nation, as long as it's not in other wars
		// also reset the conquest counter
		if (nation.getWars().isEmpty()) {
			for (Town town : nation.getNation().getTowns()) {
				town.setPVP(false);
				TownyWars.townToTownyWarsTownHash.get(town).setConquered(0);
			}
		}
		
		// save the stuff we've been doing
		TownyUniverse.getDataSource().saveTowns();
	    TownyUniverse.getDataSource().saveNations();
	}
	
	// make sure that the prewar time is between 24 and 48 hours, otherwise return false
	public boolean setPrewarTime(long time) {
		if (time>24*3600*1000 && time<48*3600*1000) {
			this.prewarTime=time;
			return true;
		}
		return false;
	}
	
	public long getPrewarTime() {
		return this.prewarTime;
	}
	
	public long getElapsedTime(WarStatus status) {
		switch(status) {
		case PREPARE:
			return 0;
		case PREWAR:
			return System.currentTimeMillis()-this.prewarTime;
		default:
			return System.currentTimeMillis()-this.startTime;
		}
		
	}

	// let all or just the players in this war know its current status as long as it's not in the prepare phase (that would be awkward)
	// if a particular player is specified, let them know the current status of this war even if it's being prepared
	public void informPlayers(TownyWarsResident resident) {
		informplayers(resident,false);
	}
	
	public void informPlayers(boolean all) {
		informplayers(null,true);
	}
		
	private void informplayers(TownyWarsResident resident, boolean all) {
		if (this.status==WarStatus.PREPARE && resident==null) {
			return;
		}
		String message=this.name+": ";
		if (!all) {
			for (TownyWarsNation nation : this.getNations()) {
				if (nation==this.declarer) {
					message=ChatColor.GOLD+"Your nation";
				}
				else {
					message=ChatColor.GOLD+nation.getNation().getName();
				}
				
				switch(this.status) {
				case CURRENT:
					message+="is in";
					break;
				case ENDED:
					message+="was in";
					break;
				case PREWAR:
					message+="is threatening";
					break;
				case PREPARE:
					message+="is preparing";
					break;
				default:
					break;
				}
				message+=" a ";
				switch(this.type){
				case FLAG:
					message+="flag war";
					break;
				case NORMAL:
					message+="war";
					break;
				case REBELLION:
					message+="rebellion";
					break;
				default:
					break;
				}
				
				// check for allies
				if (!nation.getNation().getAllies().isEmpty()) {
					boolean first=true;
					int numAllies=nation.getNation().getAllies().size();
					int k=0;
					for (Nation ally : nation.getNation().getAllies()) {
						if (this.getNations().contains(TownyWars.nationToTownyWarsNationHash.get(ally))) {
							if (first) {
								message+=" with ";
								first=false;
							}
							message+=ally.getName();
							// some fanciness: add a comma between names if there's 3 or more enemies
					  		// and add "and" after the last separating comma
					  		if (k<numAllies-1 && numAllies>2) {
					  			message+=", ";
					  			if (k<numAllies-2) {
					  				message+="and ";
					  			}
					  		}
					  		k++;
						}
					}
				}
				
				message+=" against ";
				// now handle the enemies
				
				Set<TownyWarsNation> enemies=this.getEnemies(nation);
			  	int numEnemies=enemies.size();
			  	int k=0;
			  	for (TownyWarsNation enemy : enemies) {
			  		message+=enemy.getNation().getName();
			  		
			  		// some fanciness: add a comma between names if there's 3 or more enemies
			  		// and add "and" after the last separating comma
			  		if (k<numEnemies-1 && numEnemies>2) {
			  			message+=", ";
			  			if (k<numEnemies-2) {
			  				message+="and ";
			  			}
			  		}
			  		k++;
			  	}
				
			  	// display for all players in the nation if no player was explicitly specified
			  	if (resident==null) {
					for (Resident resident1 : nation.getNation().getResidents()) {
						Player plr = Bukkit.getPlayer(resident1.getName());
					      if (plr != null) {  
					        plr.sendMessage(message);
					      }
					}
			  	}
			  	else {
			  		resident.getPlayer().sendMessage(message);
			  	}
			}
		}
		
		
		
		if (all) {
			Bukkit.getServer().broadcastMessage(message);
		}
	}
	
	// immediately end the war for everyone involved
	public void end() {
		for (TownyWarsNation nation : this.getNations()) {
			endWarForNation(nation);
		}
	}
/*

	public void chargeTownPoints(Nation nnation, Town town, double i) {
		towns.get(town).value -= i;
		if (towns.get(town).value <= 0) {
			try {
				if(nnation.getTowns().size() > 1 && nnation.getCapital() == town){
					if(nnation.getTowns().get(0) != town){
						nnation.setCapital(nnation.getTowns().get(0));
					}else{
						nnation.setCapital(nnation.getTowns().get(1));
					}
				}
					
					
				towns.remove(town);
				Nation nation = WarManager.getWarForNation(nnation).getEnemy(nnation);
				removeNationPoint(nnation);
				addNationPoint(nation, town);
				try {	
						WarManager.townremove = town;
						nnation.removeTown(town);
				} catch (Exception ex) {
				}
				nation.addTown(town);
				town.setNation(nation);
				TownyUniverse.getDataSource().saveNation(nation);
				TownyUniverse.getDataSource().saveNation(nnation);
				try {
					WarManager.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				broadcast(
						nation,
						ChatColor.GREEN
								+ town.getName()
								+ " has been conquered and joined your nation in the war!");
			} catch (Exception ex) {
				Logger.getLogger(War.class.getName()).log(Level.SEVERE, null,
						ex);
			}
		}
		try {
			if (this.getNationPoints(nnation) <= 0) {
				try {
						Nation winner = getEnemy(nnation);
						Nation looser = nnation;
						boolean endWarTransfersDone = false;
						for(Rebellion r : Rebellion.getAllRebellions()){
							if(r.getRebelnation() == winner){
								winner.getCapital().collect(winner.getHoldingBalance());
								winner.pay(winner.getHoldingBalance(), "You are disbanded. You don't need money.");
								endWarTransfersDone = true;
								break;
							}
						}
						
						if(!endWarTransfersDone){
							winner.collect(looser.getHoldingBalance());
							looser.pay(looser.getHoldingBalance(), "Conquered. Tough luck!");
						}
						WarManager.endWar(winner, looser, false);

				} catch (Exception ex) {
					Logger.getLogger(War.class.getName()).log(Level.SEVERE, null,
							ex);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			WarManager.save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeNationPoint(Nation nation) {
		if(nation1 == nation)
			nation1points--;
		if(nation2 == nation)
			nation2points--;
	}

	public void addNationPoint(Nation nation, Town town) {
		if(nation1 == nation)
			nation1points++;
		if(nation2 == nation)
			nation2points++;
		towns.put(town,
				new MutableInteger((int) (town.getNumResidents()
						* TownyWars.pPlayer + TownyWars.pPlot
						* town.getTownBlocks().size())));
	}
*/
	public static void broadcast(Nation n, String message) {
		for (Resident re : n.getResidents()) {
			Player plr = Bukkit.getPlayer(re.getName());
			if (plr != null) {
				plr.sendMessage(message);
			}
		}
	}

}
