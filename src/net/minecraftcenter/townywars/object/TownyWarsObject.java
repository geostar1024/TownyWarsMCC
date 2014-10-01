package net.minecraftcenter.townywars.object;

import java.util.UUID;

/**
 * 
 * @author geostar1024
 * 
 * a base object class that provides for a name and UUID
 */
public abstract class TownyWarsObject{
	
	private String name=null;
	private UUID uuid=null;
	
	TownyWarsObject(UUID uuid) {
		this.uuid=uuid;
	}
	
	public UUID getUUID() {
		return this.uuid;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name=name;
	}
	
}