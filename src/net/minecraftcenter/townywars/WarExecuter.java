package net.minecraftcenter.townywars;

import java.util.List;
import java.util.Set;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
//import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.minecraftcenter.townywars.interfaces.Attackable;
import net.minecraftcenter.townywars.object.TownyWarsNation;
import net.minecraftcenter.townywars.object.TownyWarsOrg;
import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;
import net.minecraftcenter.townywars.object.War.WarStatus;
import net.minecraftcenter.townywars.object.War.WarType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

class WarExecutor implements CommandExecutor {
	private TownyWars	plugin;

	public WarExecutor(TownyWars aThis) {
		this.plugin = aThis;
	}

	public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
		boolean unknownCommand = true;
		TownyWarsResident resident = null;
		Attackable declarer=null;
		boolean admin=false;
		try {
			resident = TownyWarsResident.getResident(((Player) cs).getUniqueId());
			declarer = getHighestAuthorityOrg(resident);
		} catch (ClassCastException e) {
			// command was issued from the server terminal so it is automatically an admin
			admin=true;
		}
		
		if (strings.length == 0) {
			unknownCommand = false;
			cs.sendMessage(ChatColor.GREEN + "For help with TownyWars, type /twar help");
			return true;
		}
		String farg = strings[0];
		if (farg.equals("reload")) {
			unknownCommand = false;
			if (!cs.hasPermission("townywars.admin") || !(admin)) {
				return false;
			}
			cs.sendMessage(ChatColor.GREEN + "Reloading plugin...");
			PluginManager pm = Bukkit.getServer().getPluginManager();
			pm.disablePlugin(this.plugin);
			pm.enablePlugin(this.plugin);
			cs.sendMessage(ChatColor.GREEN + "Plugin reloaded!");
		}
		if (farg.equals("help")) {
			cs.sendMessage(ChatColor.GREEN + "Towny Wars Help:");
			cs.sendMessage(ChatColor.AQUA + "/twar" + ChatColor.YELLOW + "Displays the TownyWars configuration information.");
			cs.sendMessage(ChatColor.AQUA + "/twar help - " + ChatColor.YELLOW + "Displays the TownyWars help page.");
			cs.sendMessage(ChatColor.AQUA + "/twar status - " + ChatColor.YELLOW + "Displays a list of on-going wars.");
			cs.sendMessage(ChatColor.AQUA
			        + "/twar status <town/nation> - "
			        + ChatColor.YELLOW
			        + "Displays the town's defense points and on-going wars, or a list of the nation's towns and their defense points and the nation's on-going wars.");
			cs.sendMessage(ChatColor.AQUA + "/twar showdp - " + ChatColor.YELLOW + "Shows your town's (and nation's) current defense points.");
			cs.sendMessage(ChatColor.AQUA + "/twar view <war> - " + ChatColor.YELLOW
			        + "view the stats of the specified war (VIEWING PREPARE PHASE WAR REQUIRES YOU TO BE A MAYOR/KING/ASSISTANT)");
			if (declarer != null) {
				cs.sendMessage(ChatColor.AQUA + "/twar declare <nation> - " + ChatColor.YELLOW + "Starts a war of conquest with another nation.");
				cs.sendMessage(ChatColor.AQUA + "/twar rebellion - " + ChatColor.YELLOW + "Starts a rebellion against your parent nation.");
				cs.sendMessage(ChatColor.AQUA + "/twar raid <town/nation> - " + ChatColor.YELLOW + "Starts a raid with another town/nation.");
				cs.sendMessage(ChatColor.AQUA + "/twar end <war> - " + ChatColor.YELLOW + "Indicate your desire for peace in the specified war.");
				cs.sendMessage(ChatColor.AQUA + "/twar new <war> - " + ChatColor.YELLOW + "prepare a new stored war.");
				cs.sendMessage(ChatColor.AQUA + "/twar set type <war> <wartype> - " + ChatColor.YELLOW
				        + "set the type of the specified war: normal, rebellion, raid.");
				cs.sendMessage(ChatColor.AQUA + "/twar set enemy <war> <enemy> - " + ChatColor.YELLOW + "set the enemy town/nation in the specified war.");
				cs.sendMessage(ChatColor.AQUA + "/twar set prewar <war> <time> - " + ChatColor.YELLOW
				        + "set the length of the prewar period in the specified war (between 24 and 48 hours).");
				cs.sendMessage(ChatColor.AQUA + "/twar execute <war> - " + ChatColor.YELLOW + "execute the specified stored war.");
			}
			if (cs.hasPermission("townywars.admin")) {
				cs.sendMessage(ChatColor.AQUA + "/twar reload - " + ChatColor.YELLOW + "Reload the plugin");
				cs.sendMessage(ChatColor.AQUA + "/twar adeclare <nation> <nation> ... - " + ChatColor.YELLOW
				        + "Forces the specified nation to start a raid with the specified nation");
				cs.sendMessage(ChatColor.AQUA + "/twar arebellion <town> - " + ChatColor.YELLOW
				        + "Forces the specified town to go to war with its parent nation");
				cs.sendMessage(ChatColor.AQUA + "/twar araid <nation> <town> ... - " + ChatColor.YELLOW
				        + "Forces the specified nation to start a raid with the specified town");
				cs.sendMessage(ChatColor.AQUA + "/twar aend <war> - " + ChatColor.YELLOW + "Forces the specified war to end immediately");
				cs.sendMessage(ChatColor.AQUA + "/twar moddp <town> <value> - " + ChatColor.YELLOW + "Modifies the specified town's DP with specified value");
			}
			return true;
		}
		if (farg.equals("status")) {
			unknownCommand = false;
			if (strings.length == 1) {
				Set <War> allWars=War.getAllWars();
				if (allWars.isEmpty()) {
					cs.sendMessage(ChatColor.RED + "No wars in progress!");
				} else {
					cs.sendMessage(ChatColor.GREEN + "List of on-going wars:");
					for (War war : allWars) {
						war.informPlayers(resident);
					}
					return true;
				}
			} else if (strings.length == 2) {
				Attackable target = TownyWarsTown.getTown(strings[1]);
				if (target != null) {
					cs.sendMessage(ChatColor.YELLOW + "The town \'" + target.getName() + "\' has " + (int)target.getDP() + "/" + (int)target.getMaxDP()
					        + " defense points.");
					if (target.getWars().isEmpty()) {
						return true;
					}
					for (War war : target.getWars()) {
						war.informPlayers(resident);
					}
					return true;
				}
				target = TownyWarsNation.getNation(strings[1]);
				if (target != null) {
					cs.sendMessage(ChatColor.YELLOW + "The nation \'" + target.getName() + "\' has " + (int)target.getDP() + "/" + (int)target.getMaxDP()
					        + " defense points.");
					for (Town town : ((TownyWarsNation) target).getNation().getTowns()) {
						TownyWarsTown thisTown = TownyWarsTown.getTown(town);
						if (thisTown != null) {
							cs.sendMessage(ChatColor.YELLOW + "The town \'" + thisTown.getName() + "\' has " + (int)thisTown.getDP() + "/" + (int)thisTown.getMaxDP()
							        + " defense points.");
						}
					}
					if (target.getWars().isEmpty()) {
						return true;
					}
					for (War war : target.getWars()) {
						war.informPlayers(resident);
					}
					return true;
				}
				// TODO: add check for Coalitions here
				
				cs.sendMessage(ChatColor.RED + "The specified town/nation doesn't exist!");
				return true;
			}
			return true;
		}
		if (farg.equals("showdp") && !(admin)) {
			unknownCommand = false;
			TownyWarsTown town = null;
			TownyWarsNation nation = null;
			try {
				town = TownyWarsTown.getTown(resident.getResident().getTown());
			} catch (NotRegisteredException e) {
				cs.sendMessage(ChatColor.RED + "You are not in a Town!");
				return false;
			}
			cs.sendMessage(ChatColor.YELLOW + "Your town has " + (int)town.getDP() + "/" + (int)town.getMaxDP() + " defense points!");

			try {
				nation = TownyWarsNation.getNation(town.getTown().getNation());
			} catch (NotRegisteredException e) {
				return true;
			}

			cs.sendMessage(ChatColor.YELLOW + "Your nation has " + (int)nation.getDP() + "/" + (int)nation.getMaxDP() + " defense points!");
		}
		else if (farg.equals("showdp") && admin) {
			cs.sendMessage(ChatColor.RED+"this command is pointless from the command line!");
			return true;
		}

