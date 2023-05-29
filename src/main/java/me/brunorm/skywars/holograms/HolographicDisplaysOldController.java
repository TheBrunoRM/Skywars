package me.brunorm.skywars.holograms;

import java.util.HashMap;

import org.bukkit.Location;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

import me.brunorm.skywars.Skywars;

public class HolographicDisplaysOldController implements HologramController {

	HashMap<String, Hologram> list = new HashMap<String, Hologram>();

	@Override
	public String createHologram(Object id, Location location, String text) {
		final Hologram hologram = HologramsAPI.createHologram(Skywars.get(), location);
		hologram.insertTextLine(0, text);
		this.list.put((String) id, hologram);
		return (String) id;
	}

	@Override
	public boolean changeHologram(Object id, String text, int line) {
		final Hologram hologram = this.list.get(id);
		if (text == null || text.isBlank() || text.isEmpty()) {
			hologram.removeLine(line);
			return hologram.getLine(line) == null;
		}
		return hologram.insertTextLine(line, text) != null;
	}

	@Override
	public void removeHologram(Object id) {
		this.list.get(id).delete();
	}

}
