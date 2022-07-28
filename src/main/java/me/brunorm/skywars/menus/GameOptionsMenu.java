package me.brunorm.skywars.menus;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.InventoryUtils;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;

public class GameOptionsMenu implements Listener {

	static HashMap<Player, Inventory> inventories = new HashMap<Player, Inventory>();
	static HashMap<UUID, OptionMenu> currentMenus = new HashMap<UUID, OptionMenu>();

	public static void open(Player player) {
		open(player, OptionMenu.MAIN);
	}

	public static void open(Player player, OptionMenu menu) {
		Inventory inventory = null;
		currentMenus.put(player.getUniqueId(), menu);
		switch (menu) {
		case MAIN:
			inventory = Bukkit.createInventory(null, 9 * 3, "Game settings");

			InventoryUtils.addItem(inventory, "&aWeather", 10, XMaterial.NETHER_STAR.parseItem());
			InventoryUtils.addItem(inventory, "&aTime", 13, XMaterial.CLOCK.parseItem());
			InventoryUtils.addItem(inventory, "&aChests", 16, XMaterial.CHEST.parseItem());

			break;
		case WEATHER:
			inventory = Bukkit.createInventory(null, 9 * 3, "Weather settings");

			InventoryUtils.addItem(inventory, "&aSunny", 11, XMaterial.SUNFLOWER.parseItem());
			InventoryUtils.addItem(inventory, "&aRainy", 15, XMaterial.ENDER_PEARL.parseItem());

			break;
		case CHESTS:
			inventory = Bukkit.createInventory(null, 9 * 3, "Chest settings");

			InventoryUtils.addItem(inventory, "&aNormal", 11, XMaterial.CHEST.parseItem());
			InventoryUtils.addItem(inventory, "&cOverpowered", 15, XMaterial.ENDER_CHEST.parseItem());

			break;
		case TIME:
			inventory = Bukkit.createInventory(null, 9 * 3, "Time settings");

			InventoryUtils.addItem(inventory, "&aDay", 11, XMaterial.SUNFLOWER.parseItem());
			InventoryUtils.addItem(inventory, "&aNight", 15, XMaterial.ENDER_PEARL.parseItem());

			break;
		default:
			break;
		}
		inventories.put(player, inventory);
		player.openInventory(inventory);
	}

	@EventHandler
	void onClick(InventoryClickEvent event) {
		final Player player = (Player) event.getWhoClicked();
		final Inventory inventory = inventories.get(player);
		if (!event.getInventory().equals(inventory))
			return;
		event.setCancelled(true);
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null)
			return;
		final OptionMenu currentMenu = currentMenus.get(player.getUniqueId());
		if (currentMenu == null)
			return;
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		switch (currentMenu) {
		case MAIN:
			switch (event.getSlot()) {
			case 10:
				open(player, OptionMenu.WEATHER);
				break;
			case 13:
				open(player, OptionMenu.TIME);
				break;
			case 16:
				open(player, OptionMenu.CHESTS);
				break;
			default:
				open(player, OptionMenu.MAIN);
			}
			return;
		case TIME:
			switch (event.getSlot()) {
			case 11: // day
				arena.changeGameSettings(0);
				break;
			case 15: // night
				arena.changeGameSettings(14000);
				break;
			}
			break;
		case WEATHER:
			switch (event.getSlot()) {
			case 11:
				arena.changeGameSettings(WeatherType.CLEAR);
				break;
			case 15:
				arena.changeGameSettings(WeatherType.DOWNFALL);
				break;
			}
		case CHESTS:
			break;
		default:
			break;
		}
		open(player);
	}
}
