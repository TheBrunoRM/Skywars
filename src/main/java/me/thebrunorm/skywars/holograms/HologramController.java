package me.thebrunorm.skywars.holograms;

import org.bukkit.Location;

public abstract interface HologramController {
	public abstract String createHologram(Object id, Location location, String text);

	public abstract boolean changeHologram(Object id, String text, int line);

	public abstract void removeHologram(Object id);
}
