package net.minecraftcenter.townywars;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Resident;

// some extra resident fields needed to properly record deaths
public class TownyWarsResident{
	
	private Resident resident;
	private Player player;
	private long lastHitTime=0;
	private UUID lastAttackerUUID=null;
	private long lastLoginTime=0;
	private long lastLogoutTime=0;
	private long totalPlayTime=0;
	private Set<War> activeWars=new HashSet<War>();
	private TownyWarsNation lastNation=null;
	
	public TownyWarsResident(Player player, Resident resident){
		this.player=player;
		this.resident=resident;
	}
	
	public TownyWarsResident(Player player, Resident resident, TownyWarsNation nation){
		this.player=player;
		this.resident=resident;
		this.lastNation=nation;
	}
	
	public UUID getUUID(){
		return this.player.getUniqueId();
	}
	
	public Resident getResident() {
		return this.resident;
	}
	
	public long getLastHitTime(){
		return this.lastHitTime;
	}
	
	public Player getPlayer() {
		return this.player;
	}
	
	public void setLastHitTime(long newHitTime){
		this.lastHitTime=newHitTime;
	}
	
	public UUID getLastAttackerUUID(){
		return this.lastAttackerUUID;
	}
	
	public void setLastAttackerUUID(UUID attackerUUID){
		this.lastAttackerUUID=attackerUUID;
	}
	
	public long getLastLoginTime(){
		return this.lastLoginTime;
	}
	
	public void setLastLoginTime(long loginTime) {
		this.lastLoginTime=loginTime;
	}
	
	public long getLastLogoutTime(){
		return this.lastLogoutTime;
	}
	
	public void setLastLogoutTime(long logoutTime) {
		this.lastLogoutTime=logoutTime;
	}
	
	public long getTotalPlayTime(){
		return this.totalPlayTime;
	}
	
	public void setTotalPlayTime(long totalTime) {
		this.totalPlayTime=totalTime;
	}
	
	public void removeOldWar(War war) {
		this.activeWars.remove(war);
	}
	
	public Set<War> getActiveWars(){
		return this.activeWars;
	}
	
	public void addActiveWar(War war) {
		this.activeWars.add(war);
	}
	
	public boolean isInActiveWar(War war){
		return this.activeWars.contains(war);
	}
	
	public TownyWarsNation getLastNation() {
		return this.lastNation;
	}
	
	public void setLastNation(TownyWarsNation nation) {
		this.lastNation=nation;
	}
}