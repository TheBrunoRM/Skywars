/* (C) 2021 Bruno */
package me.thebrunorm.skywars.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.thebrunorm.skywars.Skywars;
import mrblobman.sounds.Sounds;

public class MessageSound implements Listener {

	String sound = Skywars.config.getString("messageSounds.sound");

	@EventHandler
	void onMessage(AsyncPlayerChatEvent event) {
		final float random = (float) (Math.random() + 0.5f);
		for (final Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), Sounds.valueOf(this.sound).bukkitSound(), 1, random);
		}
	}

}
