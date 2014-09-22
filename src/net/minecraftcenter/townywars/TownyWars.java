package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
  
  public static Map<UUID,TownyWarsResident> allTownyWarsResidents = new HashMap<UUID,TownyWarsResident>();
  public static Map<Resident,TownyWarsResident> residentToTownyWarsResidentHash = new HashMap<Resident,TownyWarsResident>();
  public static Map<Nation,TownyWarsNation> nationToTownyWarsNationHash = new HashMap<Nation,TownyWarsNation>();
  public static Map<Town,TownyWarsTown> townToTownyWarsTownHash = new HashMap<Town,TownyWarsTown>();

  public void onDisable()
  {
	  
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
  
}
