/* (C) 2021 Bruno */
package me.thebrunorm.skywars.holograms;

import org.bukkit.Location;

public interface HologramController {
	String createHologram(String id, Location location, String text);

	void changeHologram(String id, String text, int line);

	void removeHologram(String id);
}
