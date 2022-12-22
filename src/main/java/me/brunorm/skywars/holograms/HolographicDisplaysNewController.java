package me.brunorm.skywars.holograms;

import java.util.HashMap;

import org.bukkit.Location;

import me.brunorm.skywars.Skywars;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;

public class HolographicDisplaysNewController implements HologramController {

	HashMap<String, Hologram> list = new HashMap<String, Hologram>();

	@Override
	public String createHologram(Object id, Location location, String text) {
		final HolographicDisplaysAPI api = HolographicDisplaysAPI.get(Skywars.get());
		final Hologram hologram = api.createHologram(location);
		hologram.getLines().appendText(text);
		this.list.put((String) id, hologram);
		return (String) id;
	}

	@Override
	public boolean changeHologram(Object id, String text, int line) {
		final Hologram holo = this.list.get(id);
		return holo.getLines().insertText(line, text) != null;
	}

	@Override
	public void removeHologram(Object id) {
		this.list.get(id).delete();
	}

}
