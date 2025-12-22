// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.menus;

import me.thebrunorm.skywars.Skywars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.UUID;

public class PlayerInventoryManager implements Listener {

	private static final HashMap<UUID, MenuType> currentMenu = new HashMap<>();

	@EventHandler
	void onInventoryClose(InventoryCloseEvent event) {
		final Player player = (Player) event.getPlayer();
		if (getCurrentMenu(player) == null) return;
		Skywars.get().sendDebugMessage("inventory closed: " + event.getPlayer().getName());
		setMenu(player, null);
	}

	public static MenuType getCurrentMenu(Player player) {
		return currentMenu.get(player.getUniqueId());
	}

	public static void setMenu(Player player, MenuType menu) {
		Skywars.get().sendDebugMessage("inventory set to " + menu + ": " + player.getName());
		currentMenu.put(player.getUniqueId(), menu);
	}
}