		if (farg.equals("new") && !(admin)) {
			unknownCommand = false;
			if (strings.length < 2) {
				cs.sendMessage(ChatColor.RED + "You must specify a name for this war!");
				return true;
			}
			if (declarer == null) {
				cs.sendMessage(ChatColor.RED + "You aren't in a town or you don't have enough power in your town or nation to do that!");
			}
			War war = new War(strings[1], declarer);
			
			declarer.addStoredWar(war);

		}
		if (farg.equals("view") && !(admin)) {
			unknownCommand = false;
			if (strings.length < 2) {
				cs.sendMessage(ChatColor.RED + "You must specify a war!");
				return true;
			}
			War war = War.getWar(strings[1]);
			if (war != null) {
				if (war.getWarStatus() != WarStatus.PREPARE) {
					war.displayFor(resident);
					return true;
				}
			}
			if (declarer == null) {
				cs.sendMessage(ChatColor.RED + "You aren't in a town or you don't have enough power in your town or nation to do that!");
				return false;
			}

			war = declarer.getStoredWar(strings[1]);
			if (war != null) {
				war.displayFor(resident);
				return true;
			}

			cs.sendMessage(ChatColor.RED + "The specified war does not exist!");
			return true;

		}
		if (farg.equals("set") && !(admin)) {
			unknownCommand = false;
			return setWarProperty(cs, strings, resident);
		}

