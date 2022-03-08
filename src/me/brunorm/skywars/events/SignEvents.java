package me.brunorm.skywars.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.SkywarsMap;

public class SignEvents implements Listener {

	@EventHandler
	void onSignChange(SignChangeEvent event) {
		if (event.getLine(1).equalsIgnoreCase("[SkyWars]")) {
			final String mapName = event.getLine(2);
			if (mapName != null) {
				final SkywarsMap map = Skywars.get().getMap(mapName);
				if (map != null) {
					event.setLine(1, Messager.color("&e[&bSkyWars&e]"));
					event.setLine(2, Messager.color(String.format("&a%s", map.getName())));
				}
			}
		}
	}

}
