package me.brunorm.skywars.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import mrblobman.sounds.Sounds;

public class MessageSound implements Listener {

	@EventHandler
	void onMessage(AsyncPlayerChatEvent event) {
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), Sounds.ITEM_PICKUP.bukkitSound(), 1, (float) (Math.random() + 0.5f));
		}
	}
	
}
