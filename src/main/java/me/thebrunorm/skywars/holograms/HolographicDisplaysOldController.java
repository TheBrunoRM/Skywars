/* (C) 2021 Bruno */
package me.thebrunorm.skywars.holograms;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.thebrunorm.skywars.Skywars;
import org.bukkit.Location;

import java.util.HashMap;

public class HolographicDisplaysOldController implements HologramController {

	HashMap<String, Hologram> list = new HashMap<>();

	@Override
	public String createHologram(String id, Location location, String text) {
		final Hologram hologram = HologramsAPI.createHologram(Skywars.get(), location);
		hologram.insertTextLine(0, text);
		this.list.put(id, hologram);
		return id;
	}

	@Override
	public void changeHologram(String id, String text, int line) {
		final Hologram hologram = this.list.get(id);
		if (text == null || text.isEmpty()) {
			hologram.removeLine(line);
			hologram.getLine(line);
			return;
		}
		hologram.insertTextLine(line, text);
	}

	@Override
	public void removeHologram(String id) {
		this.list.get(id).delete();
	}

}
