package me.brunorm.skywars.menus;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class PlayerInventoryManager implements Listener {

	private static HashMap<Player, Inventory> inventories = new HashMap<>();

	public static Inventory getInventory(Player player) {
		return inventories.get(player);
	}

	public static void setInventory(Player player, Inventory inventory) {
		inventories.put(player, inventory);
	}

	@EventHandler
	void onInventoryClose(InventoryCloseEvent event) {
		GameSettingsMenu.currentMenus.remove(event.getPlayer());
		for (final Entry<Player, Inventory> set : inventories.entrySet()) {
			if (set.getKey().equals(event.getPlayer()) && set.getValue().equals(event.getInventory())) {
				inventories.remove(event.getPlayer());
			}
		}
	}

	public static HashMap<Player, Inventory> getInventories() {
		return inventories;
	}
}
