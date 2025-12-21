/* (C) 2021 Bruno */
package me.thebrunorm.skywars.menus;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.InventoryUtils;
import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.ChestType;
import me.thebrunorm.skywars.structures.TimeType;
import me.thebrunorm.skywars.structures.WeatherType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class GameOptionsMenu implements Listener {

	final static HashMap<UUID, GameOptionType> currentMenus = new HashMap<>();

	public static void open(Player player) {
		open(player, GameOptionType.MAIN);
	}

	public static void open(Player player, GameOptionType menu) {
		Inventory inventory = null;
		currentMenus.put(player.getUniqueId(), menu);
		Skywars.get().sendDebugMessage(Messager.getMessage("GAME_OPTIONS_MENU_OPEN_DEBUG", player.getName(), menu), player.getName(), menu);
		switch (menu) {
		case MAIN:
			inventory = Bukkit.createInventory(null, 9 * 3, Messager.getMessage("GAME_OPTION_MENU_TITLE"));

			InventoryUtils.addItem(inventory, XMaterial.NETHER_STAR.parseItem(), 10, "&a" + Messager.getMessage("GAME_OPTION_VOTE_WEATHER"));
			InventoryUtils.addItem(inventory, XMaterial.CLOCK.parseItem(), 13, "&a" + Messager.getMessage("GAME_OPTION_VOTE_TIME"));
			InventoryUtils.addItem(inventory, XMaterial.CHEST.parseItem(), 16, "&a" + Messager.getMessage("GAME_OPTION_VOTE_CHESTS"));

			break;
		case WEATHER:
			inventory = Bukkit.createInventory(null, 9 * 3, Messager.getMessage("GAME_OPTION_WEATHER_SETTINGS_TITLE"));

			InventoryUtils.addItem(inventory, XMaterial.SUNFLOWER.parseItem(), 11, "&a" + Messager.getMessage("weather.CLEAR"));
			InventoryUtils.addItem(inventory, XMaterial.ENDER_PEARL.parseItem(), 15, "&a" + Messager.getMessage("weather.RAIN"));

			break;
		case CHESTS:
			inventory = Bukkit.createInventory(null, 9 * 3, Messager.getMessage("GAME_OPTION_CHESTS_SETTINGS_TITLE"));

			InventoryUtils.addItem(inventory, XMaterial.CHEST.parseItem(), 11, "&a" + Messager.getMessage("chests.NORMAL"));
			InventoryUtils.addItem(inventory, XMaterial.ENDER_CHEST.parseItem(), 15, "&c" + Messager.getMessage("chests.OVERPOWERED"));

			break;
		case TIME:
			inventory = Bukkit.createInventory(null, 9 * 3, Messager.getMessage("GAME_OPTION_TIME_SETTINGS_TITLE"));

			InventoryUtils.addItem(inventory, XMaterial.SUNFLOWER.parseItem(), 11, "&a" + Messager.getMessage("time.DAY"));
			InventoryUtils.addItem(inventory, XMaterial.ENDER_PEARL.parseItem(), 15, "&a" + Messager.getMessage("time.NIGHT"));

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
