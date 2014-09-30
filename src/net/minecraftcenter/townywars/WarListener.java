package net.minecraftcenter.townywars;

import java.util.UUID;

import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.minecraftcenter.townywars.War.WarType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WarListener
  implements Listener
{
  
//private TownyWars mplugin=null;
	
  WarListener(TownyWars aThis) { }//mplugin=aThis;}
  
  @EventHandler
  public void onNationDelete(DeleteNationEvent event) {
	  TownyWarsNation nation = TownyWarsNation.getNation(event.getNationName());
	  
	  // this nation needs to get removed from any wars it was in
	  for (War war : nation.getWars()) {
		  war.end();
	  }
	  
	  // save and remove the nation
	  TownyWarsNation.removeNation(nation);

  }
  
  @EventHandler
  public void onTownDelete(DeleteTownEvent event) {
	  TownyWarsTown.removeTown(TownyWarsTown.getTown(event.getTownName()));
  }
  
  // TODO: currently never executes; awaiting fix from Towny developer
  // in the meantime, the "/nation new " command is being captured below
  
 /* @EventHandler
  public void onNationCreation(NewNationEvent event)
  {
	  TownyWarsNation.putNation(event.getNation());
  }*/

  
  // listen for "/nation new" commands, since we need to catch the creation of a new nation
  // we'll handle all the nation creation stuff ourselves, we cancel the event
  @EventHandler
  public void onChat(PlayerCommandPreprocessEvent event) {
	  if (event.getMessage().contains("/nation new ")) {
		  event.setCancelled(true);
		  String strings[]=event.getMessage().split(" ");
		  Player player=event.getPlayer();
		  TownyWarsResident resident=TownyWarsResident.getResident(player.getUniqueId());
		  Town town=null;
		  boolean belongsToNation=false;
		  // need to check that the player belongs to a town, has authority, and the town doesn't already belong to a nation
		  try {
			town=resident.getResident().getTown();
		} catch (NotRegisteredException e1) {
			player.sendMessage(ChatColor.GOLD+"[Towny] "+ChatColor.RED+"Resident doesn't belong to any town.");
			return;
		}
		if (!resident.getResident().isMayor() && !town.hasAssistant(resident.getResident())) {
			player.sendMessage(ChatColor.GOLD+"[Towny] "+ChatColor.RED+"You are not the mayor or an assistant.");
			return;
		}
		
		try {
			resident.getResident().getTown().getNation();
			belongsToNation=true;
		} catch (NotRegisteredException e1) {
			// do nothing
		}
		if (belongsToNation) {
			player.sendMessage(ChatColor.GOLD+"[Towny] "+ChatColor.RED+"Target town already belongs to a nation.");
			return;
		}
		Nation nation=null;
		  try {
			TownyUniverse.getDataSource().newNation(strings[2]);
			nation=TownyUniverse.getDataSource().getNation(strings[2]);
		} catch (AlreadyRegisteredException e) {
			player.sendMessage(ChatColor.GOLD+"[Towny] "+ChatColor.RED+"Nation already exists.");
			return;
		} catch (NotRegisteredException e) {
			System.out.println("why did this happen?");
			return;
		}
		  
		// finally create the nation
		  Bukkit.getServer().broadcastMessage(ChatColor.AQUA+player.getName()+" created a new nation called "+nation.getName());
		  try {
			nation.addTown(resident.getResident().getTown());
		} catch (AlreadyRegisteredException e) {
			// safely ignoreable since we've just created the new nation
		} catch (NotRegisteredException e) {
			// safely ignoreable since we've just created the new nation
		}
		  TownyWarsNation.putNation(nation);
		  TownyUniverse.getDataSource().saveAll();
	  }
  }
  
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event)
  {
    Player player = event.getPlayer();
    long loginTime=System.currentTimeMillis();
    
    TownyWarsResident newResident=TownyWarsResident.getResident(player.getUniqueId());
    
    // add the player to the master list if they don't exist in it yet
    // we use player UUIDs to prevent confusion and spoofing
    if (newResident==null){
    	newResident=TownyWarsResident.putResident(player);
    	if (newResident==null) {
    		System.out.println("[TownyWars] error adding this player! <--- LOOK AT THIS");
    		return;
    	}
    	else {
    		System.out.println("new player added!");
    	}
    }
    newResident.setLastLoginTime(loginTime);
      Resident resident=null;
      TownyWarsNation nation=null;
	try {
		resident = TownyUniverse.getDataSource().getResident(player.getName());
		nation = TownyWarsNation.getNation(resident.getTown().getNation());
	} catch (NotRegisteredException e) {
		
		// the player is not registered for some reason or is not in a town or nation
		// clearly they don't need to be sent any messages
		return;
	}
      // it's possible the player may not be in a nation, so skip all this if so; otherwise. . .
      if (nation!=null) {
    	  
    	  // check that the player's stored lastNation is the same as the nation they're currently in and update if it's not
    	  if (nation!=TownyWarsResident.getResident(resident).getLastNation()) {
    		  TownyWarsResident.getResident(resident).setLastNation(nation);
    	  }
    	  
	      // inform the player about the wars currently going on
	      if (!nation.getWars().isEmpty()) {
	    	  for (War war : nation.getWars()) {
	    		  war.informPlayers(TownyWarsResident.getResident(player.getUniqueId()));
	    		  if (nation.getNation().hasAssistant(resident) || resident.isKing()) {
	    			  TownyWarsNation enemy=war.getEnemy(nation);
    				  if (war.getPeaceOffer(enemy)!=null) {
    					  player.sendMessage(ChatColor.GREEN+enemy.getNation().getName()+" has offered you peace:");
    					  player.sendMessage(war.getPeaceOffer(enemy));
    				  }
	    		  }
	    	  }
	      }
      }
  }
  
  // update player playtime stats and save the object when they quit
  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
	  Player player=event.getPlayer();
	  long logoutTime=System.currentTimeMillis();
	  TownyWarsResident resident=TownyWarsResident.getResident(player.getUniqueId());
	  resident.setLastLogoutTime(logoutTime);
	  resident.updateTotalPlayTime();
	  TownyWars.database.insertResident(resident);
  }
  
  @EventHandler
  public void onResidentLeave(TownRemoveResidentEvent event)
  {
    try
    {
    TownyWarsNation nation = TownyWarsNation.getNation(event.getTown().getNation());
      if (nation.isInWar()) {
  	    TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
  	    town.calculateMaxDP();
    	String message=ChatColor.RED+"Reminder: You cannot join another nation that is currently at war with the nation you've left!";
    	for (Resident resident : town.getTown().getResidents()) {
    		Player plr = Bukkit.getPlayer(resident.getName());
		      if (plr != null) {  
		        plr.sendMessage(message);
		      }
    	}
    	// check to see if this town ceasing to exist would cause any in-progress wars to stop; if so, end them
    	if (event.getTown().getResidents().size()==0) {
    		for (War war : nation.getWars()) {
    			if (war.getTargetTown().getTown()==event.getTown() && war.getWarType()==WarType.FLAG) {
    				war.end();
    			}
    		}
    	}
      }
    }
    catch (NotRegisteredException ex)
    {
      // do nothing; evidently the town the resident left was not in a nation
    }
    
    // but we need to check to see if the town is about to be disbanded because everyone left it
    /*if (event.getTown().getResidents().size()==0) {
    	System.out.println("removing town!");
    	//TownyWarsTown town=TownyWarsTown.getTown(event.getTown());
    	//TownyWars.database.insertTown(town, false);
    	TownyWarsTown.removeTown(event.getTown());
    }*/
    
  }
  
  @EventHandler
  public void onResidentAdd(TownAddResidentEvent event)
  {
	  // first we need to check if the town is in a nation that is in a war that the player has already been a part of
	  // if so, remove the player from the town
	  TownyWarsResident resident = TownyWarsResident.getResident(event.getResident());
	  
	  try {
		  TownyWarsNation nation = TownyWarsNation.getNation(event.getTown().getNation());
		  
		  // this could be null, so be careful; here we don't actually care about getting stuff out of this nation
		  TownyWarsNation lastNation=resident.getLastNation();
		  
		  // it's ok to rejoin the nation the player was last a part of
		  if (nation!=lastNation) {
			  for (War war : nation.getWars()) {
				  if (resident.isInActiveWar(war)) {
					  event.getTown().removeResident(event.getResident());
					  resident.getPlayer().sendMessage(ChatColor.RED+"You cannot join a nation who is in a war you have already participated in!");
					  return;
				  }
			  }
			  
			  // the player was allowed to join the new nation so update accordingly
			  resident.setLastNation(nation);
		  }
	} catch (NotRegisteredException e) {
		// not a problem, the town isn't in a nation
	} catch (EmptyTownException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	  // in the case that the player joined a town not in a nation, don't update lastNation for the player
	  // be careful, because if this is a new town, it may not exist yet in the hashmap
	  // if it doesn't, we'll put it in now
	  if (TownyWarsTown.getTown(event.getTown())==null) {
		  TownyWarsTown.putTown(event.getTown());
	  }
	  TownyWarsTown town = TownyWarsTown.getTown(event.getTown());
	  double oldMaxDP = town.getMaxDP();
	  double newMaxDP = town.calculateMaxDP();
	  town.modifyDP(newMaxDP-oldMaxDP);
  }
  
  @EventHandler
  public void onNationAdd(NationAddTownEvent event)
  {
	  TownyWarsTown town = TownyWarsTown.getTown(event.getTown());

	  // derate the town's starting DP based on the number of times it's been conquered so far in the wars it's been in
	  town.resetDP();
  }
  
  
  @EventHandler
  public void onNationRemove(NationRemoveTownEvent event)
  {
	  // we need to see if this was the last town removed, in which case, the nation ceases to exist
	  // we have to handle the nation removal here, because the Nation object is already gone when the NationDeletedEvent triggers!
	  if (event.getNation().getTowns().isEmpty()){
		  System.out.println("nation getting deleted!");
		// update all the lastNation field for all players of this shortly-nonexistent nation
		  for (Resident resident : event.getNation().getResidents()) {
			  TownyWarsResident.getResident(resident).setLastNation(null);
		  }
	  }
  }
  
@SuppressWarnings("deprecation")
@EventHandler
  public void onPlayerDamage(EntityDamageByEntityEvent event){
	  // get the current system time
	  long hitTime=System.currentTimeMillis();
	  
	  // check if the entity damaged was a player
	  if (event.getEntity() instanceof Player){
		  UUID attackerUUID=null;
		  
		  // check if the damaging entity was a player
		  if (event.getDamager() instanceof Player) {
			  attackerUUID=((Player)event.getDamager()).getUniqueId();
		  }
		  
		  // check if the damaging entity was an arrow shot by a player
		  else if (event.getDamager() instanceof Projectile){
			  if (((Projectile)event.getDamager()).getShooter() instanceof Player){
				  attackerUUID=((Player)((Projectile)event.getDamager()).getShooter()).getUniqueId();
			  }
		  }
		  
		  // if neither was true, then no need to update the player's stats
		  if (attackerUUID==null) { return;}
		  
		  //String playerName=((Player)event.getEntity()).getName();
		  UUID playerUUID=((Player)event.getEntity()).getUniqueId();
		  TownyWarsResident player=TownyWarsResident.getResident(playerUUID);
		  if (player!=null) {
			  // update the player's stats
			  player.setLastHitTime(hitTime);
			  player.setLastAttackerUUID(attackerUUID);
		  }
		  
	  }
  }
  
  // here we want to differentiate between player deaths due solely to environmental damage
  // and due to environmental damage in combination with player hits

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event)
  {
	  // record the timestamp immediately
	  long deathTime = System.currentTimeMillis();
	  
	  // get the dead resident
	  Player player = event.getEntity();
	  
	  // get the name of the cause of death
	  DamageCause damageCause=player.getLastDamageCause().getCause();
	  
	  Player playerKiller=null;
	  TownyWarsResident currentResident = TownyWarsResident.getResident(player.getUniqueId());
	  
	  System.out.println(player.getWorld().getName());
	  
	  // here, the kill was not done by a player, so we need to look up who to credit, if anyone
	  UUID lastAttackerUUID=null;
	  if (event.getEntity().getKiller()==null) {
		  
		  // let's look up who hit them last, and how long ago
		  long lastHitTime=0;
		  
		  
	
			  if (currentResident!=null){
				  lastHitTime=currentResident.getLastHitTime();
				  lastAttackerUUID=currentResident.getLastAttackerUUID();
			  }
			  
			  System.out.println((deathTime-lastHitTime)/1000+ " ago by "+ lastAttackerUUID);
			  
			  // if the player has been hit by another player within the past 30 seconds, credit the killer
			  if (lastAttackerUUID!=null && deathTime-lastHitTime<30000){
				  playerKiller=Bukkit.getServer().getPlayer(lastAttackerUUID);
				  // give the killer credit in chat :-)
				  event.setDeathMessage(event.getDeathMessage()+" while trying to escape "+playerKiller.getName());
			  }
			  // the player was not hit by another player within the past 30 seconds
			  // so let's see if the player's last attacker is still somewhere nearby (a chase might be afoot)
			  else {
				  for (Entity entity : event.getEntity().getNearbyEntities(10D, 10D, 10D)) {
					  if (entity.getUniqueId()==lastAttackerUUID) {
						  playerKiller=Bukkit.getServer().getPlayer(lastAttackerUUID);
						  // give the killer credit in chat :-)
						  event.setDeathMessage(event.getDeathMessage()+" while trying to escape "+playerKiller.getName());
					  }

				  }
			  }
			  
			  // let's check if the player was killed by a block explosion (TNT)
			  //if (damageCause==DamageCause.BLOCK_EXPLOSION) {
				//  event.
			  //}
			  
	  }
	  // kill was done by another player
	  else {
		  playerKiller=player.getKiller();
		  if (event.getEntity().getLastDamageCause().getCause()==DamageCause.FALL) {
			  event.setDeathMessage(event.getDeathMessage()+" while trying to escape "+playerKiller.getName());
		  }
	  }
	  if (currentResident!=null){
		// reset dead player's stats
		  currentResident.setLastAttackerUUID(null);
		  currentResident.setLastHitTime(0);
	  }
	  
	  // we need to record the kill in all its glory to a log file for moderation use
	  // takes in the time of death in milliseconds, the player that was killed, the killer, the final cause of death, and the death message
	  if (!TownyWars.database.insertKill(deathTime, player, playerKiller, damageCause.name(), event.getDeathMessage())) {
		  System.out.println("[TownyWars] death recording failed! you should check on this!");
	  }
	  else {
		  System.out.println("[TownyWars] death recorded!");
	  }
	  
	  // if the player actually wasn't killed by another player, we can stop
	  if (playerKiller==null) { return; }

	  // if we've made it this far, it means that the death should affect TownyWars
	  // now we know who to credit, so let's adjust Towny to match
	  
	  try {
	      TownyWarsTown attackerTown = TownyWarsTown.getTown(TownyUniverse.getDataSource().getResident(playerKiller.getName()).getTown());
	      TownyWarsNation attackerNation = TownyWarsNation.getNation(attackerTown.getTown().getNation());

	      TownyWarsTown defenderTown = TownyWarsTown.getTown(TownyUniverse.getDataSource().getResident(player.getName()).getTown());
	      TownyWarsNation defenderNation = TownyWarsNation.getNation(defenderTown.getTown().getNation());
	      
	      War sharedWar=WarManager.getSharedWar(attackerNation, defenderNation);
    	  if (sharedWar!=null) {
	    	  defenderTown.addDeath();
	    	  defenderNation.addDeath();
	    	  double currentDP = defenderTown.modifyDP(-1);
	    	  
	    	  // only allow town conquest in a normal war; conquest is not desirable in a rebellion or flag war, only DP damage
	    	  if (currentDP<=0 && sharedWar.getWarType()==WarType.NORMAL) {
	    		  // town was conquered, so update things
	    		  WarManager.moveTown(defenderTown, attackerNation);
	    	  }
	    	  else {
	    		  if (currentDP>0) {
	    			  event.getEntity().sendMessage(ChatColor.RED + "Warning! Your town only has " + currentDP + " DP left!");
	    		  }
	    		  else {
	    			  event.getEntity().sendMessage(ChatColor.RED + "Danger! Your town's DP is negative! (" + currentDP + ")");
	    		  }
	    	  }
	    	  if (sharedWar.checkWinner()!=null) {
	    		  sharedWar.end();
	    	  }
	      }
	  }
	  
	  // if an exception is thrown, one or more of the towns/nations could be resolved
	  // this is no problem because it means the players involved in the kill aren't in a war (since only nations can be at war)
	  catch (NotRegisteredException e) {
		  return;
	  }
	}
  
  // update the TownyWarsNation object if the linked Nation object's name changes
  @EventHandler
  public void onNationNameChange(RenameNationEvent event) {
	  TownyWarsNation.getNation(event.getNation()).setName(event.getNation().getName());
  }
  
	//update the TownyWarsTown object if the linked Town object's name changes
	 @EventHandler
	 public void onTownNameChange(RenameTownEvent event) {
		  TownyWarsTown.getTown(event.getTown()).setName(event.getTown().getName());
	 }
  
}
