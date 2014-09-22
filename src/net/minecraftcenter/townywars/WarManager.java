package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.HashSet;

import java.util.Set;

import net.minecraftcenter.townywars.War.WarStatus;
import net.minecraftcenter.townywars.War.WarType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WarManager
{
	
  private static Set<War> activeWars = new HashSet<War>();
  
  public static Set<War> getWars()
  {
    return activeWars;
  }
  
  public static void quickWar(TownyWarsNation declarer, TownyWarsNation target) {
	  
	  // make a default name using the declarer's name plus the last 6 digits of the current millisecond time
	  // ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
	  String random=Long.toString(System.currentTimeMillis());
	  String name=declarer.getNation().getName()+"-"+random.substring(random.length()-6,6);
	  
	  War war = new War(name,declarer);
	  
	  // add all the nations that were specified
	  war.setTarget(target);
	  
	  // set the type
	  war.setWarType(WarType.NORMAL);
	  
	  // to war!
	  war.execute();
  }
  
  public static void quickFlagWar(TownyWarsNation declarer, TownyWarsTown target, TownyWarsNation targetParent) {
	  
	  // make a default name using the declarer's name plus the last 6 digits of the current millisecond time
	  // ugly? yes, but it's their fault for doing things "ze kveek way" . . . .
	  String random=Long.toString(System.currentTimeMillis());
	  String name=declarer.getNation().getName()+"-"+random.substring(random.length()-6,6);
	  
	  War war = new War(name,declarer);
	  war.setTarget(targetParent);
	  
	  war.setTargetTown(target);
	  
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
	  war.setTarget(parent);
	  
	  // set the type
	  war.setWarType(WarType.REBELLION);
	  
	  // to war!
	  war.execute();
	  return true;
  }
  
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
	  TownyWarsNation enemy=war.getEnemy(nation);
	  for (Resident re : enemy.getNation().getResidents()) {
	      if ((re.isKing()) || (enemy.getNation().hasAssistant(re)))
	      {
	        Player plr = Bukkit.getPlayer(re.getName());
	        if (plr != null) {
	          plr.sendMessage(ChatColor.GREEN + nation.getName() + " has requested peace!");
	        }
	      }
	    }
	  if (war.allPeaceOffered()) {
		  war.end();
	  }
  }

	// try to end the war of the given name
	// returns true if successful
	public static boolean endWar(String warName) {
		if (warName==null) {
			return false;
		}
		for (War war : activeWars) {
			if (war.getName()==warName && war.getWarStatus()!=WarStatus.ENDED) {
				war.end();
				return true;
			}
		}
		return false;
	}
	
	// find out if two nations are in a war together
	public static War getSharedWar(TownyWarsNation nation1, TownyWarsNation nation2) {
		for (War war : nation1.getWars()) {
			if (war.hasMember(nation2)) {
				return war;
			}
		}
		return null;
	}
}
