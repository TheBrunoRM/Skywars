package me.brunorm.skywars.menus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.InventoryUtils;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.events.SetupEvents;
import me.brunorm.skywars.managers.ArenaManager;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;
import mrblobman.sounds.Sounds;

public class ConfigMenu implements Listener {

	static String teamSizeName = "&e&lTeam Size: &a&l%s";
	static String worldName = "&e&lWorld: &a&l%s";
	static String positionName = "&e&lPosition: &a&l%s";
	static String spawnName = "&e&lSpawn Setup";
	static String schematicName = "&e&lSchematic: &a&l%s";
	static String statusName = "&e&lStatus: %s";
	static String calculateSpawnsName = "&6&lCalculate spawns";
	static String regenerateCasesName = "&6&lRegenerate cases";
	static String pasteSchematicName = "&6&lPaste schematic";
	static String clearName = "&c&lClear";
	static String teleportName = "&6&lTeleport";
	static String chestsName = "&6&lFill chests";

	public static HashMap<Player, Location> playerLocations = new HashMap<Player, Location>();
	public static HashMap<Player, Arena> currentArenas = new HashMap<Player, Arena>();
	File schematicsFolder = new File(Skywars.schematicsPath);

	static void OpenSchematicsMenu(Player player) {
		final File folder = new File(Skywars.get().getDataFolder() + "/schematics");
		final Inventory inventory = Bukkit.createInventory(null, 9 * 6, Messager.color("&aSchematic files"));

		int index = 10;
		for (final File schematicFile : folder.listFiles()) {
			final List<String> lore = new ArrayList<String>();
			lore.clear();

			boolean alreadyUsing = false;
			for (final SkywarsMap map : Skywars.get().getMaps()) {
				final String mapSchematic = map.getSchematicFilename();
				if (mapSchematic != null && mapSchematic.equals(schematicFile.getName())) {
					if (map == currentArenas.get(player).getMap()) {
						lore.add(Messager.colorFormat("&6Current schematic file", map.getName()));
					} else {
						lore.add(Messager.colorFormat("&cWarning! %s already uses this file", map.getName()));
					}
					alreadyUsing = true;
					break;
				}
			}

			if (schematicFile.getName().endsWith(".schem"))
				lore.add(Messager.color("&cThe plugin does not support .schem files yet"));
			else if (!alreadyUsing)
				lore.add(Messager.color("&eClick to select this file"));

			final ItemStack item = new ItemStack(XMaterial.PAPER.parseItem());
			final ItemMeta meta = item.getItemMeta();

			meta.setDisplayName(Messager.colorFormat("&a%s", schematicFile.getName()));
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
		final List<String> lore = new ArrayList<String>();
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
				Messager.colorFormat(teamSizeName, currentMap.getTeamSize()), "&eLeft-click to add",
				"&eRight-click to remove");

		String currentWorldName = currentMap.getWorldName();
		if (currentWorldName == null)
			currentWorldName = "none";

		InventoryUtils.addItem(inventory, XMaterial.SADDLE.parseMaterial(), 12,
				Messager.colorFormat(worldName, currentWorldName), "&eLeft-click to set", "&eto your current world", "",
				"&eRight-click to unset");

		InventoryUtils.addItem(inventory, XMaterial.SADDLE.parseMaterial(), 13,
				locationName(currentArena.getLocation()), "&eLeft-click to set", "&eto your current position", "",
				"&eRight-click to unset");

		String currentSchematic = currentMap.getSchematicFilename();
		if (currentSchematic == null)
			currentSchematic = "none";

		InventoryUtils.addItem(inventory, XMaterial.PAPER.parseMaterial(), 14,
				Messager.colorFormat(schematicName, currentSchematic));

		InventoryUtils.addItem(inventory, XMaterial.GLASS.parseMaterial(), 15,
				Messager.colorFormat(statusName, "&6&lYES"));

		final List<String> spawnLore = new ArrayList<String>();
		spawnLore.add(Messager.color("&eWhen you enter &bSpawn Setup Mode&e,"));
		spawnLore.add(Messager.color("&eyou can click blocks on the arena"));
		spawnLore.add(Messager.color("&eto set spawns easily."));
		if (currentMap.getSpawns().size() > 0) {
			spawnLore.add(Messager.color(""));
			spawnLore.add(Messager.color("&cThis will delete all current spawns."));
		}

		InventoryUtils.addItem(inventory, XMaterial.BLAZE_ROD.parseMaterial(), 16, spawnName,
				(String[]) spawnLore.toArray());

		InventoryUtils.addItem(inventory, XMaterial.BEACON.parseMaterial(), 19, calculateSpawnsName,
				"&cThis will override current spawns.");

		InventoryUtils.addItem(inventory, XMaterial.WOODEN_AXE.parseMaterial(), 20, pasteSchematicName,
				"&cThis will regenerate the map.");

		InventoryUtils.addItem(inventory, XMaterial.GLASS.parseMaterial(), 18, regenerateCasesName);

		InventoryUtils.addItem(inventory, XMaterial.BARRIER.parseMaterial(), 21, clearName);

		InventoryUtils.addItem(inventory, XMaterial.COMPASS.parseMaterial(), 22, teleportName);

		InventoryUtils.addItem(inventory, XMaterial.CHEST.parseMaterial(), 23, chestsName);

	}

