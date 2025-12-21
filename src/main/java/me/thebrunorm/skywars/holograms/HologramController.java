/* (C) 2021 Bruno */
package me.thebrunorm.skywars.holograms;

import org.bukkit.Location;

public interface HologramController {
	String createHologram(Object id, Location location, String text);

	boolean changeHologram(Object id, String text, int line);

	void removeHologram(Object id);
}
