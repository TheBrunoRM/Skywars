/* (C) 2021 Bruno */
package me.thebrunorm.skywars.menus;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.ConfigurationUtils;
import me.thebrunorm.skywars.InventoryUtils;
import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.events.SetupEvents;
import me.thebrunorm.skywars.managers.ArenaManager;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;
import mrblobman.sounds.Sounds;
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
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ConfigMenu implements Listener {

	public final static HashMap<Player, Location> playerLocations = new HashMap<>();
	public final static HashMap<Player, Arena> currentArenas = new HashMap<>();
	final static String teamSizeName = "&e&lTeam Size: &a&l%s";
	final static String positionName = "&e&lPosition: &a&l%s";
	final static String spawnName = "&e&lSpawn Setup";
	final static String worldFolderName = "&e&lWorld: &a&l%s";
	final static String statusName = "&e&lStatus: %s";
	final static String calculateSpawnsName = "&6&lCalculate spawns";
	final static String regenerateCasesName = "&6&lRegenerate cases";
	final static String pasteSchematicName = "&6&lPaste schematic";
	final static String clearName = "&c&lClear";
	final static String teleportName = "&6&lTeleport";
	final static String chestsName = "&6&lFill chests";
	final File worldsFolder = new File(Skywars.worldsPath);

	static void OpenWorldsMenu(Player player) {
		final File folder = new File(Skywars.get().getDataFolder() + "/worlds");
		final Inventory inventory = Bukkit.createInventory(null, 9 * 6, Messager.color("&aWorld folders"));

		int index = 10;
		for (final File worldFolder : Objects.requireNonNull(folder.listFiles())) {
			final List<String> lore = new ArrayList<>();

			boolean alreadyUsing = false;
			for (final SkywarsMap map : Skywars.get().getMapManager().getMaps()) {
				final String worldName = map.getWorldName();
				if (worldName != null && worldName.equals(worldFolder.getName())) {
					if (map == currentArenas.get(player).getMap()) {
						lore.add(Messager.color("&6Current world folder", map.getName()));
					} else {
						lore.add(Messager.color("&cWarning! %s already uses this world folder", map.getName()));
					}
					alreadyUsing = true;
					break;
				}
			}

			if (!alreadyUsing)
				lore.add(Messager.color("&eClick to select this file"));

			final ItemStack item = new ItemStack(Objects.requireNonNull(XMaterial.PAPER.parseItem()));
			final ItemMeta meta = item.getItemMeta();

			meta.setDisplayName(Messager.color("&a%s", worldFolder.getName()));
			meta.setLore(lore);
			item.setItemMeta(meta);
			inventory.setItem(index, item);
			index++;
		}

		player.openInventory(inventory);
		PlayerInventoryManager.setMenu(player, MenuType.MAP_SCHEMATIC);
	}

	static void addItemToInventory(Inventory inv, Material mat, int slot, String name, String... loreLines) {
		final ItemStack item = new ItemStack(mat);
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Messager.color(name));
		final List<String> lore = new ArrayList<>();
		for (final String line : loreLines)
			lore.add(Messager.color("&e" + line));
		meta.setLore(lore);
		item.setItemMeta(meta);
		inv.setItem(slot, item);
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
				Messager.color(teamSizeName, currentMap.getTeamSize()), "&eLeft-click to add",
				"&eRight-click to remove");

		String currentWorldFile = currentMap.getWorldName();
		if (currentWorldFile == null)
			currentWorldFile = "none";

		InventoryUtils.addItem(inventory, XMaterial.PAPER.parseMaterial(), 14,
				Messager.color(worldFolderName, currentWorldFile));

		InventoryUtils.addItem(inventory, XMaterial.GLASS.parseMaterial(), 15,
				Messager.color(statusName, "&6&lYES"));

		final List<String> spawnLore = new ArrayList<>();
		spawnLore.add(Messager.color("&eWhen you enter &bSpawn Setup Mode&e,"));
		spawnLore.add(Messager.color("&eyou can click blocks on the arena"));
		spawnLore.add(Messager.color("&eto set spawns easily."));
		if (!currentMap.getSpawns().isEmpty()) {
			spawnLore.add(Messager.color(""));
			spawnLore.add(Messager.color("&cThis will delete all current spawns."));
		}

		InventoryUtils.addItem(inventory, XMaterial.BLAZE_ROD.parseMaterial(), 16, spawnName,
				spawnLore.toArray(new String[0]));

		InventoryUtils.addItem(inventory, XMaterial.GLASS.parseMaterial(), 18, regenerateCasesName);

		InventoryUtils.addItem(inventory, XMaterial.BEACON.parseMaterial(), 19, calculateSpawnsName,
				"&cThis will override current spawns.");

		InventoryUtils.addItem(inventory, XMaterial.WOODEN_AXE.parseMaterial(), 20, pasteSchematicName,
				"&cThis will regenerate the map.");

		InventoryUtils.addItem(inventory, XMaterial.BARRIER.parseMaterial(), 21, clearName);

		InventoryUtils.addItem(inventory, XMaterial.COMPASS.parseMaterial(), 22, teleportName);

		InventoryUtils.addItem(inventory, XMaterial.CHEST.parseMaterial(), 23, chestsName);

	}

	public static void OpenConfigurationMenu(Player player, SkywarsMap map) {
		currentArenas.put(player, ArenaManager.getArenaByMap(map, true));
		final Inventory inventory = Bukkit.createInventory(null, 9 * 3, Messager.color("&a&l" + map.getName()));
		player.openInventory(inventory);
		PlayerInventoryManager.setMenu(player, MenuType.MAP_CONFIGURATION);
		UpdateInventory(player);
	}

	static String locationName(Location location) {
		if (location == null)
			return Messager.color(positionName, "none");
		final String positionString = String.format("%s, %s, %s", (double) location.getBlockX(),
			(double) location.getBlockY(), (double) location.getBlockZ());
		return Messager.color(positionName, positionString);
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

		if (name.equals(Messager.color(teamSizeName, currentMap.getTeamSize()))) {
			int n = currentMap.getTeamSize() + (event.getClick() == ClickType.LEFT ? 1 : -1);
			n = Math.max(n, 0);
			currentMap.setTeamSize(n);
		}

		String currentWorldName = currentMap.getWorldName();
		if (currentWorldName == null)
			currentWorldName = "none";

		if (name.equals(Messager.color(spawnName))) {
			final Arena arena = currentArenas.get(player);
			final ItemStack item = new ItemStack(Objects.requireNonNull(XMaterial.BLAZE_ROD.parseItem()));
			final ItemMeta meta = item.getItemMeta();
			final List<String> lore = new ArrayList<>();
			lore.add(Messager.color("&eClick the blocks that"));
			lore.add(Messager.color("&eyou want to add spawns for."));
			lore.add(Messager.color("&eYou can also rightclick"));
			lore.add(Messager.color("&eto remove the last set spawn."));
			meta.setDisplayName(Messager.color("&eSpawn Configurator"));
			meta.setLore(lore);
			item.setItemMeta(meta);

			SetupEvents.item = item;
			player.getInventory().setItem(player.getInventory().getHeldItemSlot(), SetupEvents.item);
			player.closeInventory();

			playerLocations.put(player, player.getLocation());
			player.teleport(arena.getCenterBlock().toLocation(arena.getWorld()).clone().add(new Vector(0, 5, 0)));
			player.setVelocity(new Vector(0, 1f, 0));

			player.setAllowFlight(true);
			player.setFlying(true);

			Skywars.get().NMS().sendTitle(player, "&a&lENABLED", "&eSpawn edit mode");
			player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), 3, 2);

			if (!currentMap.getSpawns().isEmpty())
				player.sendMessage(Messager.color("&6Old arena spawns deleted."));
			currentMap.getSpawns().clear();

			player.sendMessage(Messager.color("&eYou are now in &a&lspawn edit mode"));
			player.sendMessage(Messager.color("&eUse the &6&lblaze rod &eto &b&lset and remove spawns"));
			player.sendMessage(Messager.color("&eYou can &a&lright-click &ea block to &a&ladd an spawn"));
			player.sendMessage(
					Messager.color("&eYou can &c&lright-click &ea block to &c&lremove &4&lthe last set spawn"));
			player.sendMessage(Messager.color("&e&lTo exit, &b&ldrop the blaze rod"));
			return;
		}

		if (name.equals(Messager.color(calculateSpawnsName))) {
			currentMap.calculateSpawns();
			player.sendMessage(Messager.color("&aSuccessfully &bcalculated &aand &bsaved &6%s spawns&a.",
					currentMap.getSpawns().size()));
			if (currentMap.getSpawns().size() <= 0)
				player.sendMessage(Messager.color("&cWarning: &7did you place beacons on the map?"));
		}

		if (name.equals(Messager.color(regenerateCasesName))) {
			currentArena.resetCases();
			if (currentMap.getSpawns().size() <= 0)
				player.sendMessage(Messager.color("&cWarning: &7no spawns to create cases for."));
			player.sendMessage(Messager.color("Regenerated cases for %s spawns", currentMap.getSpawns().size()));
			return;
		}

		final World arenaWorld = currentArena.getWorld();

		if (name.equals(Messager.color(teleportName))) {
			if (arenaWorld == null) {
				player.sendMessage("world not set");
				return;
			}
			Location loc = ConfigurationUtils.getLocationConfig(arenaWorld,
					currentArena.getMap().getConfig().getConfigurationSection("center"));
			if (loc == null)
				loc = currentArena.getWorld().getSpawnLocation();
			if (loc == null)
				player.sendMessage("Location not set");
			else {
				player.teleport(loc);
				player.sendMessage("Teleported");
			}
			return;
		}

		if (name.equals(Messager.color(chestsName))) {
			currentArena.fillChests();
			player.sendMessage(Messager.color("Filled %s chests for map &b%s",
				currentArena.getActiveChests().size(), currentArena.getMap().getName()));
			return;
		}

		if (name.equals(Messager.color(clearName))) {
			ArenaManager.removeArena(currentArena);
			currentArenas.remove(player);
			currentArena = null;
			player.sendMessage("Cleared");
			player.closeInventory();
		}

		if (name.equals(Messager.color(worldFolderName, currentWorldName))) {
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
				player.sendMessage(Messager.color("&eWorld set to &b%s", currentMap.getWorldName()));
				break;
			}
		}

		currentMap.saveParametersInConfig();
		currentMap.saveConfig();

		UpdateInventory(player);
	}

}
