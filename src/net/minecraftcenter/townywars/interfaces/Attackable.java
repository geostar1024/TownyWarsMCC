package net.minecraftcenter.townywars.interfaces;

import java.util.Set;
import java.util.UUID;

import net.minecraftcenter.townywars.object.War;

public interface Attackable {

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

	public void removeWar(War war);

	public void setDeaths(int deaths);

	public void setName(String name);
}