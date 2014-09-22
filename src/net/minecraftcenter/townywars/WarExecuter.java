package net.minecraftcenter.townywars;

//import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
//import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
//import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.ArrayList;
import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;

//import net.minecraftcenter.townywars.WarManager.WarStatus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;


class WarExecutor implements CommandExecutor
{
  private TownyWars plugin;
  //private static String errorPrefix="[TownyWars] ";
 
   public WarExecutor(TownyWars aThis)
  {
    this.plugin = aThis;
  }
  
  public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings)
  {
	boolean unknownCommand=true;
    if (strings.length == 0)
    {
    	unknownCommand=false;
      cs.sendMessage(ChatColor.GREEN + "For help with TownyWars, type /twar help");
      return true;
    }
    String farg = strings[0];
    if (farg.equals("reload"))
    {
    	unknownCommand=false;
      if (!cs.hasPermission("townywars.admin")) {
        return false;
      }
      cs.sendMessage(ChatColor.GREEN + "Reloading plugin...");
      PluginManager pm = Bukkit.getServer().getPluginManager();
      pm.disablePlugin(this.plugin);
      pm.enablePlugin(this.plugin);
      cs.sendMessage(ChatColor.GREEN + "Plugin reloaded!");
    }
    if (farg.equals("help"))
    {
      cs.sendMessage(ChatColor.GREEN + "Towny Wars Help:");
      cs.sendMessage(ChatColor.AQUA + "/twar" + ChatColor.YELLOW + "Displays the TownyWars configuration information");
      cs.sendMessage(ChatColor.AQUA + "/twar help - " + ChatColor.YELLOW + "Displays the TownyWars help page");
      cs.sendMessage(ChatColor.AQUA + "/twar status - " + ChatColor.YELLOW + "Displays a list of on-going wars");
      cs.sendMessage(ChatColor.AQUA + "/twar status [nation] - " + ChatColor.YELLOW + "Displays a list of the nation's towns and their defense points");
      cs.sendMessage(ChatColor.AQUA + "/twar showtowndp - " + ChatColor.YELLOW + "Shows your towns current defense points.");
      cs.sendMessage(ChatColor.AQUA + "/twar declare [nation1] [nation2] ... - " + ChatColor.YELLOW + "Starts a normal war with one or more nations (REQUIRES YOU TO BE A KING/ASSISTANT)");
      cs.sendMessage(ChatColor.AQUA + "/twar end [war] - " + ChatColor.YELLOW + "Indicate your desire for peace in the specified war (REQUIRES YOU TO BE A KING/ASSISTANT)");
      cs.sendMessage(ChatColor.AQUA + "/twar rebellion - " + ChatColor.YELLOW + "Starts a rebellion with your parent nation. (REQUIRES YOU TO BE A MAYOR/ASSISTANT)");
      cs.sendMessage(ChatColor.AQUA + "/twar flagwar [town]- " + ChatColor.YELLOW + "Starts a flagwar with another nation's town (REQUIRES YOU TO BE A KING/ASSISTANT)");
      //cs.sendMessage(ChatColor.AQUA + "/twar createrebellion [name] - " + ChatColor.YELLOW + "Creates a (secret) rebellion within your nation.");
      //cs.sendMessage(ChatColor.AQUA + "/twar joinrebellion [name] - " + ChatColor.YELLOW + "Joins a rebellion within your nation using the name.");
      //cs.sendMessage(ChatColor.AQUA + "/twar leaverebellion - " + ChatColor.YELLOW + "Leaves your current rebellion.");
      //cs.sendMessage(ChatColor.AQUA + "/twar showrebellion - " + ChatColor.YELLOW + "Shows your current rebellion and its members.");
      //cs.sendMessage(ChatColor.AQUA + "/twar executerebellion - " + ChatColor.YELLOW + "Executes your rebellion and you go to war with your nation (requires to be leader of rebellion).");
      if (cs.hasPermission("townywars.admin"))
      {
        cs.sendMessage(ChatColor.AQUA + "/twar reload - " + ChatColor.YELLOW + "Reload the plugin");
        cs.sendMessage(ChatColor.AQUA + "/twar adeclare [nation1] [nation2] ... - " + ChatColor.YELLOW + "Forces two or more nations to go to war");
        cs.sendMessage(ChatColor.AQUA + "/twar arebellion [town] ... - " + ChatColor.YELLOW + "Forces the specified town to go to war with its parent nation");
        cs.sendMessage(ChatColor.AQUA + "/twar aflagwar [nation] [town] ... - " + ChatColor.YELLOW + "Forces the specified nation to start a flag war with the specified town");
        cs.sendMessage(ChatColor.AQUA + "/twar aend [war] [nation] - " + ChatColor.YELLOW + "Forcibly removes the specified nation from the specified war");
        cs.sendMessage(ChatColor.AQUA + "/twar aend [war] - " + ChatColor.YELLOW + "Forces the specified war to stop");
        cs.sendMessage(ChatColor.AQUA + "/twar aaddtowndp [town] - " + ChatColor.YELLOW + "Adds a DP to the town");
        cs.sendMessage(ChatColor.AQUA + "/twar aremovetowndp [town] - " + ChatColor.YELLOW + "Removes a DP from the town");
      }
      return true;
    }
    //War war=null;
    if (farg.equals("status"))
    {
    	unknownCommand=false;
    	TownyWarsResident resident;
		try {
			resident = TownyWars.residentToTownyWarsResidentHash.get(TownyUniverse.getDataSource().getResident(cs.getName()));
		} catch (NotRegisteredException e) {
			cs.sendMessage(ChatColor.RED+"You are not registered in Towny!");
			e.printStackTrace();
			return false;
		}
      if (strings.length == 1)
      {
        cs.sendMessage(ChatColor.GREEN + "List of on-going wars:");
        for (War war : WarManager.getWars())
        {
        	war.informPlayers(resident);
        }
        return true;
      }
      else {
    	  TownyWarsNation nation=null;
		try {
			nation = TownyWars.nationToTownyWarsNationHash.get(TownyUniverse.getDataSource().getNation(strings[1]));
		} catch (NotRegisteredException e) {
			cs.sendMessage(ChatColor.RED+strings[1]+" is not a valid nation!");
			return false;
		}
    	  for (War war : nation.getWars()) {
    		  war.informPlayers(resident);
    	  }
    	  
      }
    }
    if (farg.equals("showtowndp")){
    	unknownCommand=false;
    	TownyWarsTown town = null;
    	try {
			town = TownyWars.townToTownyWarsTownHash.get(TownyUniverse.getDataSource().getResident(cs.getName()).getTown());
		} catch (NotRegisteredException e) {
			cs.sendMessage(ChatColor.RED + "You are not in a Town!");
			return true;
		}
    	Double points = town.getDP();
    	
    	cs.sendMessage(ChatColor.YELLOW + "Your town's defense value is currently " +  points.floatValue() + " which results in " + points.intValue() + " defense points!");
    	return true;
    }
    /*if (farg.equals("neutral"))
    {
    	unknownCommand=false;
      if (!cs.hasPermission("townywars.neutral"))
      {
        cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
        return true;
      }
      Nation csNation;
      try
      {
        Town csTown = TownyUniverse.getDataSource().getResident(cs.getName()).getTown();
        csNation = TownyUniverse.getDataSource().getTown(csTown.toString()).getNation();
      }
      catch (NotRegisteredException ex)
      {
        cs.sendMessage(ChatColor.RED + "You are not not part of a town, or your town is not part of a nation!");
        Logger.getLogger(WarExecutor.class.getName()).log(Level.SEVERE, null, ex);
        return true;
      }
      if ((!cs.isOp()) && (!csNation.toString().equals(strings[1])))
      {
        cs.sendMessage(ChatColor.RED + "You may only set your own nation to neutral, not others.");
        return true;
      }
      if (strings.length == 0) {
        cs.sendMessage(ChatColor.RED + "You must specify a nation to toggle neutrality for (eg. /twar neutral [nation]");
      }
      if (strings.length == 1)
      {
        String onation = strings[1];
        Nation t;
        try
        {
          t = TownyUniverse.getDataSource().getNation(onation);
        }
        catch (NotRegisteredException ex)
        {
          cs.sendMessage(ChatColor.GOLD + "The nation called " + onation + " could be found!");
          return true;
        }
        War.MutableInteger mi = new War.MutableInteger(0);
        WarManager.neutral.put(t.toString(), mi);
      }
    }*/
    if (farg.equals("adeclare"))
    {
    	unknownCommand=false;
      if (!cs.hasPermission("townywars.admin"))
      {
        cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
        return false;
      }
      return declareWar(cs, strings, true);
    }
    if (farg.equals("declare")) {
    	unknownCommand=false;
      return declareWar(cs, strings, false);
    }
    if (farg.equals("end")) {
    	unknownCommand=false;
      return declareEnd(cs, strings, false);
    }
    if (farg.equals("rebellion")) {
    	unknownCommand=false;
    	return declareRebellion(cs,strings,false);
    }
    if (farg.equals("flagwar")) {
    	unknownCommand=false;
    	return declareFlagWar(cs,strings,false);
    }
    if (farg.equals("aflagwar")) {
    	unknownCommand=false;
    	if (!cs.hasPermission("warexecutor.admin"))
        {
          cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
          return false;
        }
    	return declareFlagWar(cs,strings,true);
    }
    if (farg.equals("arebellion")) {
    	unknownCommand=false;
    	if (!cs.hasPermission("warexecutor.admin"))
        {
          cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
          return false;
        }
    	return declareRebellion(cs,strings,true);
    }
    
   /* if (farg.equals("createrebellion")) {
    	unknownCommand=false;
        return createRebellion(cs,strings, false);
      }
    if (farg.equals("joinrebellion")) {
    	unknownCommand=false;
    	return joinRebellion(cs,strings, false);
    }
    if (farg.equals("leaverebellion")) {
    	unknownCommand=false;
    	return leaveRebellion(cs, strings, false);
    }
    if(farg.equals("executerebellion")) {
    	unknownCommand=false;
      return executeRebellion(cs, strings, false);
    }
    if(farg.equals("showrebellion")) {
    	unknownCommand=false;
    	return showRebellion(cs, strings, false);
    }*/
    if (farg.equals("aend"))
    {
    	unknownCommand=false;
      if (!cs.hasPermission("warexecutor.admin"))
      {
        cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
        return false;
      }
      return declareEnd(cs, strings, true);
    }
    if(farg.equals("aaddtowndp")){
    	unknownCommand=false;
    	if (!cs.hasPermission("warexecutor.admin"))
        {
          cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
          return true;
        }
        return modifyTownDP(cs, strings,1);
    }
    if(farg.equals("aremovetowndp")){
    	unknownCommand=false;
    	if (!cs.hasPermission("warexecutor.admin"))
        {
          cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
          return true;
        }
        return modifyTownDP(cs, strings,-1);
    }
    /*try {
    	Resident targetResident = TownyUniverse.getDataSource().getResident(farg);
    	Town targetTown = null;
    	Nation targetNation = null;
    	try {
    		targetTown = targetResident.getTown();
    		try {
        		targetNation = targetResident.getTown().getNation();
        	}
        	catch (NotRegisteredException ex) { }
    	}
    	catch (NotRegisteredException ex) { }
    	String townName = "none";
    	String nationName = "none";
    	if (targetTown!=null) {
    		townName=targetTown.getName();
    	}
    	if (targetNation!=null) {
    		nationName=targetNation.getName();
    	}
    	long lastOnline = targetResident.getLastOnline();
    	long currentTime = System.currentTimeMillis();
    	String onlineState = "offline";
    	if (currentTime-lastOnline<1000) { onlineState = "online"; }
    	cs.sendMessage(ChatColor.GREEN + targetResident.getName()+" ("+onlineState+")");
    	cs.sendMessage(ChatColor.GREEN + "--------------------");
    	if (townName.compareTo("none")==0) {
    		cs.sendMessage("Town: "+ChatColor.GRAY+townName);
    	}
    	else {
    		cs.sendMessage("Town: "+ChatColor.GREEN+townName);
    	}
    	if (nationName.compareTo("none")==0) {
    		cs.sendMessage("Nation: "+ChatColor.GRAY+nationName);
    	}
    	else {
    		cs.sendMessage("Nation: "+ChatColor.GREEN+nationName);
    	}
    	cs.sendMessage(ChatColor.GREEN + "--------------------");
    	unknownCommand=false;
    }

	catch (NotRegisteredException ex)
    {
		cs.sendMessage(ChatColor.RED + farg + " does not exist!");
    }*/
    if (unknownCommand) {
    	cs.sendMessage(ChatColor.RED + "Unknown twar command.");
    }
    return true;
  }
  
  private boolean modifyTownDP(CommandSender cs, String[] strings,double addedDP) {
	TownyWarsTown town = null;
	if(strings.length != 2){
		cs.sendMessage(ChatColor.RED + "You need to specify a town!");
		return false;
	}
	
	try {
		town = TownyWars.townToTownyWarsTownHash.get(TownyUniverse.getDataSource().getTown(strings[1]));
	} catch (NotRegisteredException e) {
		cs.sendMessage(ChatColor.RED + "Town doesn't exist!");
		return false;
	}
	town.modifyDP(addedDP);
	cs.sendMessage(ChatColor.YELLOW + "Added "+Double.toString(addedDP)+" to " + town.getTown().getName());
	return true;
  }
  
  /*private boolean removeTownDp(CommandSender cs, String[] strings) {
		Town town = null;
		if(strings.length != 2){
			cs.sendMessage(ChatColor.RED + "You need to specify a town!");
			return false;
		}
		
		try {
			town = TownyUniverse.getDataSource().getTown(strings[1]);
		} catch (NotRegisteredException e) {
			cs.sendMessage(ChatColor.RED + "Town doesn't exist!");
			return false;
		}
			
		for(War war : WarManager.getWars())
			for(Nation nation : war.getNationsInWar())
				if(nation.hasTown(strings[1])){
					try {
						war.chargeTownPoints(town.getNation(), town, 1);
						cs.sendMessage(ChatColor.YELLOW + "Removed a DP from " + town.getName());
					} catch (NotRegisteredException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return true;
				}
		return false;
	}*/

