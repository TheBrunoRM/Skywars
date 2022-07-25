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

public class GameSettingsMenu implements Listener {

	static HashMap<Player, Inventory> inventories = new HashMap<Player, Inventory>();
	static HashMap<UUID, SettingMenu> currentMenus = new HashMap<UUID, SettingMenu>();

	public static void open(Player player) {
		open(player, SettingMenu.MAIN);
	}

	public static void open(Player player, SettingMenu menu) {
		Inventory inventory = null;
		currentMenus.put(player.getUniqueId(), menu);
		System.out.println("opening setting menu: " + menu);
		System.out.println("menu: " + currentMenus.get(player.getUniqueId()));
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
		System.out.println("test");
		final Inventory inventory = inventories.get(player);
		System.out.println("test1");
		if (!event.getInventory().equals(inventory))
			return;
		System.out.println("test2");
		event.setCancelled(true);
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null)
			return;
		System.out.println("test3");
		final SettingMenu currentMenu = currentMenus.get(player.getUniqueId());
		System.out.println("current menu: " + currentMenu);
		if (currentMenu == null)
			return;
		System.out.println("test4");
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		System.out.println("test5");
		switch (currentMenu) {
		case MAIN:
			switch (event.getSlot()) {
			case 10:
				open(player, SettingMenu.WEATHER);
				break;
			case 13:
				open(player, SettingMenu.TIME);
				break;
			case 16:
				open(player, SettingMenu.CHESTS);
				break;
			default:
				open(player, SettingMenu.MAIN);
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