	public static void OpenConfigurationMenu(Player player, SkywarsMap map) {
		currentArenas.put(player, ArenaManager.getArenaAndCreateIfNotFound(map));
		final Inventory inventory = Bukkit.createInventory(null, 9 * 3, Messager.color("&a&l" + map.getName()));
		player.openInventory(inventory);
		PlayerInventoryManager.setMenu(player, MenuType.MAP_CONFIGURATION);
		UpdateInventory(player);
	}

	static String locationName(Location location) {
		if (location == null)
			return Messager.colorFormat(positionName, "none");
		final String coords = String.format("%s, %s, %s", Math.floor(location.getBlockX()),
				Math.floor(location.getBlockY()), Math.floor(location.getBlockZ()));
		return Messager.colorFormat(positionName, coords);
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

		if (name.equals(Messager.colorFormat(teamSizeName, currentMap.getTeamSize()))) {
			int n = currentMap.getTeamSize() + (event.getClick() == ClickType.LEFT ? 1 : -1);
			n = Math.max(n, 0);
			currentMap.setTeamSize(n);
		}

		String currentWorldName = currentMap.getWorldName();
		if (currentWorldName == null)
			currentWorldName = "none";

		if (name.equals(Messager.colorFormat(worldName, currentWorldName))) {
			if (event.getClick() == ClickType.RIGHT) {
				currentMap.setWorldName(null);
				player.sendMessage("world unset! (left click to set)");
			} else {
				currentMap.setWorldName(player.getWorld().getName());
				player.sendMessage("world set to " + currentMap.getWorldName());
			}
		}

		if (name.equals(locationName(currentArena.getLocation()))) {
			if (event.getClick() == ClickType.RIGHT) {
				currentArena.setLocation(null);
				currentMap.setLocation(null);
				player.sendMessage("location unset! (left click to set)");
			} else {
				final double x = player.getLocation().getBlockX();
				final double y = player.getLocation().getBlockY();
				final double z = player.getLocation().getBlockZ();
				final World world = player.getWorld();
				final Location location = new Location(world, x, y, z);
				currentArena.setLocation(location);
				currentMap.setLocation(location);
				player.sendMessage(String.format("set location of %s to %s %s %s in world %s", currentMap.getName(),
						location.getX(), location.getY(), location.getZ(), location.getWorld().getName()));
			}
		}

		if (name.equals(Messager.color(spawnName))) {
			if (currentMap.getLocation() == null) {
				player.sendMessage("Location not set");
				return;
			}
			final Arena arena = currentArenas.get(player);
			final ItemStack item = new ItemStack(XMaterial.BLAZE_ROD.parseItem());
			final ItemMeta meta = item.getItemMeta();
			final List<String> lore = new ArrayList<String>();
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
			player.teleport(arena.getLocation().clone().add(new Vector(0, 5, 0)));
			player.setVelocity(new Vector(0, 1f, 0));

			player.setAllowFlight(true);
			player.setFlying(true);

			Skywars.get().NMS().sendTitle(player, "&a&lENABLED", "&eSpawn edit mode");
			player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), 3, 2);

