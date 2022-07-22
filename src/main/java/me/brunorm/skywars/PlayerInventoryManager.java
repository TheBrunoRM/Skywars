package me.brunorm.skywars;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class PlayerInventoryManager implements Listener {

	private static HashMap<Player, Inventory> inventories = new HashMap<Player, Inventory>();

	public static Inventory getInventory(Player player) {
		return inventories.get(player);
	}

	public static void setInventory(Player player, Inventory inventory) {
		inventories.put(player, inventory);
	}

	@EventHandler
	void onInventoryClose(InventoryCloseEvent event) {
		for (final Inventory inventory : inventories.values()) {
			if (inventory.equals(event.getInventory())) {
				inventories.remove(event.getInventory().getViewers().get(0));
			}
		}
	}
}
