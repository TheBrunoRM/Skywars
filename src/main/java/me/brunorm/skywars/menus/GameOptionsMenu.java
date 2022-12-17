package me.brunorm.skywars.menus;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import me.brunorm.skywars.structures.ChestType;
import me.brunorm.skywars.structures.TimeType;
import me.brunorm.skywars.structures.WeatherType;

public class GameOptionsMenu implements Listener {

	static HashMap<UUID, GameOptionType> currentMenus = new HashMap<UUID, GameOptionType>();

	public static void open(Player player) {
		open(player, GameOptionType.MAIN);
	}

	public static void open(Player player, GameOptionType menu) {
		Inventory inventory = null;
		currentMenus.put(player.getUniqueId(), menu);
		System.out.println("open game options menu: " + menu);
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
		player.openInventory(inventory);
		PlayerInventoryManager.setMenu(player, MenuType.GAME_OPTIONS);
	}

	@EventHandler
	void onClick(InventoryClickEvent event) {
		final Player player = (Player) event.getWhoClicked();
		if (PlayerInventoryManager.getCurrentMenu(player) != MenuType.GAME_OPTIONS)
			return;
		final GameOptionType currentMenu = currentMenus.get(player.getUniqueId());
		if (currentMenu == null)
			return;
		event.setCancelled(true);
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null)
			return;
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		switch (currentMenu) {
		case MAIN:
			switch (event.getSlot()) {
			case 10:
				open(player, GameOptionType.WEATHER);
				break;
			case 13:
				open(player, GameOptionType.TIME);
				break;
			case 16:
				open(player, GameOptionType.CHESTS);
				break;
			default:
				open(player, GameOptionType.MAIN);
			}
			return;
		case TIME:
			switch (event.getSlot()) {
			case 11: // day
				arena.voteTime(player, TimeType.DAY);
				break;
			case 15: // night
				arena.voteTime(player, TimeType.NIGHT);
				break;
			}
			break;
		case WEATHER:
			switch (event.getSlot()) {
			case 11:
				arena.voteWeather(player, WeatherType.CLEAR);
				break;
			case 15:
				arena.voteWeather(player, WeatherType.RAIN);
				break;
			}
			break;
		case CHESTS:
			switch (event.getSlot()) {
			case 11:
				arena.voteChests(player, ChestType.NORMAL);
				break;
			case 15:
				arena.voteChests(player, ChestType.OVERPOWERED);
				break;
			}
			break;
		default:
			break;
		}
		open(player);
	}
}
