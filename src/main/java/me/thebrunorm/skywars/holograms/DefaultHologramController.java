package me.thebrunorm.skywars.holograms;

import org.bukkit.Location;

public class DefaultHologramController implements HologramController {

	@Override
	public void removeHologram(Object id) {
	}

	@Override
	public String createHologram(Object id, Location location, String text) {
		return null;
	}

	@Override
	public boolean changeHologram(Object id, String text, int line) {
		return false;
	}

}