		if (farg.equals("execute") && !(admin)) {
			unknownCommand = false;
			if (strings.length < 2) {
				cs.sendMessage(ChatColor.RED + "You must specify a war!");
				return true;
			}
			if (declarer == null) {
				cs.sendMessage(ChatColor.RED + "You aren't in a town or you don't have enough power in your town or nation to do that!");
				return false;
			}
			War war = declarer.getStoredWar(strings[1]);
			if (war == null) {
				cs.sendMessage(ChatColor.RED + "The specified stored war does not exist!");
				return true;
			}
			war.execute();
		}

		if (farg.equals("declare") && !(admin)) {
			unknownCommand = false;
			declareWar(cs, strings, resident, WarType.NORMAL, false);
		}

		if (farg.equals("adeclare")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			declareWar(cs, strings, resident, WarType.NORMAL, true);
		}
		
		if (farg.equals("rebellion") && !(admin)) {
			unknownCommand = false;
			declareRebellion(cs, strings, resident, false);
		}

		if (farg.equals("arebellion")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			declareRebellion(cs, strings, resident, true);
		}

		if (farg.equals("raid") && !(admin)) {
			unknownCommand = false;
			declareWar(cs, strings, resident, WarType.RAID, false);
		}

		if (farg.equals("araid")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			declareWar(cs, strings, resident, WarType.RAID, true);
		}
		

		if (farg.equals("end") && !(admin)) {
			unknownCommand = false;
			declareEnd(cs, strings, resident, false);
		}

		if (farg.equals("aend")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			return declareEnd(cs, strings, resident, true);
		}
		if (farg.equals("moddp") || farg.equals("amoddp")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			return modifyTownDP(cs, strings);
		}

