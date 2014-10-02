package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.minecraftcenter.townywars.interfaces.Attackable;
import net.minecraftcenter.townywars.object.TownyWarsNation;

import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyWars extends JavaPlugin {
  public static TownyUniverse tUniverse;
  public static double pPlayer;
  public static double pPlot;
  public static double pKill;
  public static double declareCost;
  public static double endCost;
  
  
  private final static String databasefile="townywars.db";
  
  public static TownyWarsDatabase database=null;

  public void onDisable()
  {
	  
	  //if (!database.saveAll()) {
		//  System.out.println("[ERROR] could not save TownyWars state!!");
	  //}
	  database.close();
  }
  
  
  public void onEnable()
  {
    
    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new TownyWarsListener(), this);
    pm.registerEvents(new TownyWarsPlayerListener(), this);
    getCommand("twar").setExecutor(new WarExecutor(this));
    tUniverse = ((Towny)Bukkit.getPluginManager().getPlugin("Towny")).getTownyUniverse();
    for(Town town : TownyUniverse.getDataSource().getTowns()){
    	town.setAdminEnabledPVP(false);
    	town.setAdminDisabledPVP(false);
    	town.setPVP(false);
    }
    
    // get a database object based on the specified database file
    database=new TownyWarsDatabase(databasefile);
    
    // create necessary database tables if they don't already exist
    database.createTables();
    
    database.loadAll();
    
    //setupTownyWarsHashMaps();
    
    // load the current wars and set the PvP flag appropriately
    
    for (War war : WarManager.getWars()) {
    	// store the reference to this war in each of the TownyWarNation objects
		for (Attackable org : war.getOrgs()) {
			org.addWar(war);
		}
		
		// make all nations in this war enemies!
		for (Attackable org1 : war.getOrgs()) {
			for (Attackable org2 : war.getOrgs()) {
				if (org1!=org2) {
					try {
						// skip enemies stuff if the orgs are just towns
						if (org1.getClass().equals(TownyWarsTown.class) || org2.getClass().equals(TownyWarsTown.class)) {
							continue;
						}
							((TownyWarsNation)org1).getNation().addEnemy(((TownyWarsNation)org2).getNation());
					} catch (AlreadyRegisteredException e) {
						// ignore if a nation is already the enemy of another nation; could happen if two nations happen to be in multiple wars
					}
				}
			}
		}
		
		// turn on pvp for all the towns in all the orgs at war!
		for (Attackable org : war.getOrgs()) {
			if (org.getClass().equals(TownyWarsNation.class)) {
				for (Town town : ((TownyWarsNation)org).getNation().getTowns()){
					town.setPVP(true);
				}
			}
			if (org.getClass().equals(TownyWarsTown.class)) {
				((TownyWarsTown)org).getTown().setPVP(true);
			}
		}

    }
    
    
    TownyUniverse.getDataSource().saveAll();
    
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
   			if (TownyWarsResident.putResident(player)==null) {
   				System.out.println("[TownyWars] error loading player '"+player.getName()+"'!");
   			}
    	}
    	System.out.println(tUniverse.getActiveResidents().size()+" residents added!");
    }catch (Exception ex)
    {
        System.out.println("failed to add residents!");
        ex.printStackTrace();
      }
    
  }
 
  
  public void setupTownyWarsHashMaps(){
	  for (Nation nation : TownyUniverse.getDataSource().getNations()) {
		  if (TownyWarsNation.getNation(nation)==null){
			  TownyWarsNation.putNation(new TownyWarsNation(nation));
		  }
	  }
	  for (Town town : TownyUniverse.getDataSource().getTowns()) {
		  if (TownyWarsTown.getTown(town)==null){
			  TownyWarsTown.putTown(new TownyWarsTown(town));
		  }
	  }
  }
  
}
