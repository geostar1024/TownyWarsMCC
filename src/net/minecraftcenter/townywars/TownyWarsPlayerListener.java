package net.minecraftcenter.townywars;

import java.util.UUID;

import net.minecraftcenter.townywars.interfaces.Attackable;
import net.minecraftcenter.townywars.object.TownyWarsNation;
import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;
import net.minecraftcenter.townywars.object.War.WarType;

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

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyWarsPlayerListener implements Listener {

	// listen for "/nation new " commands, since we need to catch the creation of a new nation
	// we'll handle all the nation creation stuff ourselves, so we cancel the event immediately
	@EventHandler
	public void onChat(final PlayerCommandPreprocessEvent event) {
		if (event.getMessage().contains("/nation new ")) {
			event.setCancelled(true);
			final String strings[] = event.getMessage().split(" ");
			final Player player = event.getPlayer();
			final TownyWarsResident resident = TownyWarsResident.getResident(player.getUniqueId());
			Town town = null;
			boolean belongsToNation = false;
			// need to check that the player belongs to a town, has authority,
			// and the town doesn't already belong to a nation
			try {
				town = resident.getResident().getTown();
			} catch (final NotRegisteredException e1) {
				player.sendMessage(ChatColor.GOLD + "[Towny] " + ChatColor.RED + "Resident doesn't belong to any town.");
				return;
			}
			if (!resident.getResident().isMayor() && !town.hasAssistant(resident.getResident())) {
				player.sendMessage(ChatColor.GOLD + "[Towny] " + ChatColor.RED + "You are not the mayor or an assistant.");
				return;
			}

			try {
				resident.getResident().getTown().getNation();
				belongsToNation = true;
			} catch (final NotRegisteredException e1) {
				// do nothing
			}
			if (belongsToNation) {
				player.sendMessage(ChatColor.GOLD + "[Towny] " + ChatColor.RED + "Target town already belongs to a nation.");
				return;
			}
			Nation nation = null;
			try {
				TownyUniverse.getDataSource().newNation(strings[2]);
				nation = TownyUniverse.getDataSource().getNation(strings[2]);
			} catch (final AlreadyRegisteredException e) {
				player.sendMessage(ChatColor.GOLD + "[Towny] " + ChatColor.RED + "Nation already exists.");
				return;
			} catch (final NotRegisteredException e) {
				System.out.println("why did this happen?");
				return;
			}

			// finally create the nation
			Bukkit.getServer().broadcastMessage(ChatColor.AQUA + player.getName() + " created a new nation called " + nation.getName());
			try {
				nation.addTown(resident.getResident().getTown());
			} catch (final AlreadyRegisteredException e) {
				// safely ignoreable since we've just created the new nation
			} catch (final NotRegisteredException e) {
				// safely ignoreable since we've just created the new nation
			}
			TownyWarsNation.putNation(new TownyWarsNation(nation));
			TownyWarsNation newNation=TownyWarsNation.getNation(nation);
			for (Resident re : nation.getResidents()) {
				TownyWarsResident resident1 = TownyWarsResident.getResident(re);
				if (resident1 != null) {
					resident1.addOrg(newNation);
				}
			}
			newNation.save();
			TownyUniverse.getDataSource().saveAll();
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerDamage(final EntityDamageByEntityEvent event) {
		// get the current system time
		final long hitTime = System.currentTimeMillis();

		// check if the entity damaged was a player
		if (event.getEntity() instanceof Player) {
			UUID attackerUUID = null;

			// check if the damaging entity was a player
			if (event.getDamager() instanceof Player) {
				attackerUUID = ((Player) event.getDamager()).getUniqueId();
			}

			// check if the damaging entity was an arrow shot by a player
			else if (event.getDamager() instanceof Projectile) {
				if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
					attackerUUID = ((Player) ((Projectile) event.getDamager()).getShooter()).getUniqueId();
				}
			}

			// if neither was true, then no need to update the player's stats
			if (attackerUUID == null) {
				return;
			}

			final TownyWarsResident player = TownyWarsResident.getResident(((Player) event.getEntity()).getUniqueId());
			if (player != null) {
				// update the player's stats
				player.setLastHitTime(hitTime);
				player.setLastAttackerUUID(attackerUUID);
			}

		}
	}

	// here we want to differentiate between player deaths due solely to environmental damage
	// and due to environmental damage in combination with player hits
	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent event) {
		// record the timestamp immediately
		final long deathTime = System.currentTimeMillis();

		// get the dead resident
		final Player player = event.getEntity();

		// get the name of the cause of death
		final DamageCause damageCause = player.getLastDamageCause().getCause();

		Player playerKiller = null;
		final TownyWarsResident resident = TownyWarsResident.getResident(player.getUniqueId());

		System.out.println(player.getWorld().getName());

		// here, the kill was not done by a player, so we need to look up who to credit, if anyone
		UUID lastAttackerUUID = null;
		if (event.getEntity().getKiller() == null) {

			// let's look up who hit them last, and how long ago
			long lastHitTime = 0;

			if (resident != null) {
				lastHitTime = resident.getLastHitTime();
				lastAttackerUUID = resident.getLastAttackerUUID();
			}

			System.out.println(((deathTime - lastHitTime) / 1000) + " ago by " + lastAttackerUUID);

			// if the player has been hit by another player within the past 30 seconds, credit the killer
			if ((lastAttackerUUID != null) && ((deathTime - lastHitTime) < 30000)) {
				playerKiller = Bukkit.getServer().getPlayer(lastAttackerUUID);
				// give the killer credit in chat :-)
				event.setDeathMessage(event.getDeathMessage() + " while trying to escape " + playerKiller.getName());
			}
			// the player was not hit by another player within the past 30 seconds
			// so let's see if the player's last attacker is still somewhere nearby (a chase might be afoot)
			else {
				for (final Entity entity : event.getEntity().getNearbyEntities(10D, 10D, 10D)) {
					if (entity.getUniqueId() == lastAttackerUUID) {
						playerKiller = Bukkit.getServer().getPlayer(lastAttackerUUID);
						// give the killer credit in chat :-)
						event.setDeathMessage(event.getDeathMessage() + " while trying to escape " + playerKiller.getName());
					}
				}
			}
		}

		// kill was done by another player
		else {
			playerKiller = player.getKiller();
			if (event.getEntity().getLastDamageCause().getCause() == DamageCause.FALL) {
				event.setDeathMessage(event.getDeathMessage() + " while trying to escape " + playerKiller.getName());
			}
		}
		if (resident != null) {
			// reset dead player's stats
			resident.setLastAttackerUUID(null);
			resident.setLastHitTime(0);
		}

		// we need to record the kill in all its glory to a log file for moderation use
		// takes in the time of death in milliseconds, the player that was killed, the killer, the final cause of death, and the death message
		if (!TownyWars.database.saveKill(deathTime, player, playerKiller, damageCause.name(), event.getDeathMessage())) {
			System.out.println("[TownyWars] death recording failed! you should check on this!");
		} else {
			System.out.println("[TownyWars] death recorded!");
		}

		// if the player actually wasn't killed by another player, we can stop
		if (playerKiller == null) {
			return;
		}

		TownyWarsResident killer = TownyWarsResident.getResident(playerKiller.getUniqueId());

		// if we've made it this far, it means that the death should affect TownyWars
		// now we know who to credit, so let's adjust Towny to match

		TownyWarsTown attackerTown = null;

		try {
			attackerTown = TownyWarsTown.getTown(TownyWarsResident.getResident(playerKiller.getUniqueId()).getResident().getTown());
			TownyWarsTown.getTown(TownyWarsResident.getResident(player.getUniqueId()).getResident().getTown());
		} catch (NotRegisteredException e) {
			// either the attacker or the defender is not in a town, and thus can't be in a war, so we're done here
			return;
		}

		// we want any wars that both players share
		// there could be multiple, as the various levels of orgs that the players belong to could be in separate wars
		double dploss = -1;
		if (attackerTown.getDP() < 0) {
			dploss *= Math.pow(2, 4 * (attackerTown.getDP() / (attackerTown.getMaxDP() * attackerTown.getMinDPFactor())));
		}
		for (War war : resident.getActiveWars()) {
			for (War war1 : killer.getActiveWars()) {
				if (war.equals(war1)) {
					for (Attackable org : war.getOrgs()) {
						if (resident.inOrg(org)) {
							war.addDeath(org);
							org.setDP(dploss);
							if (org.getDP() < 0 && org instanceof TownyWarsTown && !(war.getEnemy(org) instanceof TownyWarsTown)
							        && war.getWarType().equals(WarType.NORMAL)) {
								// town was conquered
								WarManager.moveTown((TownyWarsTown) org, (TownyWarsNation) war.getEnemy(org));
							}
							int currentDP = (int) org.getDP();
							int currentMaxDP = (int) org.getMaxDP();
							for (TownyWarsResident re : org.getResidents()) {
								Player plr = re.getPlayer();
								if (plr == null) {
									// player is offline!
									continue;
								}
								if (currentDP < 0) {
									plr.sendMessage(ChatColor.RED + "Danger! Your town's DP is negative! (" + currentDP + "/" + currentMaxDP + ")");
								} else {
									plr.sendMessage(ChatColor.RED + "Warning! Your town only has " + currentDP + "out of " + currentMaxDP + " DP left!");
								}
								// see if someone has won as a result of this
								if (war.checkWinner() != null) {
									war.end();
								}
								break;
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final long loginTime = System.currentTimeMillis();

		TownyWarsResident newResident = TownyWarsResident.getResident(player.getUniqueId());

		// add the player to the master list if they don't exist in it yet
		// we use player UUIDs to prevent confusion and spoofing
		if (newResident == null) {
			newResident = TownyWarsResident.putResident(player);
			if (newResident == null) {
				System.out.println("[TownyWars] error adding this player! <--- LOOK AT THIS");
				return;
			} else {
				System.out.println("new player added!");
			}
		} else {
			TownyWarsResident.updateOrgs(newResident);
		}
		newResident.setLastLoginTime(loginTime);

		// see if resident is in any orgs, return if not
		if (!newResident.inAnyOrg()) {
			return;
		}

		for (Attackable org : newResident.getOrgs()) {
			for (War war : org.getWars()) {
				if (!newResident.isInActiveWar(war)) {
					newResident.addActiveWar(war);
				}
				war.informPlayers(newResident);
				final Attackable enemy = war.getEnemy(org);
				if (org instanceof TownyWarsTown) {
					if (((TownyWarsTown) org).getTown().hasAssistant(newResident.getResident()) || newResident.getResident().isMayor()) {
						if (war.getPeaceOffer(enemy) != null) {
							player.sendMessage(ChatColor.GREEN + enemy.getName() + " has offered you peace:");
							player.sendMessage(war.getPeaceOffer(enemy));
						}
					}
				}
				if (org instanceof TownyWarsNation) {
					if (((TownyWarsNation) org).getNation().hasAssistant(newResident.getResident()) || newResident.getResident().isKing()) {
						if (war.getPeaceOffer(enemy) != null) {
							player.sendMessage(ChatColor.GREEN + enemy.getName() + " has offered you peace:");
							player.sendMessage(war.getPeaceOffer(enemy));
						}
					}
				}

			}
		}

	}

	// update player playtime stats and save the object when they quit
	@EventHandler
	public void onPlayerLeave(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		final long logoutTime = System.currentTimeMillis();
		final TownyWarsResident resident = TownyWarsResident.getResident(player.getUniqueId());
		resident.setLastLogoutTime(logoutTime);
		resident.updateTotalPlayTime();
		resident.save();
		resident.clearOrgs();
	}

}
