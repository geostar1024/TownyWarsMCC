package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.util.FileMgmt;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//import net.minecraftcenter.townywars.War.MutableInteger;
import net.minecraftcenter.townywars.War.WarType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarManager
{

	/*public static enum WarStatus {
		BEGIN,
		END,
		PEACE,
		SURRENDER,
		CONQUERED,
		CONQUEROR,
		CURRENT,
	}*/
	
  private static String fileSeparator = System.getProperty("file.separator");
  private static Set<War> activeWars = new HashSet<War>();
  private static Set<String> requestedPeace = new HashSet<String>();
  //public static Map<String, MutableInteger> neutral = new HashMap<String, MutableInteger>();
  public static Town townremove;
  //private static final int SAVING_VERSION = 1;
  
 /* public static void save()
    throws Exception
  {
	  FileMgmt.CheckYMLExists(new File("plugins" + fileSeparator + "TownyWars" + fileSeparator + "activeWars.yml"));
	    if(!WarManager.getWars().isEmpty()){
		    String s = new String("");
		    
		    for(War w : WarManager.getWars())
		    	s += w.objectToString() + "\n";
		    
		    s = s.substring(0, s.length()-1);
		    
		    FileMgmt.stringToFile(s, "plugins" + fileSeparator + "TownyWars" + fileSeparator + "activeWars.yml");
		 } else
	    	FileMgmt.stringToFile("", "plugins" + fileSeparator + "TownyWars" + fileSeparator + "activeWars.yml");
    
    //save Rebellions
    //tripple space to separate rebellion objects
    FileMgmt.CheckYMLExists(new File("plugins" + fileSeparator + "TownyWars" + fileSeparator + "rebellions.yml"));
    if(!Rebellion.getAllRebellions().isEmpty()){
	    String s = new String("");
	    
	    for(Rebellion r : Rebellion.getAllRebellions())
	    	s += r.objectToString() + "\n";
	    
	    s = s.substring(0, s.length()-1);
	    
	    FileMgmt.stringToFile(s, "plugins" + fileSeparator + "TownyWars" + fileSeparator + "rebellions.yml");
	 } else
    	FileMgmt.stringToFile("", "plugins" + fileSeparator + "TownyWars" + fileSeparator + "rebellions.yml");
  }
  
  public static void load(File dataFolder)
    throws Exception
  {
	  	String folders[] = {"plugins" + fileSeparator + "TownyWars"};
	  	FileMgmt.checkFolders(folders);
	  	
	  	 //load rebellions
	    FileMgmt.CheckYMLExists(new File("plugins" + fileSeparator + "TownyWars" + fileSeparator + "rebellions.yml"));
	    String s = FileMgmt.convertFileToString(new File("plugins" + fileSeparator + "TownyWars" + fileSeparator + "rebellions.yml"));
	    
	    if(!s.isEmpty()){
		    ArrayList<String> slist = new ArrayList<String>();
		    
		    for(String temp : s.split("\n"))
		    	slist.add(temp);
		    
		    for(String s2 : slist)
		    	Rebellion.getAllRebellions().add(new Rebellion(s2));
	    }
	    
	    //load wars
	  	FileMgmt.CheckYMLExists(new File("plugins" + fileSeparator + "TownyWars" + fileSeparator + "activeWars.yml"));
	    String sw = FileMgmt.convertFileToString(new File("plugins" + fileSeparator + "TownyWars" + fileSeparator + "activeWars.yml"));
	    
	    if(!sw.isEmpty()){
		    ArrayList<String> slist = new ArrayList<String>();
		    
		    for(String temp : sw.split("\n"))
		    	slist.add(temp);
		    
		    for(String s2 : slist)
		    	WarManager.getWars().add(new War(s2));
	    }
  }*/
  
  public static Set<War> getWars()
  {
    return activeWars;
  }
  
  public static void quickWar(TownyWarsNation declarer, List<TownyWarsNation> nations) {
	  
	  // make a default name using the declarer's name plus the current millisecond time
	  // ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
	  String name=declarer.getNation().getName()+"-"+Long.toString(System.currentTimeMillis());
	  
	  War war = new War(name,declarer);
	  
	  // add all the nations that were specified
	  for (TownyWarsNation nation : nations) {
		  war.addMember(nation);
	  }
	  
	  // set the type
	  war.setWarType(WarType.NORMAL);
	  
	  // to war!
	  war.execute();
  }
  
  public static void quickFlagWar(TownyWarsNation declarer, TownyWarsTown target, TownyWarsNation targetParent) {
	  
	  // make a default name using the declarer's name plus the current millisecond time
	  // ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
	  String name=declarer.getNation().getName()+"-"+Long.toString(System.currentTimeMillis());
	  
	  War war = new War(name,declarer);
	  war.addMember(targetParent);
	  
	  war.addTarget(target);
	  
	  war.setWarType(WarType.FLAG);
	  
	  war.execute();
  }
  
  public static boolean quickRebellion(String name, TownyWarsTown rebelTown, TownyWarsNation parent) {
	  // make a default name for the war using the rebel town's name plus the current millisecond time
	  // ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
	  String warName=rebelTown.getTown().getName()+"-"+Long.toString(System.currentTimeMillis());
	  
	  TownyWarsNation rebelNation=null;
	  
	  // try to make a new nation . . .
	  try {
		  TownyUniverse.getDataSource().newNation(name);
		  rebelNation = TownyWars.nationToTownyWarsNationHash.get(TownyUniverse.getDataSource().getNation(name));
	  }
	  catch (Exception e) {
		  System.out.println("[TownyWars] quick rebellion creation failed catastrophically!");
		  e.printStackTrace();
		  return false;
	  }
	  
	  // . . .and move the town
	  try {
		  parent.getNation().removeTown(rebelTown.getTown());
		  rebelNation.getNation().addTown(rebelTown.getTown());
	  } catch (Exception e) {
		  System.out.println("[TownyWars] quick rebellion town movement failed catastrophically!");
		  e.printStackTrace();
		  return false;
	  }
	  
	  War war = new War(warName,rebelNation);
	  
	  // the only other member of the war is the parent nation, of course
	  war.addMember(parent);
	  
	  // set the type
	  war.setWarType(WarType.REBELLION);
	  
	  // to war!
	  war.execute();
	  return true;
  }
  
  // a new war is born . . . send back a reference so it can be worked on more
  public static War newWar(String name, TownyWarsNation declarer) {
	  return new War(name,declarer);
  }
  
  
  /*public static void createWar(TownyWarsNation nations[], War.WarType type) {
	  War war = new War(nations, type);
	  activeWars.add(war);
	  for (int k=0; k<nations.length; k++) {
		  informPlayers(nations[k], war, WarStatus.BEGIN, null);
	  }
   
	}
  public static void createWar(TownyWarsNation nations[], War.WarType type, TownyWarsTown target) {
	  if (type==WarType.REBELLION) {
		  setupRebellion(nations[0],nations[1]);
	  }
	  War war = new War(nations, type, target);
	  activeWars.add(war);
	  for (int k=0; k<nations.length; k++) {
		  informPlayers(nations[k], war, WarStatus.BEGIN, target);
	  }
  }
  
  public static boolean setupRebellion(TownyWarsTown rebel, TownyWarsNation parent) {
	  if (!parent.getNation().hasTown(rebel.getTown())){
		  return false;
	  }
	  else {
		  TownyUniverse.getDataSource().newNation(name + "-rebels");
	  }
  }
  
  public static void informPlayers(TownyWarsNation nation, War war, WarStatus warStatus) {
	  informPlayers(nation,null,war,warStatus);
  }

  public static void informPlayers(TownyWarsNation nation, TownyWarsResident resident, War war, WarStatus warStatus) {
	// only need to construct the message once per nation
	WarType warType=war.getWarType();
	String message="";
	if (nation.getNation().hasResident(TownyUniverse.getDataSource().getResident(resident.getPlayer().getName()))) {
		message+=ChatColor.RED+"Your nation ";
	}
	else {
		message+=ChatColor.BLUE+nation.getNation().getName()+" ";
	}
	switch(warStatus) {
	case BEGIN:
		message+="has entered";
		break;
	case CONQUERED:
		message+="has been conquered in";
		break;
	case CONQUEROR:
		message+="has won";
		break;
	case END:
		message+="has ended";
		break;
	case PEACE:
		message+="has made peace in";
		break;
	case SURRENDER:
		message+="has surrendered in";
		break;
	case CURRENT:
		message+="is in";
		break;
	default:
		break;
	}
	message+=" a ";
	switch(warType){
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
	message+=" against ";
	Set<TownyWarsNation> enemies=war.getEnemies(nation);
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
  	if (warType==WarType.FLAG) {
  		message+=", with target "+war.getTarget().getTown().getName();
  	}
  	message+="!";
  	
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
}*/
  
  // handles the nitty gritty of conquest
  // this will trigger the Towny event NationRemoveTownEvent
  public static void moveTown(TownyWarsTown town, TownyWarsNation newNation) {
	  TownyWarsNation oldNation=null;
	try {
		oldNation = TownyWars.nationToTownyWarsNationHash.get(town.getTown().getNation());
	} catch (NotRegisteredException e) {
		System.out.println("[TownyWars] moveTown: getting the town to move failed catastrophically!");
		e.printStackTrace();
	}
	  
	  // we'll need to know later if the town that's being moved was a capital
	  boolean wasCapital=town.getTown().isCapital();
	  try {
		oldNation.getNation().removeTown(town.getTown());
		newNation.getNation().addTown(town.getTown());
	} catch (Exception e) {
		System.out.println("[TownyWars] moveTown: moving the town failed catastrophically!");
		e.printStackTrace();
	}
	  if (wasCapital) {
		  // the old capital is no longer part of the town
		  oldNation.removeCapitalPriorityForTown(town);
		  
		  // set the new capital to the first item in the capitalPriority list
		  oldNation.getNation().setCapital(oldNation.getNextCapital().getTown());
	  }
	  
	  // now increment the town's conquered state and reinitialize its DP
	  town.addConquered();
  }
  
  // sets a generic peace request for the given nation in the given war
  // if everyone wants peace, the war gets ended
  public static void requestPeace(War war, TownyWarsNation nation) {
	  war.setPeaceOffer(nation, "peace");
	  if (war.allPeace()) {
		  war.end();
		  activeWars.remove(war);
	  }
  }
  
  
/*public static boolean requestPeace(Nation nat, Nation onat, boolean admin)
  {
	  
    if ((admin) || (requestedPeace.contains(onat.getName())))
    {
      if(getWarForNation(nat).getRebellion() != null)
    	  getWarForNation(nat).getRebellion().peace();
      endWar(nat, onat, true);
      
      try
      {
        nat.collect(TownyWars.endCost);
        onat.collect(TownyWars.endCost);
      }
      catch (EconomyException ex)
      {
        Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
      }
      return true;
    }
    if (admin)
    {
      endWar(nat, onat, true);
      return true;
    }
    requestedPeace.add(nat.getName());
    for (Resident re : onat.getResidents()) {
      if ((re.isKing()) || (onat.hasAssistant(re)))
      {
        Player plr = Bukkit.getPlayer(re.getName());
        if (plr != null) {
          plr.sendMessage(ChatColor.GREEN + nat.getName() + " has requested peace!");
        }
      }
    }
    return false;
  }*/

	// try to end the war of the given name
	// returns true if successful
	public static boolean endWar(String warName) {
		if (warName==null) {
			return false;
		}
		for (War war : activeWars) {
			if (war.getName()==warName) {
				war.end();
				return true;
			}
		}
		return false;
	}
	
	// try to end the war of the given name
		// returns true if successful
	public static boolean endWar(War war, TownyWarsNation nation) {
		war.endWarForNation(nation);
		if (war.getNations().isEmpty()) {
			activeWars.remove(war);
		}
		return true;
	}
	
	public static War getSharedWar(TownyWarsNation nation1, TownyWarsNation nation2) {
		for (War war : activeWars) {
			if (war.isMember(nation1) && war.isMember(nation2)) {
				return war;
			}
		}
		return null;
	}

	/*&public static void endWar(War war, TownyWarsNation winner) {
		// figure out what the outcome should be
		switch(war.getWarType()) {
		case FLAG:
			break;
		case NORMAL:
			break;
		case REBELLION:
			break;
		default:
			break;
		}
		informPlayers(winner,war.getWarType(),WarStatus.CONQUEROR,null);
		
		
		// end the war and remove it from the active war list
		// TODO: should also record the outcome here!
		//war.endWar(winner);
		activeWars.remove(war);
	}*/
  

 /* public static void endWar(Nation winner, Nation looser, boolean peace)
  {
	boolean isRebelWar = WarManager.getWarForNation(winner).getRebellion() != null;
	Rebellion rebellion = WarManager.getWarForNation(winner).getRebellion();
	
	try
	{
	   TownyUniverse.getDataSource().getNation(winner.getName()).removeEnemy(looser);
	   TownyUniverse.getDataSource().getNation(looser.getName()).removeEnemy(winner);
	    }
	    catch (NotRegisteredException ex)
	    {
	      Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
	    }
    
    activeWars.remove(getWarForNation(winner));
    requestedPeace.remove(looser.getName());
    War.broadcast(winner, ChatColor.GREEN + "You are now at peace!");
    War.broadcast(looser, ChatColor.GREEN + "You are now at peace!");
    for (Town t : winner.getTowns()) {
      t.setPVP(false);
    }
    
    //rebels win
    if(!peace && isRebelWar && winner == rebellion.getRebelnation()){
		War.broadcast(looser, ChatColor.RED + winner.getName() + " won the rebellion and are now free!");
		War.broadcast(winner, ChatColor.GREEN + winner.getName() + " won the rebellion and are now free!");
    	rebellion.success();
    	Rebellion.getAllRebellions().remove(rebellion);
    	TownyUniverse.getDataSource().removeNation(winner);
        winner.clear();
        TownyWars.tUniverse.getNationsMap().remove(winner.getName());
    }
    
    //rebelwar white peace
    if(isRebelWar && peace){
    	if(winner != rebellion.getMotherNation()){
	    	TownyUniverse.getDataSource().removeNation(winner);
		    TownyWars.tUniverse.getNationsMap().remove(winner.getName());
    	} else{
    		TownyUniverse.getDataSource().removeNation(looser);
		    TownyWars.tUniverse.getNationsMap().remove(looser.getName());
    	}
    }
    
    //TODO risk of concurrentmodificationexception please fix or something
    for (Town t : looser.getTowns())
    {
      if (!peace && !isRebelWar) {
        try
        {
          WarManager.townremove = t;
          looser.removeTown(t);
          winner.addTown(t);
        }
        catch (Exception ex)
        {
          Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      t.setPVP(false);
    }
    if (!peace && isRebelWar && winner != rebellion.getRebelnation())
    {
      TownyUniverse.getDataSource().removeNation(looser);
      looser.clear();
      TownyWars.tUniverse.getNationsMap().remove(looser.getName());
    }
    Rebellion.getAllRebellions().remove(rebellion);
    
    if(looser.getTowns().size() == 0)
    	TownyUniverse.getDataSource().removeNation(looser);
    if(winner.getTowns().size() == 0)
    	TownyUniverse.getDataSource().removeNation(winner);
    
    TownyUniverse.getDataSource().saveTowns();
    TownyUniverse.getDataSource().saveNations();
    try {
		WarManager.save();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }*/
  
  public static boolean hasBeenOffered(War ww, Nation nation)
  {
    try {
		return requestedPeace.contains(ww.getEnemy(nation));
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
    return false;
  }
}
