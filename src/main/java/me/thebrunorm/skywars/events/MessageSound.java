/* (C) 2021 Bruno */
package me.thebrunorm.skywars.events;

import mrblobman.sounds.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.thebrunorm.skywars.Skywars;

public class MessageSound implements Listener {

	@EventHandler
	void onMessage(AsyncPlayerChatEvent event) {
		String soundName = Skywars.get().getConfig().getString("messageSounds.sound");
		Sound sound = Sounds.valueOf(soundName).bukkitSound();
		if(sound == null) {
			Skywars.get().getLogger().severe("[Message Sounds] Could not find sound: " + soundName);
			return;
		}
		final float random = (float) (Math.random() + 0.5f);
		for (final Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), sound, 1, random);
		}
	}

}
