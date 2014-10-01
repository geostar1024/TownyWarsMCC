package net.minecraftcenter.townywars;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.text.SimpleDateFormat;

import net.minecraftcenter.townywars.object.TownyWarsNation;
import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.TownyWarsTown;
import net.minecraftcenter.townywars.object.War;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyWarsDatabase {
	
	public Connection connection=null;
	
	public TownyWarsDatabase(String file) {
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"+file);
	    } catch ( Exception e ) {
	    	this.connection=null;
	    }
	}
	
	public static final String[] tableNames={"townywarsresidents","townywarstowns","townywarsnations","townywarswars","townywarsdeaths"};

	public static final String[] tableCreation={
		"CREATE TABLE "+tableNames[0]+
        "(uuid TEXT PRIMARY KEY NOT NULL," +
        " name TEXT NOT NULL," + 
        " nation TEXT," + 
        " hit LONG DEFAULT 0," + 
        " attacker TEXT," +
        " login LONG DEFAULT 0," +
        " logout LONG DEFAULT 0," +
        " playtime LONG DEFAULT 0," +
        " wars TEXT)",
        
		"CREATE TABLE "+tableNames[1]+
        "(uuid TEXT PRIMARY KEY NOT NULL," +
        " name TEXT NOT NULL," + 
        " active BOOLEAN NOT NULL,"+
        " deaths INTEGER DEFAULT 0," + 
        " dp DOUBLE DEFAULT 0," + 
        " maxdp DOUBLE DEFAULT 0," +
        " conquered INTEGER DEFAULT 0,"+
        " mindpfactor DOUBLE DEFAULT .1)",
        
        "CREATE TABLE "+tableNames[2]+
        "(uuid TEXT PRIMARY KEY NOT NULL," +
        " name TEXT NOT NULL," + 
        " active BOOLEAN NOT NULL,"+
        " deaths INTEGER DEFAULT 0," + 
        " wars TEXT," + 
        " capitalpriority TEXT)",
        
        "CREATE TABLE "+tableNames[3]+
        "(uuid TEXT PRIMARY KEY NOT NULL," +
        " name TEXT NOT NULL," + 
        " start LONG DEFAULT 0," + 
        " end LONG DEFAULT 0," + 
        " prewar LONG DEFAULT 86400000," +
        " type TEXT DEFAULT 'NORMAL'," +
        " status TEXT DEFAULT 'PREPARE'," +
        " declarer TEXT," +
        " target TEXT,"+
        " targettown TEXT,"+
        " winner TEXT," +
        " threshold DOUBLE DEFAULT .3,"+
        " nations TEXT,"+
        " deaths TEXT," +
        " offers TEXT," +
        " accepted TEXT," +
        " money TEXT)",
        
        "CREATE TABLE "+tableNames[4]+
        "(time LONG PRIMARY KEY NOT NULL," +
        " date TEXT NOT NULL," +
        " puuid TEXT NOT NULL," +
        " pname TEXT NOT NULL," +
        " auuid TEXT," +
        " aname TEXT," +
        " cause TEXT," +
        " message TEXT)"
	};
			
	public static final String townyWarsTownInsert="replace into townywarstowns (uuid,name,active,deaths,dp,maxdp,conquered,mindpfactor)";
	public static final String townyWarsNationInsert="replace into townywarsnations (uuid,name,active,deaths,wars,capitalpriority)";
	public static final String townyWarsWarInsert="replace into townywarswars (uuid,name,start,end,prewar,type,status,declarer,target,targettown,winner,threshold,nations,offers,accepted,money)";
	public static final String townyWarsResidentInsert="replace into townywarsresidents (uuid,name,nation,hit,attacker,login,logout,playtime,wars)";
	public static final String townyWarsDeathInsert="replace into townywarsdeaths (time,date,puuid,pname,auuid,aname,cause,message)";
    
	public static Connection openDatabaseConnection(String file) {
	Connection c = null;
	try {
      Class.forName("org.sqlite.JDBC");
      c = DriverManager.getConnection("jdbc:sqlite:"+file);
    } catch ( Exception e ) {
      System.out.println( e.getClass().getName() + ": " + e.getMessage() );
    }
    System.out.println("Opened database successfully");
    return c;
	}
	
	public void close(){
		try {
			this.connection.close();
		} catch (SQLException e) {
			System.out.println("[ERROR] problem encounterd while closing database connection!");
		}
	}
	
	// used for replace and update
	public boolean executeSQL(String sql) {
		Statement statement=null;
		try {
			statement=this.connection.createStatement();
		    statement.executeUpdate(sql);
		    statement.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	    return false;
		}
		return true;
	}
	
	// used for select
	public ResultSet executeQuery(String sql) {
		Statement statement=null;
		ResultSet result=null;
		try {
			statement=this.connection.createStatement();
		    result=statement.executeQuery(sql);
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			return null;
		}
		return result;
	}
	
	public boolean createTables() {
		boolean noerror=true;
		for (int k=0;k<tableNames.length;k++) {
			String tableQuery="select name from sqlite_master where type='table' and name='"+tableNames[k]+"';";
			ResultSet result=this.executeQuery(tableQuery);
			if (result!=null) {
				try {
					if (!result.isBeforeFirst()) {
						if (!this.executeSQL(tableCreation[k])) {
							noerror=false;
						}
					}
				} catch (SQLException e) {
					System.out.println("[ERROR] some problem with checking that tables are created!");
				}
			}
		}
		return noerror;
	}
	
	public boolean saveKill(Long deathTime, Player player, Player killer, String damageCause, String deathMessage) {
		// convert the time in milliseconds to a date and then convert it to a string in a useful format (have to tack on the milliseconds)
		// format example: 2014-08-29 EDT 10:05:25:756
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd zzz HH:mm:ss");
		
		Date deathDate = new Date(deathTime);
	    String deathDateString = format.format(deathDate)+":"+deathTime%1000;
	    String killerName=null;
	    String killerUUIDString=null;
		
	    if (killer==null) {
	    	killerName="nonplayer";
	    	killerUUIDString="";
	    }
	    else {
	    	killerName=killer.getName();
	    	killerUUIDString=killer.getUniqueId().toString();
	    }
	    
	    String sql=townyWarsDeathInsert+" VALUES ("+deathTime.toString()+",'"+deathDateString+"','"+player.getUniqueId().toString()+"','"+player.getName()+"','"+killerUUIDString+"','"+killerName+"','"
	    		+damageCause+"','"+deathMessage+"');";
	    return this.executeSQL(sql);
	}
	
	public boolean saveNation(TownyWarsNation nation, boolean active) {
		String warString="";
		String capitalString="";
		for (War war : nation.getWars()) {
			warString+=","+war.getUUID().toString();
		}
		if (warString.isEmpty()) {
			warString="' '";
		}
		for (TownyWarsTown town : nation.getCapitalPriority()) {
			capitalString+=","+town.getUUID();
		}
		if (capitalString.isEmpty()) {
			capitalString="' '";
		}
		String sql=townyWarsNationInsert+" VALUES ('"+nation.getUUID().toString()+"','"+nation.getName()+"','"+Boolean.toString(active)+"',"+Integer.toString(nation.getDeaths())+","
				+warString+","+capitalString+");";
		return this.executeSQL(sql);
	}
	
	public boolean saveResident(TownyWarsResident resident) {
		String warString="";
		String lastNationUUID="";
		if (resident.getLastNation()!=null) {
			lastNationUUID=resident.getLastNation().getUUID().toString();
		}
		String lastAttackerUUID="";
		if (resident.getLastAttackerUUID()!=null) {
			lastAttackerUUID=resident.getLastAttackerUUID().toString();
		}
		for (War war : resident.getActiveWars()) {
			warString+=","+war.getUUID().toString();
		}
		if (warString.isEmpty()) {
			warString="''";
		}
		
		String sql=townyWarsResidentInsert+" VALUES ('"+resident.getUUID().toString()+"','"+resident.getPlayer().getName()+"','"+lastNationUUID+"',"
				+Long.toString(resident.getLastHitTime())+",'"+lastAttackerUUID+"',"+Long.toString(resident.getLastLoginTime())+","
				+Long.toString(resident.getLastLogoutTime())+","+Long.toString(resident.getTotalPlayTime())+","+warString+");";
		System.out.print(sql);
		return this.executeSQL(sql);
	}
	
	public boolean saveTown(TownyWarsTown town, boolean active) {
		String sql=townyWarsTownInsert+" VALUES ('"+town.getUUID().toString()+"','"+town.getName()+"','"+Boolean.toString(active)+"',"+Integer.toString(town.getDeaths())+","
				+Double.toString(town.getDP())+","+Double.toString(town.getMaxDP())+","+Integer.toString(town.getConquered())+","+Double.toString(town.getMinDPFactor())+");";
		return this.executeSQL(sql);
	}
	
	public boolean saveWar(War war) {
		String nationString="";
		String deathString="";
		String offerString="";
		String acceptedString="";
		String moneyString="";
		for (TownyWarsNation nation : war.getNations()) {
			nationString=","+nation.getUUID().toString();
			deathString+=","+Integer.toString(war.getDeaths(nation));
			offerString+=",'"+war.getPeaceOffer(nation)+"'";
			acceptedString+=",'"+Boolean.toString(war.acceptedPeace(nation))+"'";
			moneyString+=","+Double.toString(war.getRequestedMoney(nation));
		}
		if (offerString.isEmpty()) {
			offerString="''";
		}
		
		String sql=townyWarsWarInsert+" VALUES ('"+war.getUUID().toString()+"','"+war.getName()+"',"+Long.toString(war.getStartTime())+","
				+Long.toString(war.getEndTime())+","+Long.toString(war.getPrewarTime())+","+war.getWarType()+","+war.getWarStatus()+",'"
				+war.getDeclarer().getUUID().toString()+"','"+war.getTarget().getUUID().toString()+"','"+war.getTargetTown().getUUID().toString()+"','"+war.getWinner().getUUID().toString()+"',"
				+Double.toString(War.getThreshold())+","+nationString+","+deathString+","+offerString+","+acceptedString+","+moneyString+");";
		return this.executeSQL(sql);
	}
	
	public boolean saveAllTowns() {
		boolean noerror=true;
		for (TownyWarsTown town : TownyWarsTown.getAllTowns()) {
			if (!this.saveTown(town, true)) {
				noerror=false;
			}
		}
		return noerror;
	}
	
	public boolean saveAllNations() {
		boolean noerror=true;
		for (TownyWarsNation nation : TownyWarsNation.getAllNations()) {
			if (!this.saveNation(nation, true)) {
				noerror=false;
			}
		}
		return noerror;
	}
	
	public boolean saveAllResidents() {
		boolean noerror=true;
		for (TownyWarsResident resident : TownyWarsResident.getAllResidents()) {
			if (!this.saveResident(resident)) {
				noerror=false;
			}
		}
		return noerror;
	}
	
	public boolean saveAllWars() {
		boolean noerror=true;
		for (War war : WarManager.getWars()) {
			if (!this.saveWar(war)) {
				noerror=false;
			}
		}
		return noerror;
	}

	public boolean saveAll() {
		return saveAllResidents() && saveAllTowns() && saveAllNations() && saveAllWars();
	}
	
	
	// note that residents get loaded on demand as players log in, or during a server reload
	// this is because the number of players that are in the table is likely to be much much less than those in-game at any given time
	public boolean loadAll() {
		return loadAllTowns() && loadAllNations() && loadAllWars();
	}
	
	public boolean loadAllWars() {
		String sql="select * from townywarswars where state is not ENDED;";
		ResultSet result=this.executeQuery(sql);
		if (result==null) {
			return false;
		}
		try {
			while (result.next()) {
				String name=result.getString("name");
				UUID uuid=UUID.fromString(result.getString("uuid"));
				long startTime=result.getLong("start");
				long prewarTime=result.getLong("prewar");
				War.WarType type=War.WarType.valueOf(result.getString("type"));
				War.WarStatus status=War.WarStatus.valueOf(result.getString("status"));
				TownyWarsNation declarer=TownyWarsNation.getNation(UUID.fromString(result.getString("declarer")));
				TownyWarsNation target=TownyWarsNation.getNation(UUID.fromString(result.getString("target")));
				TownyWarsTown targettown=TownyWarsTown.getTown(UUID.fromString(result.getString("target")));
				String nationString=result.getString("nations");
				String deathsString=result.getString("deaths");
				String offersString=result.getString("offers");
				String acceptedString=result.getString("accepted");
				//String moneyString=result.getString("money");
				
				
				
				boolean error=false;
				List<TownyWarsNation> allNations=new ArrayList<TownyWarsNation>();
				for (String nationUUID : nationString.split(",")) {
					TownyWarsNation nation=TownyWarsNation.getNation(UUID.fromString(nationUUID));
					if (nation==null) {
						error=true;
						break;
					}
					allNations.add(nation);
				}
				if (error) {
					return false;
				}
				
				War war = new War(name,declarer,uuid);
				war.setWarType(type);
				war.setWarStatus(status);
				war.setTarget(target);
				war.setTargetTown(targettown);
				war.setPrewarTime(prewarTime);
				war.setStartTime(startTime);
				
				String allDeaths[]=deathsString.split(",");
				String allOffers[]=offersString.split(",");
				String allAccepted[]=acceptedString.split(",");
				//String allMoney[]=moneyString.split(",");
				
				for (int k=0; k<allNations.size(); k++) {
					TownyWarsNation nation=allNations.get(k);
					war.setDeaths(nation, Integer.parseInt(allDeaths[k]));
					war.setPeaceOffer(nation, allOffers[k]);
					if (Boolean.parseBoolean(allAccepted[k])) {
						war.acceptedPeace(nation);
					}
				}
				
			}
		
			result.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	public boolean loadAllTowns() {
		String sql="select * from townywarstowns where active is true;";
		ResultSet result=this.executeQuery(sql);
		if (result==null) {
			return false;
		}
		try {
			while (result.next()) {
				String name=result.getString("name");
				UUID uuid=UUID.fromString(result.getString("uuid"));
				Town town = null;
				int deaths=result.getInt("deaths");
				int conquered=result.getInt("conquered");
				double dp=result.getDouble("dp");
				double mindpfactor=result.getDouble("mindpfactor");
				try {
					 town = TownyUniverse.getDataSource().getTown(name);
				} catch (NotRegisteredException e) {
					
					// need to mark this town as inactive
					this.executeQuery("replace into townywarstowns (uuid,active) values ("+uuid+",false);");
				}
				if (town==null) {
					continue;
				}
				if (TownyWarsTown.getTown(town)!=null) {
					continue;
				}
				TownyWarsTown.putTown(town, new TownyWarsTown(town,uuid,deaths,dp,conquered,mindpfactor));
			}
		
			result.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean loadResident(Player player) {
		String sql="select * from townywarsresidents where uuid is '"+player.getUniqueId().toString()+"';";
		ResultSet result=this.executeQuery(sql);
		if (result==null) {
			return false;
		}
		try {
			String nationString=result.getString("nation");
			UUID nationUUID=null;
			if (!nationString.isEmpty()) {
				nationUUID=UUID.fromString(nationString);
			}
			long hitTime=result.getLong("hit");
			String attackerString=result.getString("attacker");
			UUID attackerUUID=null;
			if (!attackerString.isEmpty()) {
				attackerUUID=UUID.fromString(attackerString);
			}
			long loginTime=result.getLong("login");
			long logoutTime=result.getLong("logout");
			long playTime=result.getLong("playtime");
			String warString=result.getString("wars");
			TownyWarsResident resident=TownyWarsResident.putResident(player,false);
			if (resident==null) {
				System.out.println("[TownyWars] error loading player from database!");
			}
		
			// doesn't matter if it comes back null
			TownyWarsNation nation = TownyWarsNation.getNation(nationUUID);
			
			resident.setLastAttackerUUID(attackerUUID);
			resident.setLastHitTime(hitTime);
			resident.setLastLoginTime(loginTime);
			resident.setLastLogoutTime(logoutTime);
			resident.setTotalPlayTime(playTime);
			resident.setLastNation(nation);
			
			// match the saved war names to active wars for this resident
			for (String warName : warString.split(",")) {
				War war = WarManager.getWar(warName);
				if (war!=null) {
					resident.addActiveWar(war);
				}
			}

			result.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	public boolean loadAllNations() {
		String sql="select * from townywarsnations where active is true;";
		ResultSet result=this.executeQuery(sql);
		if (result==null) {
			return false;
		}
		try {
			while (result.next()) {
				String name=result.getString("name");
				UUID uuid=UUID.fromString(result.getString("uuid"));
				Nation nation = null;
				int deaths=result.getInt("deaths");
				String capitalString=result.getString("capitalpriority");
				try {
					 nation = TownyUniverse.getDataSource().getNation(name);
				} catch (NotRegisteredException e) {
					
					// need to mark this town as inactive
					this.executeQuery("replace into townywarsnations (uuid,active) values ("+uuid+",false);");
				}
				if (nation==null) {
					continue;
				}
				if (TownyWarsNation.getNation(nation)!=null) {
					continue;
				}

				TownyWarsNation newNation = new TownyWarsNation(nation,uuid);
				newNation.setDeaths(deaths);
				TownyWarsNation.putNation(nation,newNation);
				
				int k=0;
				for (String capital : capitalString.split(",")) {
					Town town=null;
					try {
						town=TownyUniverse.getDataSource().getTown(capital);
					} catch (NotRegisteredException e) {
						// town doesn't exist anymore
						continue;
					}
					newNation.setCapitalPriorityForTown(TownyWarsTown.getTown(town), k);
					k++;
				}
			}
		
			result.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	
	public static void closeDatabaseConnection(Connection c) {
		try {
			c.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	  
}