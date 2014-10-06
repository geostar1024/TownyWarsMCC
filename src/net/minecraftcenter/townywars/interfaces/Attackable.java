package net.minecraftcenter.townywars.interfaces;

import java.util.Set;
import java.util.UUID;

import net.minecraftcenter.townywars.object.TownyWarsResident;
import net.minecraftcenter.townywars.object.War;

public interface Attackable {

	public boolean hasResident(TownyWarsResident resident);
	
	public Set<TownyWarsResident> getResidents();
	
	public void addResident(TownyWarsResident resident);
	
	public void removeResident(TownyWarsResident resident);
	
	public void addDeath();

	public void addWar(War war);

	public int getDeaths();

	public double getDP();
	
	public void setDP(double dp);
	
	public void setMaxDP(double maxdp);

	public double getMaxDP();

	public String getName();

	public UUID getUUID();

	public Set<War> getWars();
	
	public War getWar(String warName);
	
	public War getStoredWar(String warName);
	
	public boolean hasWar(War war);
	
	public boolean hasStoredWar(War war);

	public void removeWar(War war);

	public void setDeaths(int deaths);

	public void setName(String name);
	
	public Set<War> getStoredWars();
	
	public void addStoredWar(War war);
	
	public void removeStoredWar(War war);
	
	public void clearStoredWars();
	
	public boolean save(boolean active);
	
	public boolean save();
}