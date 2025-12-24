// Copyright (c) 2025 Bruno

package me.thebrunorm.skywars.events;

import me.thebrunorm.skywars.managers.ArenaManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EndPortalJoinsRandomGame implements Listener {
	@EventHandler
	void onPlayerPortal(PlayerPortalEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) return;
		event.setTo(event.getPlayer().getLocation());
		event.setCancelled(true);
		ArenaManager.joinRandomMap(event.getPlayer());
	}
}
