package me.brunorm.skywars.holograms;

import org.bukkit.Location;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

public class DecentHologramsController implements HologramController {

	@Override
	public String createHologram(Object id, Location location, String text) {
		final Hologram holo = DHAPI.createHologram((String) id, location);
		DHAPI.addHologramLine(holo, text);
		return holo.getName();
	}

	@Override
	public boolean changeHologram(Object id, String text) {
		return DHAPI.getHologram((String) id).getPage(0).setLine(0, text);
	}

	@Override
	public void removeHologram(Object id) {
		DHAPI.removeHologram((String) id);
	}

}
