package net.minecraftcenter.townywars;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.minecraftcenter.townywars.object.TownyWarsNation;
import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;

class WarExecutor implements CommandExecutor {
	private TownyWars	plugin;

	public WarExecutor(TownyWars aThis) {
		this.plugin = aThis;
	}

	public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
		boolean unknownCommand = true;
		if (strings.length == 0) {
			unknownCommand = false;
			cs.sendMessage(ChatColor.GREEN + "For help with TownyWars, type /twar help");
			return true;
		}
		String farg = strings[0];
		if (farg.equals("reload")) {
			unknownCommand = false;
			if (!cs.hasPermission("townywars.admin")) {
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
			cs.sendMessage(ChatColor.AQUA + "/twar" + ChatColor.YELLOW + "Displays the TownyWars configuration information");
			cs.sendMessage(ChatColor.AQUA + "/twar help - " + ChatColor.YELLOW + "Displays the TownyWars help page");
			cs.sendMessage(ChatColor.AQUA + "/twar status - " + ChatColor.YELLOW + "Displays a list of on-going wars");
			cs.sendMessage(ChatColor.AQUA + "/twar status <nation> - " + ChatColor.YELLOW + "Displays a list of the nation's towns and their defense points");
			cs.sendMessage(ChatColor.AQUA + "/twar showdp - " + ChatColor.YELLOW + "Shows your towns current defense points.");
			cs.sendMessage(ChatColor.AQUA + "/twar rebellion - " + ChatColor.YELLOW
			        + "Starts a rebellion with your parent nation. (REQUIRES YOU TO BE A MAYOR/ASSISTANT)");
			cs.sendMessage(ChatColor.AQUA + "/twar flagwar <town> - " + ChatColor.YELLOW
			        + "Starts a flagwar with another nation's town (REQUIRES YOU TO BE A KING/ASSISTANT)");
			cs.sendMessage(ChatColor.AQUA + "/twar end <war> - " + ChatColor.YELLOW
			        + "Indicate your desire for peace in the specified war (REQUIRES YOU TO BE A KING/ASSISTANT)");
			if (cs.hasPermission("townywars.admin")) {
				cs.sendMessage(ChatColor.AQUA + "/twar reload - " + ChatColor.YELLOW + "Reload the plugin");
				cs.sendMessage(ChatColor.AQUA + "/twar arebellion <town> - " + ChatColor.YELLOW
				        + "Forces the specified town to go to war with its parent nation");
				cs.sendMessage(ChatColor.AQUA + "/twar aflagwar <nation> <town> ... - " + ChatColor.YELLOW
				        + "Forces the specified nation to start a flag war with the specified town");
				cs.sendMessage(ChatColor.AQUA + "/twar aend <war> - " + ChatColor.YELLOW + "Forces the specified war to end immediately");
				cs.sendMessage(ChatColor.AQUA + "/twar moddp <town> <value> - " + ChatColor.YELLOW + "Modifies the specified town's DP with specified value");
			}
			return true;
		}
		if (farg.equals("status")) {
			unknownCommand = false;
			TownyWarsResident resident;
			try {
				resident = TownyWarsResident.getResident(TownyUniverse.getDataSource().getResident(cs.getName()));
			} catch (NotRegisteredException e) {
				cs.sendMessage(ChatColor.RED + "You are not registered in Towny!");
				e.printStackTrace();
				return false;
			}
			if (strings.length == 1) {
				if (WarManager.getWars().isEmpty()) {
					cs.sendMessage(ChatColor.RED + "No wars in progress!");
				} else {
					cs.sendMessage(ChatColor.GREEN + "List of on-going wars:");
					for (War war : WarManager.getWars()) {
						war.informPlayers(resident);
					}
				}
			} else if (strings.length == 2) {
				TownyWarsNation nation = null;
				try {
					nation = TownyWarsNation.getNation(TownyUniverse.getDataSource().getNation(strings[1]));
				} catch (NotRegisteredException e) {
					cs.sendMessage(ChatColor.RED + "\"" + strings[1] + "\" is not a valid nation!");
				}
				if (nation.getWars().isEmpty()) {
					cs.sendMessage(ChatColor.RED + nation.getName() + " is not in any wars right now!");
				}
				for (War war : nation.getWars()) {
					war.informPlayers(resident);
				}
			}
		}
		if (farg.equals("showdp")) {
			unknownCommand = false;
			TownyWarsTown town = null;
			try {
				town = TownyWarsTown.getTown(TownyUniverse.getDataSource().getResident(cs.getName()).getTown());
			} catch (NotRegisteredException e) {
				cs.sendMessage(ChatColor.RED + "You are not in a Town!");
				return false;
			}
			Double points = town.getDP();

			cs.sendMessage(ChatColor.YELLOW + "Your town's defense value is currently " + points.floatValue() + " which results in " + points.intValue()
			        + " defense points!");
		}

		if (farg.equals("rebellion")) {
			unknownCommand = false;
			declareRebellion(cs, strings, false);
		}

		if (farg.equals("arebellion")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			declareRebellion(cs, strings, true);
		}

		if (farg.equals("flagwar")) {
			unknownCommand = false;
			declareFlagWar(cs, strings, false);
		}

		if (farg.equals("aflagwar")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			declareFlagWar(cs, strings, true);
		}

		if (farg.equals("end")) {
			unknownCommand = false;
			declareEnd(cs, strings, false);
		}

		if (farg.equals("aend")) {
			unknownCommand = false;
			if (!cs.hasPermission("warexecutor.admin")) {
				cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
				return false;
			}
			return declareEnd(cs, strings, true);
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

	private boolean declareEnd(CommandSender cs, String[] strings, boolean admin) {
		if (strings.length < 2) {
			cs.sendMessage(ChatColor.RED + "You need to specify a war!");
		}
		if (admin) {
			if (!WarManager.endWar(strings[1])) {
				cs.sendMessage(ChatColor.RED + "The specified war does not exist or already ended!");
			}
			return true;
		}
		Resident resident = null;
		TownyWarsNation nation = null;
		try {
			resident = TownyUniverse.getDataSource().getResident(cs.getName());
			nation = TownyWarsNation.getNation(resident.getTown().getNation());
		} catch (Exception ex) {
			cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation!");
			return false;
		}

		if (!resident.isKing() && !nation.getNation().hasAssistant(resident)) {
			cs.sendMessage(ChatColor.RED + "You are not powerful enough in your nation to do that!");
			return false;
		}

		// find the war that is to be ended
		War currentWar = null;
		for (War war : nation.getWars()) {
			if (war.getName().compareTo(strings[1]) == 0) {
				currentWar = war;
				break;
			}
		}
		if (currentWar == null) {
			cs.sendMessage(ChatColor.RED + "You are not in the specified war");
		} else {
			WarManager.requestPeace(currentWar, nation);
			cs.sendMessage(ChatColor.GREEN + "Requested peace!");
		}
		return true;
	}

	// a quick way to start a flag war: the prepare phase is skipped and the flag war is started immediately
	private boolean declareFlagWar(CommandSender cs, String[] strings, boolean admin) {
		TownyWarsNation declarer = null;
		TownyWarsTown target = null;
		TownyWarsNation targetParent = null;
		Resident resident = null;
		if ((strings.length < 3) && (admin)) {
			cs.sendMessage(ChatColor.RED + "You need to specify two combatants!");
			return false;
		}
		if (strings.length < 2) {
			cs.sendMessage(ChatColor.RED + "You need to specify a target!");
			return false;
		}
		try {
			if (admin) {
				target = TownyWarsTown.getTown(TownyUniverse.getDataSource().getTown(strings[2]));
				targetParent = TownyWarsNation.getNation(target.getTown().getNation());
				declarer = TownyWarsNation.getNation(TownyUniverse.getDataSource().getNation(strings[1]));
			} else {
				resident = TownyUniverse.getDataSource().getResident(cs.getName());
				target = TownyWarsTown.getTown(TownyUniverse.getDataSource().getTown(strings[1]));
				targetParent = TownyWarsNation.getNation(target.getTown().getNation());
				declarer = TownyWarsNation.getNation(resident.getTown().getNation());
			}
		} catch (Exception ex) {
			cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation, or your target is not part of a nation!");
			return false;
		}
		if ((!admin) && (!declarer.getNation().isKing(resident)) && (!declarer.getNation().hasAssistant(resident))) {
			cs.sendMessage(ChatColor.RED + "You are not powerful enough in your nation to do that!");
			return false;
		}

		// now create the quick flag war
		if (!WarManager.quickFlagWar(declarer, target, targetParent)) {
			cs.sendMessage(ChatColor.RED + "Flag war creation failed; are the specified nations already at war with each other?");
		}
		return true;
	}

	// a quick way to start a rebellion: the prepare phase is skipped and the rebellion is started immediately
	// this is particularly quick to set up because we know there will only be two participants and the other participant is the parent nation
	private boolean declareRebellion(CommandSender cs, String[] strings, boolean admin) {
		TownyWarsTown rebelTown = null;
		TownyWarsNation parent = null;
		Resident resident = null;
		if ((strings.length < 3) && (admin)) {
			cs.sendMessage(ChatColor.RED + "You need to specify a rebellion name and a town!");
			return false;
		}
		if (strings.length == 1) {
			cs.sendMessage(ChatColor.RED + "You need to specify a name!");
			return false;
		}
		try {
			if (admin) {
				rebelTown = TownyWarsTown.getTown(TownyUniverse.getDataSource().getTown(strings[2]));
				parent = TownyWarsNation.getNation(rebelTown.getTown().getNation());
			} else {
				resident = TownyUniverse.getDataSource().getResident(cs.getName());
				rebelTown = TownyWarsTown.getTown(resident.getTown());
				parent = TownyWarsNation.getNation(resident.getTown().getNation());
			}
		} catch (Exception ex) {
			cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation!");
			return false;
		}
		if ((!admin) && (!rebelTown.getTown().isMayor(resident)) && (!rebelTown.getTown().hasAssistant(resident))) {
			cs.sendMessage(ChatColor.RED + "You are not powerful enough in your town to do that!");
			return false;
		}

		System.out.println(parent);

		// now create the quick rebellion
		if (!WarManager.quickRebellion(strings[1], rebelTown, parent)) {
			cs.sendMessage(ChatColor.RED + "The parent nation is already in a rebellion or only one town is in the parent nation!");
		}
		return true;
	}
}
