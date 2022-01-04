package me.brunorm.skywars.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.brunorm.skywars.Skywars;
import mrblobman.sounds.Sounds;

public class MessageSound implements Listener {

	String sound = Skywars.config.getString("messageSounds.sound");
	
	@EventHandler
	void onMessage(AsyncPlayerChatEvent event) {
		float random = (float) (Math.random() + 0.5f);
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), Sounds.valueOf(sound).bukkitSound(), 1, random);
		}
	}
	
}
