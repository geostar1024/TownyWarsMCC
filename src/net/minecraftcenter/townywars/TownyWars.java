package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

//import main.java.com.danielrharris.townywars.War.MutableInteger;


import org.bukkit.Bukkit;
import org.bukkit.Location;
//import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class TownyWars extends JavaPlugin {
  public static TownyUniverse tUniverse;
  public static double pPlayer;
  public static double pPlot;
  public static double pKill;
  public static double declareCost;
  public static double endCost;
  
  public static Map<UUID,TownyWarsResident> allTownyWarsResidents = new HashMap<UUID,TownyWarsResident>();
  public static Map<Resident,TownyWarsResident> residentToTownyWarsResidentHash = new HashMap<Resident,TownyWarsResident>();
  public static Map<Nation,TownyWarsNation> nationToTownyWarsNationHash = new HashMap<Nation,TownyWarsNation>();
  public static Map<Town,TownyWarsTown> townToTownyWarsTownHash = new HashMap<Town,TownyWarsTown>();
  
  private static final Charset utf8 = StandardCharsets.UTF_8;
  
  private static final String nationsFile="nations.txt";
  
  private static final String warsFile="wars.txt";

  public void onDisable()
  {
	  saveTownyWarsNations(nationsFile);
    try
    {
      WarManager.save();
    }
    catch (Exception ex)
    {
      Logger.getLogger(TownyWars.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  
  public void onEnable()
  {
	  
	loadTownyWarsNations(nationsFile);
	loadWarsData(warsFile);
    try
    {
      WarManager.load(getDataFolder());
    }
    catch (Exception ex)
    {
      Logger.getLogger(TownyWars.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new WarListener(this), this);
    getCommand("twar").setExecutor(new WarExecutor(this));
    tUniverse = ((Towny)Bukkit.getPluginManager().getPlugin("Towny")).getTownyUniverse();
    for(Town town : TownyUniverse.getDataSource().getTowns()){
    	town.setAdminEnabledPVP(false);
    	town.setAdminDisabledPVP(false);
    	town.setPVP(false);
    }
    
    // load the current wars and set the PvP flag appropriately
    
    for (String nationName : allWars.keySet()) {
    	Nation nation=null;
    	try {
			nation=TownyUniverse.getDataSource().getNation(nationName);
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			System.out.println("[TownyWars] couldn't set PvP status for "+nationName+"! Specified nation doesn't seem to exist!");
			e.printStackTrace();
		}
    	for (Town town : nation.getTowns()) {
    		town.setPVP(true);
    	}
    }
    
    
    /*for (War w : WarManager.getWars()) {
      for (Nation nation : w.getNationsInWar()) {
          for (Town t : nation.getTowns()) {
            t.setPVP(true);
          }
      }
    }*/
    
    
    
    TownyUniverse.getDataSource().saveTowns();
    
    getConfig().addDefault("pper-player", Double.valueOf(2.0D));
    getConfig().addDefault("pper-plot", Double.valueOf(0.5D));
    getConfig().addDefault("declare-cost", Double.valueOf(10.0D));
    getConfig().addDefault("end-cost", Double.valueOf(0.0D));
    getConfig().addDefault("death-cost", Double.valueOf(0.0D));
    getConfig().options().copyDefaults(true);
    saveConfig();
    
    pPlayer = getConfig().getDouble("pper-player");
    pPlot = getConfig().getDouble("pper-plot");
    declareCost = getConfig().getDouble("declare-cost");
    endCost = getConfig().getDouble("end-cost");
    pKill = getConfig().getDouble("death-cost");
    
    // load all players into the TownyWarsResidents hashmap
    // usually all players will be Towny Residents, but we can't be completely sure, so just get the whole list
    // usually only needed on reloads
    try{
    	for (Player player : Bukkit.getServer().getWorlds().get(0).getPlayers()) {
    		if (allTownyWarsResidents.get(player.getUniqueId())==null){
    			addTownyWarsResident(player);
    		}
    	}
    	System.out.println(tUniverse.getActiveResidents().size()+" residents added!");
    }catch (Exception ex)
    {
        System.out.println("failed to add residents!");
        ex.printStackTrace();
      }
    
  }
  
  public void addTownyWarsResident(Player player){
	  Resident resident=null;
	try {
		resident=TownyUniverse.getDataSource().getResident(player.getName());
		
	} catch (NotRegisteredException e) {
		// this is really bad; this should never happen
		System.out.println("[TownyWars] player not registered!");
		e.printStackTrace();
		return;
	}
	TownyWarsResident newPlayer = new TownyWarsResident(player,resident);
	allTownyWarsResidents.put(player.getUniqueId(),newPlayer);
	residentToTownyWarsResidentHash.put(resident, newPlayer);
  }
  
  public void setupTownyWarsHashMaps(){
	  for (Nation nation : TownyUniverse.getDataSource().getNations()) {
		  if (nationToTownyWarsNationHash.get(nation)==null){
			  TownyWarsNation townyWarsNation=new TownyWarsNation(nation);
			  nationToTownyWarsNationHash.put(nation, townyWarsNation);
		  }
		  for (Town town : nation.getTowns()) {
			  if (townToTownyWarsTownHash.get(town)==null){
				  TownyWarsTown townyWarsTown=new TownyWarsTown(town);
				  townToTownyWarsTownHash.put(town, townyWarsTown);
			  }
		  }
	  }
  }
  
  
  public void loadTownyWarsNations(String savefile){
	  List<String> townyWarsData=null;
	  try {
		townyWarsData=Files.readAllLines(Paths.get(savefile), utf8);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		System.out.println("[TownyWars] file I/O error while loading nations!");
		e.printStackTrace();
	}
	  if (townyWarsData!=null) {
		  for (String townyWarsLine : townyWarsData) {
			  if (townyWarsLine.contains("save time")) continue;
			  String nationLine[]=townyWarsLine.split(" ");
			  Nation currentNation=null;
			  try {
				  currentNation = TownyUniverse.getDataSource().getNation(nationLine[0]);
				  } catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					System.out.println("[TownyWars] error loading nation "+nationLine[0]+"!");
					e.printStackTrace();
				}
				if (currentNation!=null && nationToTownyWarsNationHash.get(currentNation)==null) {
				  nationToTownyWarsNationHash.put(currentNation, new TownyWarsNation(currentNation));
				}
			  for (int k=1; k<nationLine.length;k+=2) {
				  Town currentTown=null;
				  try {
						currentTown = TownyUniverse.getDataSource().getTown(nationLine[k]);
					} catch (NotRegisteredException e) {
						// TODO Auto-generated catch block
						System.out.println("[TownyWars] error loading town "+nationLine[k]+"!");
						e.printStackTrace();
					}
				  if (currentTown!=null) {
					  if (townToTownyWarsTownHash.get(currentTown)==null) {
						  townToTownyWarsTownHash.put(currentTown, new TownyWarsTown(currentTown));
					  }
					  townToTownyWarsTownHash.get(currentTown).setDP(Double.parseDouble(nationLine[k+1]));
				  }
			  }
		  }
	  }
  }
  
public void saveTownyWarsNations(String savefile){
	
	List<String> townyWarsData=Arrays.asList("save time: "+Long.toString(System.currentTimeMillis()));
	  for (Nation nation : nationToTownyWarsNationHash.keySet()) {
		  String nationString=nation.getName();
		  for (Town town : nation.getTowns()) {
			  TownyWarsTown townyWarsTown=townToTownyWarsTownHash.get(town);
			  if (townyWarsTown!=null) {
				  nationString.concat(" "+town.getName()+" "+Double.toString(townyWarsTown.getDP()));
			  }
		  }
		  townyWarsData.add(nationString);
	  }
	  try {
		Files.write(Paths.get(savefile), townyWarsData, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		System.out.println("[TownyWars] file I/O error while saving nations!");
		e.printStackTrace();
	}
  }

public void saveWarsData(String savefile){
	
	
	List<String> warsData=Arrays.asList("save time: "+Long.toString(System.currentTimeMillis()));
	
	// each line has the following format (space separated):
	// <war uuid> <war name>  <nation1 uuid> <nation2 uuid>, . . .
	for (War war : allWars) {
		String aWar=war.getUUID()+" "+war.getName();
		for (String aCombatant : currentCombatants) {
			combatantString.concat(" "+aCombatant);
		}
		warsData.add(combatantString);
	}
	try {
		Files.write(Paths.get(savefile), warsData, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		System.out.println("[TownyWars] file I/O error while saving wars!");
	}
	
}

public void loadWarsData(String savefile){
	 List<String> warsData=null;
	  try {
		warsData=Files.readAllLines(Paths.get(savefile), utf8);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		System.out.println("[TownyWars] file I/O error while loading wars!");
		e.printStackTrace();
	}
	  if (warsData!=null) {
	  	for (String warLine : warsData) {
	  		String currentWar[]=warLine.split(" ");
	  		if (allWars.get(currentWar[0])!=null) {
	  			String combatants[]={currentWar[1],currentWar[2]};
	  			allWars.put(currentWar[0], combatants);
	  		}
	  	}
	  }
}
  
}
