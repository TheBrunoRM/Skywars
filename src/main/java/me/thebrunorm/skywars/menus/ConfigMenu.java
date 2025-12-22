// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.menus;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.managers.ArenaManager;
import me.thebrunorm.skywars.singletons.InventoryUtils;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.singletons.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ConfigMenu implements Listener {

	public final static HashMap<Player, Location> playerLocations = new HashMap<>();
	public final static HashMap<Player, Arena> currentArenas = new HashMap<>();
	final static String teamSizeName = "&e&lTeam Size: &a&l%s";
	final static String positionName = "&e&lPosition: &a&l%s";
	final static String worldFolderName = "&e&lWorld: &a&l%s";
	final static String calculateSpawnsName = "&6&lCalculate spawns";
	final static String regenerateCasesName = "&6&lRegenerate cases";
	final static String reloadWorld = "&6&lReload world";
	final static String saveWorld = "&b&lSave world";
	final static String clearName = "&c&lClear";
	final static String teleportName = "&6&lTeleport";
	final static String chestsName = "&6&lFill chests";
	final File worldsFolder = new File(Skywars.worldsPath);

	static void addItemToInventory(Inventory inv, Material mat, int slot, String name, String... loreLines) {
		final ItemStack item = new ItemStack(mat);
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(MessageUtils.color(name));
		final List<String> lore = new ArrayList<>();
		for (final String line : loreLines)
			lore.add(MessageUtils.color("&e" + line));
		meta.setLore(lore);
		item.setItemMeta(meta);
		inv.setItem(slot, item);
	}

	public static void OpenConfigurationMenu(Player player, SkywarsMap map) {
		currentArenas.put(player, ArenaManager.getArenaByMap(map, true));
		final Inventory inventory = Bukkit.createInventory(null, 9 * 3, MessageUtils.color("&a&l" + map.getName()));
		player.openInventory(inventory);
		PlayerInventoryManager.setMenu(player, MenuType.MAP_CONFIGURATION);
		UpdateInventory(player);
	}

	static void UpdateInventory(Player player) {
		final InventoryView openInv = player.getOpenInventory();
		if (openInv == null)
			return;
		final Inventory inventory = openInv.getTopInventory();
		if (inventory == null)
			return;
		final Arena currentArena = currentArenas.get(player);
		if (currentArena == null)
			return;
		final SkywarsMap currentMap = currentArena.getMap();

		InventoryUtils.addItem(inventory, XMaterial.SADDLE.parseMaterial(), 11,
				MessageUtils.color(teamSizeName, currentMap.getTeamSize()), "&eLeft-click to add",
				"&eRight-click to remove");

		String currentWorldFile = currentMap.getWorldName();
		if (currentWorldFile == null)
			currentWorldFile = "none";

		InventoryUtils.addItem(inventory, XMaterial.PAPER.parseMaterial(), 14,
				MessageUtils.color(worldFolderName, currentWorldFile));

		InventoryUtils.addItem(inventory, XMaterial.GLASS.parseMaterial(), 18, regenerateCasesName);

		InventoryUtils.addItem(inventory, XMaterial.BEACON.parseMaterial(), 19, calculateSpawnsName,
				"&eSet spawns based on beacons placed on the map",
				"&cThis will override current spawns.");

		InventoryUtils.addItem(inventory, XMaterial.WOODEN_AXE.parseMaterial(), 21, reloadWorld,
				"&cAll changes to the arena will be lost!");

		InventoryUtils.addItem(inventory, XMaterial.WRITABLE_BOOK.parseMaterial(), 22, saveWorld,
				"&eThe arena world will be saved as it is");

		InventoryUtils.addItem(inventory, XMaterial.BARRIER.parseMaterial(), 24, clearName);

		InventoryUtils.addItem(inventory, XMaterial.COMPASS.parseMaterial(), 25, teleportName);

		InventoryUtils.addItem(inventory, XMaterial.CHEST.parseMaterial(), 26, chestsName);

	}

	static String locationName(Location location) {
		if (location == null)
			return MessageUtils.color(positionName, "none");
		final String positionString = String.format("%s, %s, %s", (double) location.getBlockX(),
				(double) location.getBlockY(), (double) location.getBlockZ());
		return MessageUtils.color(positionName, positionString);
	}

	@EventHandler
	void onClick(InventoryClickEvent event) {
		final Player player = (Player) event.getWhoClicked();
		final MenuType currentMenu = PlayerInventoryManager.getCurrentMenu(player);
		if (currentMenu != MenuType.MAP_CONFIGURATION && currentMenu != MenuType.MAP_SCHEMATIC)
			return;
		event.setCancelled(true);
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null)
			return;
		final String name = clicked.getItemMeta().getDisplayName();

		Arena currentArena = currentArenas.get(player);
		if (currentArena == null)
			return;

		final SkywarsMap currentMap = currentArena.getMap();

		if (name.equals(MessageUtils.color(teamSizeName, currentMap.getTeamSize()))) {
			int n = currentMap.getTeamSize() + (event.getClick() == ClickType.LEFT ? 1:-1);
			n = Math.max(n, 0);
			currentMap.setTeamSize(n);
		}

		if (name.equals(MessageUtils.color(calculateSpawnsName))) {
			currentMap.calculateSpawns();
			player.sendMessage(MessageUtils.color("&aSuccessfully &bcalculated &aand &bsaved &6%s spawns&a.",
					currentMap.getSpawns().size()));
			if (currentMap.getSpawns().size() <= 0)
				player.sendMessage(MessageUtils.color("&cWarning: &7did you place beacons on the map?"));
		}

		if (name.equals(MessageUtils.color(regenerateCasesName))) {
			currentArena.resetCases();
			if (currentMap.getSpawns().size() <= 0)
				player.sendMessage(MessageUtils.color("&cWarning: &7no spawns to create cases for."));
			player.sendMessage(MessageUtils.color("Regenerated cases for %s spawns", currentMap.getSpawns().size()));
			return;
		}

		if (name.equals(MessageUtils.color(clearName))) {
			ArenaManager.removeArena(currentArena);
			currentArenas.remove(player);
			player.sendMessage("Cleared");
			player.closeInventory();
		}

		if (name.equals(MessageUtils.color(reloadWorld))) {
			ArenaManager.removeArena(currentArena);
			currentArenas.put(player, ArenaManager.getArenaByMap(currentMap, true));
			return;
		}

		if (name.equals(MessageUtils.color(saveWorld))) {
			Path worldsPath = Skywars.get().getDataFolder().toPath().resolve("worlds").toAbsolutePath().normalize();
			Path newFolder = worldsPath.resolve(currentMap.getName()).normalize();
			if (newFolder.toFile().exists()) {
				String backupWorldName = currentMap.getName() + "_old" + System.currentTimeMillis();
				Path oldWorldsPath = Skywars.get().getDataFolder().toPath().resolve("old_worlds").toAbsolutePath().normalize();
				if (!oldWorldsPath.toFile().exists())
					if (!oldWorldsPath.toFile().mkdirs())
						player.sendMessage("Could not create old_worlds directory: " + oldWorldsPath.toAbsolutePath());
				Path worldBackup = oldWorldsPath.resolve(backupWorldName).toAbsolutePath().normalize();
				try {
					Files.move(newFolder, worldBackup, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					player.sendMessage("Error moving world folder: " + e.getMessage());
					player.sendMessage("\nCould not move world\nFrom: "
							+ newFolder + "\nTo: " + worldBackup);
				}
			}

			World currentWorld = currentArena.getWorld();

			player.sendMessage("Teleporting any players inside the world outside of it...");
			for (final Player p : currentWorld.getPlayers())
				SkywarsUtils.teleportPlayerLobbyOrLastLocation(p, true);

			currentWorld.save();
			player.sendMessage("Saved the world for arena: " + currentMap.getName());

			/*
			if (!Bukkit.unloadWorld(currentWorld, true)) {
				player.sendMessage("Could not unload world :(");
				return;
			}
			player.sendMessage("Unloaded the world for arena: " + currentMap.getName());
			*/

			File worldFolder = currentWorld.getWorldFolder();
			Path worldPath = Paths.get(worldFolder.getAbsolutePath()).normalize();

			if (!worldFolder.isDirectory()) {
				player.sendMessage("The world folder does not exist or is not a directory: " + worldPath);
				return;
			}

			try {
				Files.copy(worldPath, newFolder, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				player.sendMessage("Error moving world folder: " + e.getMessage());
				player.sendMessage("\nCould not move world\nFrom: "
						+ worldPath + "\nTo: " + newFolder);
			}

			return;
		}

		if (name.equals(MessageUtils.color(teleportName))) {
			currentArena.goBackToCenter(player);
			player.setGameMode(GameMode.CREATIVE);
			return;
		}

		if (name.equals(MessageUtils.color(chestsName))) {
			currentArena.fillChests();
			player.sendMessage(MessageUtils.color("Filled %s chests for map &b%s",
					currentArena.getActiveChests().size(), currentArena.getMap().getName()));
			return;
		}

		String currentWorldName = currentMap.getWorldName();
		if (currentWorldName == null)
			currentWorldName = "none";

		if (name.equals(MessageUtils.color(worldFolderName, currentWorldName))) {
			if (this.worldsFolder.exists() && this.worldsFolder.listFiles().length <= 0) {
				player.closeInventory();
				player.sendMessage("&c&lThere are no world folders!");
				player.sendMessage("&e&lYou need to put &bschematics files &ein the &bschematics folder");
			} else
				OpenWorldsMenu(player);
			return;
		}

		final String worldFolderName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
		for (final File worldFolder : this.worldsFolder.listFiles()) {
			Skywars.get().sendDebugMessage("current file: " + worldFolder.getName());
			if (worldFolder.getName().equals(worldFolderName)) {
				currentMap.setWorldName(worldFolderName);
				player.sendMessage(MessageUtils.color("&eWorld set to &b%s", currentMap.getWorldName()));
				break;
			}
		}

		currentMap.saveParametersInConfig();
		currentMap.saveConfig();

		UpdateInventory(player);
	}

	static void OpenWorldsMenu(Player player) {
		final File folder = new File(Skywars.get().getDataFolder() + "/worlds");
		final Inventory inventory = Bukkit.createInventory(null, 9 * 6, MessageUtils.color("&aWorld folders"));

		int index = 10;
		for (final File worldFolder : Objects.requireNonNull(folder.listFiles())) {
			final List<String> lore = new ArrayList<>();

			boolean alreadyUsing = false;
			for (final SkywarsMap map : Skywars.get().getMapManager().getMaps()) {
				final String worldName = map.getWorldName();
				if (worldName != null && worldName.equals(worldFolder.getName())) {
					if (map == currentArenas.get(player).getMap()) {
						lore.add(MessageUtils.color("&6Current world folder", map.getName()));
					} else {
						lore.add(MessageUtils.color("&cWarning! %s already uses this world folder", map.getName()));
					}
					alreadyUsing = true;
					break;
				}
			}

			if (!alreadyUsing)
				lore.add(MessageUtils.color("&eClick to select this file"));

			final ItemStack item = new ItemStack(Objects.requireNonNull(XMaterial.PAPER.parseItem()));
			final ItemMeta meta = item.getItemMeta();

			meta.setDisplayName(MessageUtils.color("&a%s", worldFolder.getName()));
			meta.setLore(lore);
			item.setItemMeta(meta);
			inventory.setItem(index, item);
			index++;
		}

		player.openInventory(inventory);
		PlayerInventoryManager.setMenu(player, MenuType.MAP_SCHEMATIC);
	}

}
