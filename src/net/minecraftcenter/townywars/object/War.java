package net.minecraftcenter.townywars.object;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraftcenter.townywars.interfaces.Attackable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class War extends TownyWarsObject {

	public static List<Attackable>	testmap	= new ArrayList<Attackable>();

	public static enum WarType {
		NORMAL, REBELLION, RAID,
	}

	public static enum WarStatus {
		PREPARE, PREWAR, CURRENT, ENDED,
	}

	private static final double	threshold	   = .3;
	private long	            startTime	   = 0;
	private long	            endTime	       = 0;
	private long	            prewarTime	   = 0;
	private WarType	            type	       = null;
	private WarStatus	        status	       = null;
	private Attackable	        target	       = null;
	private Attackable	        declarer	   = null;
	private Attackable	        winner	       = null;

	private Map<UUID, String>	peaceOffers	   = new HashMap<UUID, String>();
	private Map<UUID, Double>	requestedMoney	= new HashMap<UUID, Double>();
	private Map<UUID, Integer>	deaths	       = new HashMap<UUID, Integer>();
	private Set<UUID>	        peaceAccepted	= new HashSet<UUID>();

	public War(String name, Attackable declarer2, UUID uuid) {
		super(uuid);
		newWar(name, declarer2);
	}

	// use a random UUID to uniquely identify this war
	public War(String name, TownyWarsOrg declarer) {
		super(UUID.randomUUID());
		newWar(name, declarer);
	}

	// everything needed to start setting up a new war
	// minimum information is a name for the war, the organization that declared, and a UUID for the war object
	private void newWar(String name, Attackable declarer) {
		setName(name);

		// by default, the prewar period is 24 hours
		this.prewarTime = 24 * 3600 * 1000;

		// by default, all wars start in the preparation phase
		this.status = WarStatus.PREPARE;

		// the creator of the war is by default the declarer
		this.declarer = declarer;

		this.deaths.put(declarer.getUUID(), 0);

		//testmap.add(new TownyWarsNation((Nation) this.declarer.getObject()));
		//testmap.add(new TownyWarsTown((Town) this.declarer.getObject()));
	}

	// time to start the war!
	public boolean execute() {

		// make sure the war is sufficiently set up
		if (!checkWar()) {
			System.out.println("war not setup correctly!");
			return false;
		}

		// record the war start time and set the status to CURRENT
		this.startTime = System.currentTimeMillis();
		this.status = WarStatus.CURRENT;

		// store the reference to this war in each of the TownyWarOrg objects
		for (Attackable org : this.getOrgs()) {
			org.addWar(this);
		}

		// make all nations in this war enemies!
		for (Attackable org1 : this.getOrgs()) {
			for (Attackable org2 : this.getOrgs()) {
				if (org1 != org2) {
					try {
						// skip enemies stuff if the orgs are just towns
						if (org1 instanceof TownyWarsNation && org2 instanceof TownyWarsNation) {
							((TownyWarsNation) org1).getNation().addEnemy(((TownyWarsNation) org2).getNation());
						}
					} catch (AlreadyRegisteredException e) {
						// ignore if a nation is already the enemy of another nation; could happen if two nations happen to be in multiple wars
					}
				}
			}
		}

		// turn on pvp for all the towns in all the orgs at war!
		for (Attackable org : this.getOrgs()) {
			if (org instanceof TownyWarsNation) {
				for (Town town : ((TownyWarsNation) org).getNation().getTowns()) {
					town.setPVP(true);
				}
			}
			if (org instanceof TownyWarsTown) {
				((TownyWarsTown) org).getTown().setPVP(true);
			}
		}

		// save the stuff we've been doing
		TownyUniverse.getDataSource().saveAll();

		informPlayers(true);
		return true;
	}

	// determine if enough war parameters are set
	boolean checkWar() {
		if (this.type == null || (this.target == null && this.type == WarType.RAID) || this.getOrgs().size() < 2) {
			return false;
		}
		return true;
	}

	public Attackable getDeclarer() {
		return this.declarer;
	}

	public void setStartTime(long time) {
		this.startTime = time;
	}

	public List<Attackable> getOrgs() {
		List<Attackable> allOrgs = new ArrayList<Attackable>();
		allOrgs.add(this.declarer);
		allOrgs.add(this.target);
		return allOrgs;
	}

	public WarType getWarType() {
		return this.type;
	}

	public WarStatus getWarStatus() {
		return this.status;
	}

	public void setWarStatus(WarStatus status) {
		this.status = status;
	}

	public static double getThreshold() {
		return threshold;
	}

	public boolean setWarType(WarType type) {
		// only allow this modification before the war is executed
		if (this.status != WarStatus.PREPARE) {
			return false;
		}
		this.type = type;
		return true;
	}

	public Attackable getWinner() {
		return this.winner;
	}

	public void setWinner(Attackable winner) {
		this.winner = winner;
	}

	public Attackable getTarget() {
		return this.target;
	}

	public void setTarget(Attackable org) {
		if (this.target != null) {
			this.deaths.remove(this.target.getUUID());
		}
		this.target = org;
		this.deaths.put(this.target.getUUID(), 0);

	}

	public long getStartTime() {
		return this.startTime;
	}

	public long getEndTime() {
		return this.endTime;
	}

	public void setEndTime(long time) {
		this.endTime = time;
	}

	public int getDeaths(Attackable org) {
		return this.deaths.get(org.getUUID());
	}

	public void setDeaths(Attackable org, int deaths) {
		this.deaths.put(org.getUUID(), deaths);
	}

	public String getPeaceOffer(Attackable org) {
		return this.peaceOffers.get(org.getUUID());
	}

	public boolean offeredPeace(Attackable org) {
		return (this.peaceOffers.get(org.getUUID()) != null);
	}

	public boolean acceptedPeace(Attackable org) {
		return this.peaceAccepted.contains(org.getUUID());
	}

	public void acceptPeace(Attackable org) {
		this.peaceAccepted.add(org.getUUID());
	}

	public void setPeaceOffer(Attackable org, String peaceOffer) {
		this.peaceOffers.put(org.getUUID(), peaceOffer);
	}

	public boolean allPeaceOffered() {
		for (Attackable org : this.getOrgs()) {
			if (this.peaceOffers.get(org.getUUID()) == null) {
				return false;
			}
		}
		return true;
	}

	public boolean allPeaceAccepted() {
		for (Attackable org : this.getOrgs()) {
			if (!this.peaceAccepted.contains(org.getUUID())) {
				return false;
			}
		}
		return true;
	}

	public double getRequestedMoney(Attackable org) {
		return this.requestedMoney.get(org.getUUID());
	}

	public void setRequestedMoney(TownyWarsOrg org, double money) {
		this.requestedMoney.put(org.getUUID(), money);
	}

	public Attackable checkWinner() {
		switch (this.type) {
		case RAID:
			// if the target town's DP or the DP of the target town's org is depleted below the threshold, the declarer wins
			if (target.getDP() / target.getMaxDP() < threshold) {
				return this.declarer;
			}

			// if the declarer's DP is depleted below the threshold, the target wins
			if (this.declarer.getDP() / this.declarer.getMaxDP() < threshold) {
				return this.target;
			}
			break;
		case NORMAL:
			if (this.getOrgs().size() == 1) {
				for (Attackable org : this.getOrgs()) {
					return org;
				}
			}
			break;
		case REBELLION:
			// check if the parent org has been brought to its knees
			if (target.getDP() / target.getMaxDP() < threshold) {
				return this.declarer;
			}

			// if the rebel org's DP is depleted below the threshold, the parent org wins
			if (this.declarer.getDP() / this.declarer.getMaxDP() < threshold) {
				return this.target;
			}
			// no org has won yet
			break;
		default:
			break;
		}
		return null;
	}

	public Attackable getEnemy(Attackable declarer) {
		if (declarer == this.declarer) {
			return this.target;
		} else {
			return this.declarer;
		}
	}

	public boolean hasMember(TownyWarsOrg org) {
		return (this.getOrgs().contains(org));
	}

	// make sure that the prewar time is between 24 and 48 hours, otherwise return false
	public boolean setPrewarTime(long time) {
		if (time > 24 * 3600 * 1000 && time < 48 * 3600 * 1000) {
			this.prewarTime = time;
			return true;
		}
		return false;
	}

	public long getPrewarTime() {
		return this.prewarTime;
	}

	public long getElapsedTime(WarStatus status) {
		switch (status) {
		case PREPARE:
			return 0;
		case PREWAR:
			return System.currentTimeMillis() - this.prewarTime;
		case ENDED:
			return this.endTime - this.startTime;
		default:
			return System.currentTimeMillis() - this.startTime;
		}

	}

	// let all or just the players in this war know its current status as long as it's not in the prepare phase (that would be awkward)
	// if a particular player is specified, let them know the current status of this war even if it's being prepared
	public void informPlayers(TownyWarsResident resident) {
		informplayers(resident, false);
	}

	public void informPlayers(boolean all) {
		informplayers(null, true);
	}

	private void informplayers(TownyWarsResident resident, boolean all) {
		if (this.status == WarStatus.PREPARE && resident == null) {
			return;
		}
		String message = ChatColor.GOLD + "War " + ChatColor.WHITE + getName() + ": " + ChatColor.GOLD;
		if (all) {
			message += ((TownyWarsOrg)this.declarer).getName() + informPlayersSelector(this.declarer);
			Bukkit.getServer().broadcastMessage(message);
			return;
		}

		if (resident != null) {
			Attackable org = null;
			try {
				org = TownyWarsNation.getNation(resident.getResident().getTown().getNation());
			} catch (NotRegisteredException e) {
				// just continue, it isn't fatal if this resident isn't in a org
			}
			if (org == this.declarer) {
				message += "Your org";
			} else {
				message += ((TownyWarsOrg)this.declarer).getName();
			}
			message += informPlayersSelector(this.declarer);
			resident.getPlayer().sendMessage(message);
			return;
		}
		for (Attackable org : this.getOrgs()) {
			if (org == this.declarer) {
				message += "Your org";
			} else {
				message += org.getName();
			}
			message += informPlayersSelector(this.declarer);

			// display for all players in the org
			if (org instanceof TownyWarsTown) {
				for (Resident resident1 : ((TownyWarsTown) org).getTown().getResidents()) {
					Player plr = Bukkit.getPlayer(resident1.getName());
					if (plr != null) {
						plr.sendMessage(message);
					}
				}
			}
			if (org instanceof TownyWarsNation) {
				for (Resident resident1 : ((TownyWarsNation) org).getNation().getResidents()) {
					Player plr = Bukkit.getPlayer(resident1.getName());
					if (plr != null) {
						plr.sendMessage(message);
					}
				}
			}
		}

	}

	private String informPlayersSelector(Attackable declarer) {
		String message = " ";
		switch (this.status) {
		case CURRENT:
			message += "is in";
			break;
		case ENDED:
			if (declarer == this.winner) {
				message += "won";
			} else if (this.winner == null) {
				message += "made peace in";
			} else {
				message += "lost";
			}
			break;
		case PREWAR:
			message += "is threatening";
			break;
		case PREPARE:
			message += "is preparing";
			break;
		default:
			break;
		}
		message += " a ";
		switch (this.type) {
		case RAID:
			message += "flag war";
			break;
		case NORMAL:
			message += "war";
			break;
		case REBELLION:
			message += "rebellion";
			break;
		default:
			break;
		}

		// now handle the enemy
		message += " against " + ((TownyWarsOrg)this.getEnemy(declarer)).getName();
		return message;
	}

	// immediately end the war for everyone involved
	public void end() {
		this.setWinner(this.checkWinner());
		// indicate that the war has ended
		this.status = WarStatus.ENDED;
		this.endTime = System.currentTimeMillis();

		this.informPlayers(true);
		for (Attackable org1 : this.getOrgs()) {
			org1.removeWar(this);

			// make this org neutral (remove from enemies lists)
			// and remove the other orgs in the war from its enemies list
			for (Attackable org2 : this.getOrgs()) {
				if (org1 != org2) {
					// skip enemies stuff if the orgs are just towns
					if (org1.getClass().equals(TownyWarsTown.class) || org2.getClass().equals(TownyWarsTown.class)) {
						continue;
					}
					try {
						((TownyWarsNation) org1).getNation().removeEnemy(((TownyWarsNation) org2).getNation());
						((TownyWarsNation) org2).getNation().removeEnemy(((TownyWarsNation) org1).getNation());
					} catch (NotRegisteredException e) {
						// evidently the nations weren't enemies for whatever reason, so just continue
					}
				}
			}

			// turn off pvp for all the towns in this org, as long as it's not in other wars
			// also reset the conquest counter
			if (org1.getWars().isEmpty()) {
				if (org1.getClass().equals(TownyWarsNation.class)) {
					for (Town town : ((TownyWarsNation) org1).getNation().getTowns()) {
						town.setPVP(false);
						TownyWarsTown.getTown(town).setConquered(0);
					}
				}
				if (org1.getClass().equals(TownyWarsTown.class)) {
					((TownyWarsTown) org1).getTown().setPVP(false);
					((TownyWarsTown) org1).setConquered(0);
				}
			}
		}

		// finally, handle the special case of a rebellion ending not in the rebels' favor
		if (this.type == WarType.REBELLION && this.winner != this.declarer) {
			// TownyWarsTown loser = TownyWarsTown.getTown(this.declarer.getObject().getCapital());
			try {
				// this.declarer.getObject().removeTown(loser.getTown());
				((TownyWarsNation) this.winner).getNation().addTown(((TownyWarsTown) this.declarer).getTown());
			} catch (Exception e) {
				System.out.println("[TownyWars] cleaning up after failed rebellion failed catastrophically!");
				e.printStackTrace();
			}
			// TownyUniverse.getDataSource().removeObject(this.declarer.getObject());

			// TownyWarsObject.removeObject(this.declarer);
		}

		// save the stuff we've been doing
		TownyUniverse.getDataSource().saveAll();
	}

}
