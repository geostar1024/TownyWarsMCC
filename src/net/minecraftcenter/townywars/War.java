package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
	private long prewarTime=0;
	private WarType type=null;
	private WarStatus status=null;
	private TownyWarsTown targetTown=null;
	private TownyWarsNation target=null;
	private TownyWarsNation declarer=null;
	private TownyWarsNation winner=null;
	private String targetTownName=null;
	private String targetName=null;
	private String declarerName=null;
	
	
	private Map<UUID,String> peaceOffers = new HashMap<UUID,String>();
	private Map<UUID,Double> requestedMoney = new HashMap<UUID,Double>();
	private Map<UUID,Integer> deaths = new HashMap<UUID,Integer>();
	private Set<UUID> peaceAccepted = new HashSet<UUID>();
	
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
		
		this.deaths.put(declarer.getUUID(), 0);
	}
	
	
	// time to start the war!
	public boolean execute() {
		
		// make sure the war is sufficiently set up
		if (!checkWar()) {
			System.out.println("war not setup correctly!");
			return false;
		}
		
		// record the war start time and set the status to CURRENT
		this.startTime=System.currentTimeMillis();
		this.status=WarStatus.CURRENT;
		
		// store the reference to this war in each of the TownyWarNation objects
		for (TownyWarsNation nation : this.getNations()) {
			nation.addWar(this);
		}
		
		// make all nations in this war enemies!
		for (TownyWarsNation nation1 : this.getNations()) {
			for (TownyWarsNation nation2 : this.getNations()) {
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
			for (TownyWarsNation nation : this.getNations()) {
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
			this.targetTown.getTown().setPVP(true);
		}
		
		// save the stuff we've been doing
		TownyUniverse.getDataSource().saveTowns();
	    TownyUniverse.getDataSource().saveNations();
	    
	    informPlayers(true);
	    return true;
	}
	
	// determine if enough war parameters are set
	boolean checkWar() {
		if (this.type==null || (this.target==null && this.type==WarType.FLAG) || this.getNations().size()<2) {
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
	
	public void setStartTime(long time) {
		this.startTime=time;
	}
	
	public List<TownyWarsNation> getNations(){
		List<TownyWarsNation> allNations=new ArrayList<TownyWarsNation>();
		allNations.add(this.declarer);
		allNations.add(this.target);
		return allNations;
	}
	
	public WarType getWarType() {
		return this.type;
	}
	
	public WarStatus getWarStatus() {
		return this.status;
	}
	
	public void setWarStatus(WarStatus status) {
		this.status=status;
	}
	
	public static double getThreshold() {
		return threshold;
	}
	
	public boolean setWarType(WarType type) {
		// only allow this modification before the war is executed
		if (this.status!=WarStatus.PREPARE) {
			return false;
		}
		this.type=type;
		return true;
	}
	
	public TownyWarsNation getWinner() {
		return this.winner;
	}
	
	public void setWinner(TownyWarsNation winner) {
		this.winner=winner;
	}
	
	public String getTargetName() {
		if (this.targetName!=null) {
			return this.targetName;
		}
		if (this.target!=null) {
			try {
				return this.target.getNation().getName();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	
	public String getDeclarerName() {
		if (this.declarerName!=null) {
			return this.declarerName;
		}
		if (this.declarer!=null) {
			try {
				return this.declarer.getNation().getName();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	
	public String getTargetTownName() {
		if (this.targetTownName!=null) {
			return this.targetTownName;
		}
		if (this.targetTown!=null) {
			try {
				return this.targetTown.getTown().getName();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	
	public TownyWarsNation getTarget() {
		return this.target;
	}
	
	public void setTarget(TownyWarsNation nation) {
		if (this.target!=null) {
			this.deaths.remove(this.target.getUUID());
		}
		this.target=nation;
		this.deaths.put(this.target.getUUID(), 0);
		
	}
	
	public TownyWarsTown getTargetTown() {
		return this.targetTown;
	}
	
	public boolean setTargetTown(TownyWarsTown town) {
		this.targetTown=town;
		TownyWarsNation nation=null;
		
		// attempt to add this town's parent nation to the war
		try {
			nation=TownyWarsNation.getNation(town.getTown().getNation());	
		} catch (NotRegisteredException e) {
			
			// the town needs to be in a nation, or it is not a valid target at this point
			return false;
		}
		setTarget(nation);
		return true;
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
		this.deaths.put(nation.getUUID(),deaths);
	}
	
	public String getPeaceOffer(TownyWarsNation nation) {
		return this.peaceOffers.get(nation.getUUID());
	}
	
	public boolean offeredPeace(TownyWarsNation nation) {
		return (this.peaceOffers.get(nation.getUUID())!=null);
	}
	
	public boolean acceptedPeace(TownyWarsNation nation) {
		return this.peaceAccepted.contains(nation.getUUID());
	}
	
	public void acceptPeace(TownyWarsNation nation) {
		this.peaceAccepted.add(nation.getUUID());
	}
	
	public void setPeaceOffer(TownyWarsNation nation, String peaceOffer) {
		this.peaceOffers.put(nation.getUUID(), peaceOffer);
	}
	
	public boolean allPeaceOffered(){
		for (TownyWarsNation nation : this.getNations()) {
			if (this.peaceOffers.get(nation.getUUID())==null) {
				return false;
			}
		}
		return true;
	}
	
	public boolean allPeaceAccepted() {
		for (TownyWarsNation nation : this.getNations()) {
			if (!this.peaceAccepted.contains(nation.getUUID())) {
				return false;
			}
		}
		return true;
	}
	
	public double getRequestedMoney(TownyWarsNation nation) {
		return this.requestedMoney.get(nation.getUUID());
	}
	
	public void setRequestedMoney(TownyWarsNation nation, double money) {
		this.requestedMoney.put(nation.getUUID(),money);
	}
	
	public TownyWarsNation checkWinner(){
		switch(this.type) {
		case FLAG:
			// if the target town's DP or the DP of the target town's nation is depleted below the threshold, the declarer wins
			if (targetTown.getDP()/targetTown.getMaxDP()<threshold || target.getDP()/target.getMaxDP()<threshold) {
				return this.declarer;
			}
			
			// if the declarer's DP is depleted below the threshold, the target wins
			if (this.declarer.getDP()/this.declarer.getMaxDP()<threshold) {
				return this.target;
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
			// check if the parent nation has been brought to its knees
			if (target.getDP()/target.getMaxDP()<threshold) {
				return this.declarer;
			}
			
			// if the rebel nation's DP is depleted below the threshold, the parent nation wins
			if (this.declarer.getDP()/this.declarer.getMaxDP()<threshold) {
				return this.target;
			}
			// no nation has won yet
			break;
		default:
			break;
		}
		return null;
	}
	
	public TownyWarsNation getEnemy(TownyWarsNation nation){
		if (nation==this.declarer) {
			return this.target;
		}
		else {
			return this.declarer;
		}
	}
	
	public boolean hasMember(TownyWarsNation nation) {
		return (this.getNations().contains(nation));
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
		case ENDED:
			return this.endTime-this.startTime;
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
		String message=ChatColor.GOLD+"War "+ChatColor.WHITE+this.name+": "+ChatColor.GOLD;
		if (all) {
			message+=this.declarer.getName()+informPlayersSelector(this.declarer);
			Bukkit.getServer().broadcastMessage(message);
			return;
		}
		
		if (resident!=null) {
			TownyWarsNation nation=null;
			try {
				nation=TownyWarsNation.getNation(resident.getResident().getTown().getNation());
			} catch (NotRegisteredException e) {
				// just continue, it isn't fatal if this resident isn't in a nation
			}
			if (nation==this.declarer) {
				message+="Your nation";
			}
			else {
				message+=this.declarer.getName();
			}
			message+=informPlayersSelector(this.declarer);
			resident.getPlayer().sendMessage(message);
			return;
		}
		for (TownyWarsNation nation : this.getNations()) {
			if (nation==this.declarer) {
				message+="Your nation";
			}
			else {
				message+=nation.getNation().getName();
			}
			message+=informPlayersSelector(this.declarer);

		  	// display for all players in the nation
			for (Resident resident1 : nation.getNation().getResidents()) {
				Player plr = Bukkit.getPlayer(resident1.getName());
			      if (plr != null) {  
			        plr.sendMessage(message);
			      }
			}
		 }

	}
	
	private String informPlayersSelector(TownyWarsNation nation) {
		String message=" ";
		switch(this.status) {
		case CURRENT:
			message+="is in";
			break;
		case ENDED:
			if (nation==this.winner) {
				message+="won";
			}
			else if (this.winner==null) {
				message+="made peace in";
			}
			else {
				message+="lost";
			}
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
		
		// now handle the enemy
		message+=" against "+this.getEnemy(nation).getName();
		return message;
	}
	
	// immediately end the war for everyone involved
	public void end() {
		this.setWinner(this.checkWinner());
		// indicate that the war has ended
		this.status=WarStatus.ENDED;
		this.endTime=System.currentTimeMillis();
		
		this.informPlayers(true);
		for (TownyWarsNation nation : this.getNations()) {
			nation.removeWar(this);
			
			// make this nation neutral (remove from enemies lists)
			// and remove the other nations in the war from its enemies list
			for (TownyWarsNation nation1 : this.getNations()) {
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
					TownyWarsTown.getTown(town).setConquered(0);
				}
			}
		}
		
		// save the names of the nations and town in this war
		this.declarerName=this.declarer.getName();
		this.targetName=this.target.getName();
		this.targetTownName=this.targetTown.getName();
		
		
		// finally, handle the special case of a rebellion ending not in the rebels' favor
		if (this.type==WarType.REBELLION && this.winner!=this.declarer) {
			TownyWarsTown loser=TownyWarsTown.getTown(this.declarer.getNation().getCapital());
			try {
				this.declarer.getNation().removeTown(loser.getTown());
				this.winner.getNation().addTown(loser.getTown());
			} catch (Exception e) {
				System.out.println("[TownyWars] cleaning up after failed rebellion failed catastrophically!");
				e.printStackTrace();
			}
			TownyUniverse.getDataSource().removeNation(this.declarer.getNation());
			
			TownyWarsNation.removeNation(this.declarer);
		}

		// save the stuff we've been doing
		TownyUniverse.getDataSource().saveTowns();
	    TownyUniverse.getDataSource().saveNations();
	}

}
