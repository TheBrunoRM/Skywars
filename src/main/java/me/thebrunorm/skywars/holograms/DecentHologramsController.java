/* (C) 2021 Bruno */
package me.thebrunorm.skywars.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

public class DecentHologramsController implements HologramController {

	@Override
	public String createHologram(String id, Location location, String text) {
		final Hologram holo = DHAPI.createHologram(id, location);
		DHAPI.addHologramLine(holo, text);
		return holo.getName();
	}

	@Override
	public void changeHologram(String id, String text, int line) {
		final Hologram holo = DHAPI.getHologram(id);
		if (holo == null) return;
		if (holo.getPage(0).getLine(line) == null) {
			DHAPI.addHologramLine(holo, text);
			return;
		} else if (text == null || text.isEmpty()) {
			holo.getPage(0).removeLine(line);
			return;
		}
		holo.getPage(0).setLine(line, text);
	}

	@Override
	public void removeHologram(String id) {
		DHAPI.removeHologram(id);
	}

}
