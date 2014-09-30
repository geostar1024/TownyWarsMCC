package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.minecraftcenter.townywars.War.WarType;

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
	  
	  if (!database.saveAll()) {
		  System.out.println("[ERROR] could not save TownyWars state!!");
	  }
	  database.close();
  }
  
  
  public void onEnable()
  {
    
    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new WarListener(this), this);
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
    
    setupTownyWarsHashMaps();
    
    // load the current wars and set the PvP flag appropriately
    
    for (War war : WarManager.getWars()) {
    	// store the reference to this war in each of the TownyWarNation objects
		for (TownyWarsNation nation : war.getNations()) {
			nation.addWar(war);
		}
		
		// make all nations in this war enemies!
		for (TownyWarsNation nation1 : war.getNations()) {
			for (TownyWarsNation nation2 : war.getNations()) {
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
		if (war.getWarType()!=WarType.FLAG) {
			for (TownyWarsNation nation : war.getNations()) {
				for (Town town : nation.getNation().getTowns()) {
					town.setPVP(true);
				}
			}
		}
		// but if it's a flag war, only turn on pvp for the aggressor and the target town(s)
		else {
			for (Town town : war.getDeclarer().getNation().getTowns()) {
				town.setPVP(true);
			}
			war.getTargetTown().getTown().setPVP(true);
		}

    }
    
    
    TownyUniverse.getDataSource().saveNations();
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
		  System.out.println(nation);
		  if (TownyWarsNation.getNation(nation)==null){
			  TownyWarsNation.putNation(nation, new TownyWarsNation(nation));
		  }
	  }
	  TownyWarsNation.printnations();
	  for (Town town : TownyUniverse.getDataSource().getTowns()) {
		  if (TownyWarsTown.getTown(town)==null){
			  TownyWarsTown.putTown(town, new TownyWarsTown(town));
		  }
	  }
	  TownyWarsTown.printtowns();
  }
  
}