		if (unknownCommand) {
			cs.sendMessage(ChatColor.RED + "Unknown twar command.");
		}
		return true;
	}

	private boolean modifyTownDP(CommandSender cs, String[] strings) {
		TownyWarsTown town = null;
		if (strings.length < 2) {
			cs.sendMessage(ChatColor.RED + "You need to specify a town!");
			return false;
		}
		if (strings.length < 3) {
			cs.sendMessage(ChatColor.RED + "You need to specify an amount!");
			return false;
		}

		try {
			town = TownyWarsTown.getTown(TownyUniverse.getDataSource().getTown(strings[1]));
		} catch (NotRegisteredException e) {
			cs.sendMessage(ChatColor.RED + "Town doesn't exist!");
			return false;
		}
		double addedDP = Double.parseDouble(strings[2]);
		if (Math.abs(addedDP) > 110) {
			cs.sendMessage(ChatColor.RED + "OK human, just what do you think you are doing? That value is out of range!");
			return false;
		}
		town.modifyDP(addedDP);
		cs.sendMessage(ChatColor.YELLOW + "Added " + Double.toString(addedDP) + " to " + town.getTown().getName());
		return true;
	}

	private boolean declareEnd(CommandSender cs, String[] strings, TownyWarsResident resident, boolean admin) {
		if (strings.length < 2) {
			cs.sendMessage(ChatColor.RED + "You need to specify a war!");
			return true;
		}
		if (admin) {
			if (!WarManager.endWar(strings[1])) {
				cs.sendMessage(ChatColor.RED + "The specified war does not exist or already ended!");
			}
			return true;
		}
		
		Attackable org = getHighestAuthorityOrg(resident);
		if (org == null) {
			cs.sendMessage(ChatColor.RED + "You aren't in a town or you don't have enough power in your town or nation to do that!");
			return false;
		}
		
		War war=War.getWar(strings[1]);
		if (war==null) {
			cs.sendMessage(ChatColor.RED + "The specified war does not exist or already ended!");
			return true;
		}
		
		if (!war.hasMember(org)) {
			cs.sendMessage(ChatColor.RED + "You are not in the specified war");
			return true;
		}
		WarManager.requestPeace(war, org);
		cs.sendMessage(ChatColor.GREEN + "Requested peace!");
		return true;
	}

	// a quick way to start a normal war or raid: the prepare phase is skipped and the war is started immediately
	private boolean declareWar(CommandSender cs, String[] strings, TownyWarsResident resident, War.WarType type, boolean admin) {
		Attackable declarer = null;
		Attackable target = null;
		if ((strings.length < 3) && (admin)) {
			cs.sendMessage(ChatColor.RED + "You need to specify two combatants!");
			return false;
		}
		if (strings.length < 2) {
			cs.sendMessage(ChatColor.RED + "You need to specify a target!");
			return false;
		}
		if (admin) {
			List<Attackable> possibleDeclarers = TownyWarsOrg.getOrg(strings[1]);
			if (possibleDeclarers == null) {
				cs.sendMessage(ChatColor.RED + "The specified town/nation doesn't exist");
				return true;
			}
			declarer = possibleDeclarers.get(0);
		} else {
			declarer = getHighestAuthorityOrg(resident);
		}

		if (declarer == null) {
			cs.sendMessage(ChatColor.RED + "You aren't in a town or you don't have enough power in your town or nation to do that!");
			return false;
		}
		if (admin) {
			target = findEnemy(declarer, strings[2]);
		} else {
			target = findEnemy(declarer, strings[1]);
		}

		if (type == War.WarType.RAID) {
			if (declarer instanceof TownyWarsTown && !(target instanceof TownyWarsTown)) {
				cs.sendMessage(ChatColor.RED + "Your town can only raid another town!");
			}

			// TODO: add check for Coalitions here

			if (declarer instanceof TownyWarsTown && target instanceof TownyWarsTown) {
				Attackable declarerParent = null;
				Attackable targetParent = null;
				try {
					declarerParent = TownyWarsNation.getNation(((TownyWarsTown) declarer).getTown().getNation());
					targetParent = TownyWarsNation.getNation(((TownyWarsTown) target).getTown().getNation());
					if (declarerParent.equals(targetParent)) {
						cs.sendMessage(ChatColor.RED + "You can't raid another town in your nation!");
					}
				} catch (NotRegisteredException e) {
					// nothing to do here
				}
			}
			if (!WarManager.quickRaid(declarer, target)) {
				cs.sendMessage(ChatColor.RED + "Raid creation failed; are the specified nations already at war with each other?");
				return true;
			}
		}

		if (type == WarType.NORMAL) {

			// TODO: add check for Coalitions here

			if (!(declarer instanceof TownyWarsNation)) {
				cs.sendMessage(ChatColor.RED + "You must be at least a nation to start a war of conquest!");
			}
			if (!(target instanceof TownyWarsNation)) {
				cs.sendMessage(ChatColor.RED + "You can only start a war of conquest with at least another nation");
			}
			if (!WarManager.quickWar(declarer, target)) {
				cs.sendMessage(ChatColor.RED + "War creation failed; are the specified nations already at war with each other?");
				return true;
			}
		}
		return true;
	}

	// a quick way to start a rebellion: the prepare phase is skipped and the rebellion is started immediately
	// this is particularly quick to set up because we know there will only be two participants and the other participant is the parent nation
	private boolean declareRebellion(CommandSender cs, String[] strings, TownyWarsResident resident, boolean admin) {
		Attackable rebelTown = null;
		Attackable parent = null;
		if ((strings.length < 2)) {
			cs.sendMessage(ChatColor.RED + "You need to specify a name for the new nation!");
			return true;
		}
		if ((strings.length < 3) && (admin)) {
			cs.sendMessage(ChatColor.RED + "You need to specify a name for the new nation and a town!");
			return true;
		}
		if (admin) {
			List<Attackable> possibleDeclarers = TownyWarsOrg.getOrg(strings[2]);
			if (possibleDeclarers == null) {
				cs.sendMessage(ChatColor.RED + "The specified town/nation doesn't exist");
				return true;
			}
			for (Attackable declarer : possibleDeclarers) {
				if (declarer instanceof TownyWarsTown) {
					rebelTown = declarer;
				}
			}
			if (rebelTown == null) {
				cs.sendMessage(ChatColor.RED + "Specified town does not exist!");
				return true;
			}
			try {
				parent = TownyWarsNation.getNation(((TownyWarsTown) rebelTown).getTown().getNation());
			} catch (NotRegisteredException e) {
				cs.sendMessage(ChatColor.RED + "Specified town is not in a nation!");
				return true;
			}

		} else {
			try {
				rebelTown = TownyWarsTown.getTown(resident.getResident().getTown());
			} catch (Exception ex) {
				cs.sendMessage(ChatColor.RED + "You are not in a town!");
				return true;
			}
			if (!resident.getResident().isMayor() && !((TownyWarsTown) rebelTown).getTown().hasAssistant(resident.getResident())) {
				cs.sendMessage("You don't have enough power in your town to do that!");
				return true;
			}
			try {
				parent = TownyWarsNation.getNation(((TownyWarsTown) rebelTown).getTown().getNation());
			} catch (NotRegisteredException e) {
				cs.sendMessage(ChatColor.RED + "Your town is not in a nation!");
				return true;
			}
		}

		System.out.println(parent);

		// now create the quick rebellion
		if (!WarManager.quickRebellion(strings[1], rebelTown, parent)) {
			cs.sendMessage(ChatColor.RED + "The parent nation is already in a rebellion or only one town is in the parent nation!");
		}
		return true;
	}

	private static Attackable getHighestAuthorityOrg(TownyWarsResident resident) {
		Attackable org = null;
		TownyWarsTown town = null;
		if (!resident.getResident().hasTown()) {
			return null;
		}
		try {
			town = TownyWarsTown.getTown(resident.getResident().getTown());
		} catch (Exception ex) {
			return null;
		}
		if (!resident.getResident().isMayor() && !town.getTown().hasAssistant(resident.getResident())) {
			return null;
		}
		org = town;
		TownyWarsNation nation = null;
		if (resident.getResident().hasNation()) {
			try {
				nation = TownyWarsNation.getNation(town.getTown().getNation());
			} catch (Exception ex) {
				return null;
			}
			if (resident.getResident().isKing() || nation.getNation().hasAssistant(resident.getResident())) {
				org = nation;
			}
		}

		// TODO: add in check for Coalitions here

		return org;
	}

	private static boolean setWarProperty(CommandSender cs, String strings[], TownyWarsResident resident) {
		if (strings.length < 2) {
			cs.sendMessage(ChatColor.RED + "You must specify a property to set!");
			return true;
		}
		if (strings.length < 3) {
			cs.sendMessage(ChatColor.RED + "You must specify a war!");
			return true;
		}
		if (strings.length < 4) {
			cs.sendMessage(ChatColor.RED + "You must specify a value for the property!");
			return true;
		}
		Attackable declarer = getHighestAuthorityOrg(resident);
		if (declarer == null) {
			cs.sendMessage(ChatColor.RED + "You aren't in a town or you don't have enough power in your town or nation to do that!");
			return false;
		}
		War war = declarer.getStoredWar(strings[2]);
		if (war == null) {
			cs.sendMessage(ChatColor.RED + "The specified war does not exist!");
			return true;
		}

		if (strings[1].equals("type")) {
			War.WarType newType = War.WarType.valueOf(strings[3].toUpperCase());
			if (newType == null) {
				cs.sendMessage(ChatColor.RED + "You must specify a valid war type (NORMAL, RAID, REBELLION)!");
				return true;
			}
			war.setWarType(newType);
			war.save();
			return true;
		}
		if (strings[1].equals("enemy")) {
			String enemyName = strings[3];
			Attackable enemy = findEnemy(declarer, enemyName);
			if (enemy == null) {
				cs.sendMessage(ChatColor.RED + "The specified town/nation does not exist or you are allies with the specified nation!");
				return true;
			}
			war.setTarget(enemy);
			war.save();
			return true;
		}
		if (strings[1].equals("prewar")) {
			Integer newPrewar = Integer.parseInt(strings[3]);
			if (newPrewar == null) {
				cs.sendMessage(ChatColor.RED + "Invalid time specified!");
				return true;
			}
			if (newPrewar < 24 || newPrewar > 48) {
				cs.sendMessage(ChatColor.RED + "You must specify a time between 24 and 48 hours");
				return true;
			}
			war.setPrewarTime(newPrewar * 3600 * 1000);
			war.save();
			return true;
		}
		return true;
	}

	private static Attackable findEnemy(Attackable declarer, String enemyName) {
		Attackable enemy = TownyWarsTown.getTown(enemyName);
		if (enemy == null) {
			enemy = TownyWarsNation.getNation(enemyName);
			if (enemy == null) {

				// TODO: add in check for Coalitions here

				return null;
			}
			if (declarer instanceof TownyWarsNation && enemy instanceof TownyWarsNation) {
				if (((TownyWarsNation) declarer).getNation().hasAlly(((TownyWarsNation) enemy).getNation())) {
					return null;
				}
			}
		}
		return enemy;
	}

}
