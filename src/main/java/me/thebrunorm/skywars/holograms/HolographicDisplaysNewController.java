/* (C) 2021 Bruno */
package me.thebrunorm.skywars.holograms;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.thebrunorm.skywars.Skywars;
import org.bukkit.Location;

import java.util.HashMap;

public class HolographicDisplaysNewController implements HologramController {

	final HashMap<String, Hologram> list = new HashMap<>();

	@Override
	public String createHologram(String id, Location location, String text) {
		final HolographicDisplaysAPI api = HolographicDisplaysAPI.get(Skywars.get());
		final Hologram hologram = api.createHologram(location);
		hologram.getLines().appendText(text);
		this.list.put(id, hologram);
		return id;
	}

	@Override
	public void changeHologram(String id, String text, int line) {
		final Hologram hologram = this.list.get(id);
		if (hologram == null) return;
		hologram.getLines().insertText(line, text);
	}

	@Override
	public void removeHologram(String id) {
		this.list.get(id).delete();
	}

}