/*private boolean showRebellion(CommandSender cs, String[] strings, boolean admin) {
	  
	  Resident res = null;
	  try {
		res = TownyUniverse.getDataSource().getResident(cs.getName());
	  } catch (NotRegisteredException e3) {
		// TODO Auto-generated catch block
		e3.printStackTrace();
		}
	  
	  try {
			if ((!admin) && (!res.getTown().isMayor(res)))
			  {
			      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your town to do that!");
			      return true;
			  }
		} catch (NotRegisteredException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	  
	  for(Rebellion r : Rebellion.getAllRebellions()){
			try {
				if(r.isRebelTown(res.getTown()) || r.isRebelLeader(res.getTown())){
						cs.sendMessage(ChatColor.YELLOW + ".oOo.___________.[ " + r.getName() + " (Rebellion) ].___________.oOo.");
						cs.sendMessage(ChatColor.GREEN + "Nation: " + r.getMotherNation().getName());
						cs.sendMessage(ChatColor.GREEN + "Leader: " + r.getLeader().getName());
						String members = new String("");
						for(Town town : r.getRebels())
							members = members + ", " + town.getName();
						if(!members.isEmpty())
							members = members.substring(1);
						cs.sendMessage(ChatColor.GREEN + "Members: " + members);
						return true;
				}
			} catch (NotRegisteredException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  }
	  
	  cs.sendMessage(ChatColor.RED + "You are not in a rebellion!");
	  return true;
  }

//Author: Noxer
  private boolean createRebellion(CommandSender cs, String[] strings, boolean admin){
	  
	  Resident res = null;
	try {
		res = TownyUniverse.getDataSource().getResident(cs.getName());
	} catch (NotRegisteredException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
	  
	  if(strings.length != 2){
	  	cs.sendMessage(ChatColor.RED + "You need to give your rebellion a name!");
	  	return true;
	  }
	  
	  try {
		if((!admin) && (!res.getTown().hasNation())){
			cs.sendMessage(ChatColor.RED + "You are not in a nation!");
			return true;
		  }
	} catch (NotRegisteredException e3) {
		// TODO Auto-generated catch block
		e3.printStackTrace();
	}
	  
	  try {
		if ((!admin) && (!res.getTown().isMayor(res)))
		  {
			  cs.sendMessage(ChatColor.RED + "You are not powerful enough in your town to do that!");
			  return true;
		  }
	} catch (NotRegisteredException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
	  
	  try {
		if (res.getTown().getNation().getCapital() == res.getTown())
		  {
			  cs.sendMessage(ChatColor.RED + "You cannot create a rebellion (towards yourself) when you are the capital!");
			  return true;
		  }
	} catch (NotRegisteredException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	  
	  for(Rebellion r : Rebellion.getAllRebellions()){
		  try {
			if(r.isRebelTown(res.getTown()) || r.isRebelLeader(res.getTown())){
			  		cs.sendMessage(ChatColor.RED + "You are already in a rebellion!");
			      	return true;
			  }
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	  for(Rebellion r : Rebellion.getAllRebellions())
			if(r.getName() == strings[1]){
				  cs.sendMessage(ChatColor.RED + "Rebellion with that name already exists!");
				  return true;
			  }
	  if(strings[1].length() > 13){
		  cs.sendMessage(ChatColor.RED + "Rebellion name too long (max 13)!");
		  return true;
	  }
	  try {
		new Rebellion(res.getTown().getNation(), strings[1], res.getTown());
		cs.sendMessage(ChatColor.YELLOW + "You created the rebellion " + strings[1] + " in your nation!");
	} catch (NotRegisteredException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  return true;
  }
  
  //Author: Noxer
private boolean joinRebellion(CommandSender cs, String[] strings, boolean admin)
  {
	  Resident res = null;
	  try {
		res = TownyUniverse.getDataSource().getResident(cs.getName());
	} catch (NotRegisteredException e3) {
		// TODO Auto-generated catch block
		e3.printStackTrace();
	}
	  
	  if(strings.length != 2){
		  	cs.sendMessage(ChatColor.RED + "You need to specify which rebellion to join!");
		  	return true;
	 }
	  
	  try {
		if ((!admin) && (!res.getTown().isMayor(res)))
		  {
		      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your town to do that!");
		      return true;
		  }
	} catch (NotRegisteredException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
	  
	  try {
		if (res.getTown().getNation().getCapital() == res.getTown())
		  {
			  cs.sendMessage(ChatColor.RED + "You cannot join a rebellion (towards yourself) when you are the capital!");
			  return true;
		  }
	} catch (NotRegisteredException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	  
	  for(Rebellion r : Rebellion.getAllRebellions()){
		  try {
			if(r.isRebelTown(res.getTown()) || r.isRebelLeader(res.getTown())){
			  		cs.sendMessage(ChatColor.RED + "You are already in a rebellion!");
			      	return true;
			  }
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	  for(Rebellion r : Rebellion.getAllRebellions()){
	  	try {
			if(r.getName().equals(strings[1]	) && res.getTown().getNation() == r.getMotherNation()){
				try {
					r.addRebell(res.getTown());
					cs.sendMessage(ChatColor.YELLOW + "You join the rebellion " + r.getName() + "!");
					Bukkit.getPlayer(r.getLeader().getMayor().getName()).sendMessage(ChatColor.YELLOW + res.getTown().getName() + " joined your rebellion!");
					return true;
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	cs.sendMessage(ChatColor.YELLOW + "No rebellion with that name!");
	return true;
  }
  
  //Author: Noxer
  private boolean leaveRebellion(CommandSender cs, String[] strings, boolean admin){
	  
	Resident res = null;
	
	try {
		res = TownyUniverse.getDataSource().getResident(cs.getName());
	} catch (NotRegisteredException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
	
	try {
		if ((!admin) && (!res.getTown().isMayor(res)))
		  {
		      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your town to do that!");
		      return true;
		  }
	} catch (NotRegisteredException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	  
	  for(Rebellion r : Rebellion.getAllRebellions())
		try {
			if(r.isRebelLeader(res.getTown())){
				Rebellion.getAllRebellions().remove(r);
				cs.sendMessage(ChatColor.RED + "You disbanded your rebellion in your nation!");
				return true;
			}
			else if(r.isRebelTown(res.getTown())){
				r.removeRebell(res.getTown());
				cs.sendMessage(ChatColor.RED + "You left the rebellion in your nation!");
				return true;
			}
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  
	  cs.sendMessage(ChatColor.RED + "You are not in a rebellion!");
	  return true;
  }
  
  //Author: Noxer
  private boolean executeRebellion(CommandSender cs, String[] strings, boolean admin){

	  Resident res = null;
	  
	  try {
			res = TownyUniverse.getDataSource().getResident(cs.getName());
		} catch (NotRegisteredException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	  
	  try {
		if ((!admin) && (!res.getTown().isMayor(res)))
		  {
		      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your town to do that!");
		      return true;
		  }
	} catch (NotRegisteredException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	  try {
		if (WarManager.getWarForNation(res.getTown().getNation()) != null)
		  {
		      cs.sendMessage(ChatColor.RED + "You can't rebel while your nation is at war!");
		      return true;
		  }
	} catch (NotRegisteredException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	  for(Rebellion r : Rebellion.getAllRebellions())
		try {
			if(res.getTown().getNation() == r.getMotherNation() && r.isRebelLeader(res.getTown())){
				  r.Execute(cs);
				  return true;
			}
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  
	  cs.sendMessage(ChatColor.RED + "You are not in a rebellion!");
      return true;
  }*/
  
  private boolean declareEnd(CommandSender cs, String[] strings, boolean admin)
  {
    if ((admin) && (strings.length < 3))
    {
    	if (!WarManager.endWar(strings[1])) {
    		cs.sendMessage(ChatColor.RED + "You need to specify a war!");
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    Resident resident = null;
    TownyWarsNation nation=null;
    try
    {
      if (admin)
      {
        nation = TownyWars.nationToTownyWarsNationHash.get(TownyUniverse.getDataSource().getNation(strings[2]));
      }
      else
      {
        resident = TownyUniverse.getDataSource().getResident(cs.getName());
        nation = TownyWars.nationToTownyWarsNationHash.get(resident.getTown().getNation());
      }
    }
    catch (Exception ex)
    {
    	if (admin) {
    		cs.sendMessage(ChatColor.RED+"The specified nations doesn't exist!");
    	}
    	else {
    		cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation!");
    	}
      return false;
    }
    if (!admin && !resident.isKing() && !nation.getNation().hasAssistant(resident))
    {
      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your nation to do that!");
      return false;
    }

    War currentWar=null;
	for (War war : nation.getWars()) {
		if (war.getName()==strings[1]) {
			currentWar=war;
			break;
		}
	}
	if (currentWar==null) {
		if (admin) {
			cs.sendMessage(ChatColor.RED + "The specified nation is not in the specified war!");
		}
		else {
			cs.sendMessage(ChatColor.RED + "You are not in the specified war");
		}
		return false;
	}
	
	if (admin) {
		WarManager.endWar(currentWar,nation);
		cs.sendMessage(ChatColor.GREEN + "Forced peace!");
	}
	else {
		WarManager.requestPeace(currentWar, nation);
		cs.sendMessage(ChatColor.GREEN + "Requested peace!");
	}
    return true;
  }
  
  //a quick way to start a flag war: the prepare phase is skipped and the flag war is started immediately
  private boolean declareFlagWar(CommandSender cs, String[] strings, boolean admin){
	  TownyWarsNation declarer=null;
	  TownyWarsTown target=null;
	  TownyWarsNation targetParent=null;
	  Resident resident=null;
	  if ((strings.length < 3) && (admin))
	    {
	      cs.sendMessage(ChatColor.RED + "You need to specify a nation and a town!");
	      return false;
	    }
	    if (strings.length < 2)
	    {
	      cs.sendMessage(ChatColor.RED + "You need to specify a town!");
	      return false;
	    }
	    try
		{
		  if (admin)
		  {
			  target = TownyWars.townToTownyWarsTownHash.get(TownyUniverse.getDataSource().getTown(strings[2]));
			  targetParent = TownyWars.nationToTownyWarsNationHash.get(target.getTown().getNation());
			  declarer = TownyWars.nationToTownyWarsNationHash.get(TownyUniverse.getDataSource().getNation(strings[1]));
		  }
		  else
		  {
		    resident = TownyUniverse.getDataSource().getResident(cs.getName());
		    target = TownyWars.townToTownyWarsTownHash.get(TownyUniverse.getDataSource().getTown(strings[1]));
		    targetParent = TownyWars.nationToTownyWarsNationHash.get(target.getTown().getNation());
		    declarer = TownyWars.nationToTownyWarsNationHash.get(resident.getTown().getNation());
		  }
		}
	    catch (Exception ex)
	    {
	      cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation, or your target is not part of a nation!");
	      return false;
	    }
	    if ((!admin) && (!declarer.getNation().isKing(resident)) && (!declarer.getNation().hasAssistant(resident)))
	    {
	      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your nation to do that!");
	      return false;
	    }
	    
	    // now create the quick rebellion
 		WarManager.quickFlagWar(declarer,target,targetParent);
 		
 		return true;
  }
  
  // a quick way to start a rebellion: the prepare phase is skipped and the rebellion is started immediately
  // this is particularly quick to set up because we know there will only be two participants and the other participant is the parent nation
  private boolean declareRebellion(CommandSender cs, String[] strings, boolean admin){
	  TownyWarsTown rebelTown=null;
	  TownyWarsNation parent=null;
	  Resident resident=null;
	  if ((strings.length < 3) && (admin))
	    {
	      cs.sendMessage(ChatColor.RED + "You need to specify a rebellion name and a town!");
	      return false;
	    }
	    if (strings.length == 1)
	    {
	      cs.sendMessage(ChatColor.RED + "You need to specify a name!");
	      return false;
	    }
		try
		{
		  if (admin)
		  {
			  rebelTown = TownyWars.townToTownyWarsTownHash.get(TownyUniverse.getDataSource().getTown(strings[2]));
			  parent = TownyWars.nationToTownyWarsNationHash.get(rebelTown.getTown().getNation());
		  }
		  else
		  {
		    resident = TownyUniverse.getDataSource().getResident(cs.getName());
		    rebelTown = TownyWars.townToTownyWarsTownHash.get(resident.getTown());
		    parent = TownyWars.nationToTownyWarsNationHash.get(resident.getTown().getNation());
		  }
		}
		catch (Exception ex)
	    {
	      cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation!");
	      return false;
	    }
	    if ((!admin) && (!rebelTown.getTown().isMayor(resident)) && (!rebelTown.getTown().hasAssistant(resident)))
	    {
	      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your town to do that!");
	      return false;
	    }
		
		// now create the quick rebellion
		if (!WarManager.quickRebellion(strings[1],rebelTown,parent)) {
			cs.sendMessage(ChatColor.RED+"Rebellion creation failed catastrophically! Contact a server admin ASAP!");
			return false;
		}
		return true;
  }
  
  
  // a quick way to start a war: the prepare phase is skipped and the war is started immediately as a conquest-style war
  private boolean declareWar(CommandSender cs, String[] strings, boolean admin)
  {
    if ((strings.length < 3) && (admin))
    {
      cs.sendMessage(ChatColor.RED + "You need to specify two nations!");
      return true;
    }
    if (strings.length == 1)
    {
      cs.sendMessage(ChatColor.RED + "You need to specify a nation!");
      return true;
    }
    
    TownyWarsNation declarer=null;
    List<TownyWarsNation> nations=new ArrayList<TownyWarsNation>();
    Resident resident=null;
    try
    {
      if (admin)
      {
        declarer = TownyWars.nationToTownyWarsNationHash.get(TownyUniverse.getDataSource().getNation(strings[1]));
      }
      else
      {
        resident = TownyUniverse.getDataSource().getResident(cs.getName());
        declarer = TownyWars.nationToTownyWarsNationHash.get(resident.getTown().getNation());
      }
    }
    catch (Exception ex)
    {
      cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation!");
      return false;
    }
    if ((!admin) && (!declarer.getNation().isKing(resident)) && (!declarer.getNation().hasAssistant(resident)))
    {
      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your nation to do that!");
      return false;
    }
    
    // here we're trying to form a list of nations to include in the war
    // adjust the start of the loop if the command sender was an admin
    for (int k=1+(admin?1:0); k<strings.length; k++) {
    	TownyWarsNation nation=null;
		try {
			nation = TownyWars.nationToTownyWarsNationHash.get(TownyUniverse.getDataSource().getNation(strings[k]));
		} catch (NotRegisteredException e) {
			cs.sendMessage(ChatColor.RED+"'"+strings[k]+"' is not a valid nation!");
			return false;
		}
    	nations.add(nation);
    }
    
    // send it to the war manager for processing
    WarManager.quickWar(declarer, nations);
    
    return true;
  }
}
