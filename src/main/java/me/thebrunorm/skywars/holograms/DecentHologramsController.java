/* (C) 2021 Bruno */
package me.thebrunorm.skywars.holograms;

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
	public boolean changeHologram(Object id, String text, int line) {
		final Hologram holo = DHAPI.getHologram((String) id);
		if (holo.getPage(0).getLine(line) == null)
			return DHAPI.addHologramLine(holo, text) != null;
		else if (text == null || text.isEmpty() || text.isEmpty())
			holo.getPage(0).removeLine(line);
		return holo.getPage(0).setLine(line, text);
	}

	@Override
	public void removeHologram(Object id) {
		DHAPI.removeHologram((String) id);
	}

}
