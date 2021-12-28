package me.brunorm.skywars.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;

public class SignEvents implements Listener {
	
	@EventHandler
	void onSignChange(SignChangeEvent event) {
		if(event.getLine(1).equalsIgnoreCase("[SkyWars]")) {
			String arenaName = event.getLine(2);
			if(arenaName != null) {
				Arena arena = Skywars.get().getArena(arenaName);
				if(arena != null) {
					event.setLine(1, Messager.color("&e[&bSkyWars&e]"));
					event.setLine(2, Messager.color(String.format("&a%s", arena.getName())));
				} else System.out.println("null arena " + arenaName);
			} else System.out.println("null arena name");
		}
	}
	
}