			if (currentMap.getSpawns().size() > 0)
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
			player.sendMessage(Messager.colorFormat("&aSuccessfully &bcalculated &aand &bsaved &6%s spawns&a.",
					currentMap.getSpawns().size()));
			if (currentMap.getSpawns().size() <= 0)
				player.sendMessage(Messager.color("&cWarning: &7did you place beacons on the map?"));
		}

		if (name.equals(Messager.color(pasteSchematicName))) {
			currentArena.pasteSchematic();
			player.sendMessage("Pasted schematic.");
			return;
		}

		if (name.equals(Messager.color(regenerateCasesName))) {
			currentArena.resetCases();
			if (currentMap.getSpawns().size() <= 0)
				player.sendMessage(Messager.color("&cWarning: &7no spawns to create cases for."));
			player.sendMessage(Messager.colorFormat("Regenerated cases for %s spawns", currentMap.getSpawns().size()));
			return;
		}

		if (name.equals(Messager.color(teleportName))) {
			Location loc = currentArena.getLocation();
			if (loc == null) {
				if (currentMap.getWorldName() == null) {
					player.sendMessage("World not set");
					return;
				}
				final World world = currentArena.getWorldAndLoadIfItIsNotLoaded();
				if (world != null)
					loc = world.getSpawnLocation();
			}
			if (loc == null)
				player.sendMessage("Location not set");
			else {
				player.teleport(loc);
				player.sendMessage("Teleported");
			}
			return;
		}

		if (name.equals(Messager.color(chestsName))) {
			currentArena.calculateAndFillChests();
			player.sendMessage("Chests filled");
			return;
		}

		if (name.equals(Messager.color(clearName))) {
			ArenaManager.removeArenaFromListAndDeleteArena(currentArena);
			currentArenas.remove(player);
			currentArena = null;
			player.sendMessage("Cleared");
			player.closeInventory();
		}

		String currentSchematic = currentMap.getSchematicFilename();
		if (currentSchematic == null)
			currentSchematic = "none";
		if (name.equals(Messager.colorFormat(schematicName, currentSchematic))) {
			if (this.schematicsFolder.listFiles() == null) {
				player.closeInventory();
				player.sendMessage("&c&lThere are no schematic files!");
				player.sendMessage("&e&lYou need to put &bschematics files &ein the &bschematics folder");
			} else
				OpenSchematicsMenu(player);
			return;
		}

		boolean pasted = false;
		final String schematicName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
		for (final File schematicFile : this.schematicsFolder.listFiles()) {
			Skywars.get().sendDebugMessage("current file: " + schematicFile.getName());
			if (schematicFile.getName().equals(schematicName)) {
				currentArena.clearBlocks();
				currentMap.setSchematicFilename(schematicName);
				currentMap.loadSchematic();
				currentArena.pasteSchematic();
				player.sendMessage(Messager.colorFormat("&eSchematic set to &b%s", currentMap.getSchematicFilename()));
				player.sendMessage(Messager.color("&eSchematic pasted."));
				pasted = true;
				break;
			}
		}
		if (pasted)
			return;

		currentMap.saveParametersInConfig();
		currentMap.saveConfig();

		UpdateInventory(player);
	}

}
