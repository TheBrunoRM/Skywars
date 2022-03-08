package me.brunorm.skywars.menus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.events.SetupEvents;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;
import mrblobman.sounds.Sounds;

public class SetupMenu implements Listener {

	static String minPlayersName = "&e&lMin Players: &a&l%s";
	static String maxPlayersName = "&e&lMax Players: &a&l%s";
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

	public static HashMap<Player, Inventory> inventories = new HashMap<Player, Inventory>();
	public static HashMap<Player, Location> playerLocations = new HashMap<Player, Location>();
	public static HashMap<Player, Arena> currentArenas = new HashMap<Player, Arena>();
	File schematicsFolder = new File(Skywars.schematicsPath);

	static void OpenSchematicsMenu(Player player) {
		final File folder = new File(Skywars.get().getDataFolder() + "/schematics");
		final Inventory inventory = Bukkit.createInventory(null, 9 * 6, Messager.color("&aSchematic files"));
		inventories.put(player, inventory);

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
	}

	static void UpdateInventory(Inventory inventory, Player player) {
		final Arena currentArena = currentArenas.get(player);
		if (currentArena == null)
			return;
		final SkywarsMap currentMap = currentArena.getMap();

		final List<String> intLore = new ArrayList<String>();
		intLore.add(Messager.color("&eLeft-click to add"));
		intLore.add(Messager.color("&eRight-click to remove"));

		final ItemStack minPlayers = new ItemStack(XMaterial.SADDLE.parseItem());
		final ItemMeta minPlayersMeta = minPlayers.getItemMeta();
		minPlayersMeta.setDisplayName(Messager.colorFormat(minPlayersName, currentMap.getMinPlayers()));
		minPlayersMeta.setLore(intLore);
		minPlayers.setItemMeta(minPlayersMeta);
		inventory.setItem(10, minPlayers);

		final ItemStack maxPlayers = new ItemStack(XMaterial.SADDLE.parseItem());
		final ItemMeta maxPlayersMeta = minPlayers.getItemMeta();
		maxPlayersMeta.setDisplayName(Messager.colorFormat(maxPlayersName, currentMap.getMaxPlayers()));
		maxPlayersMeta.setLore(intLore);
		maxPlayers.setItemMeta(maxPlayersMeta);
		inventory.setItem(11, maxPlayers);

		final List<String> worldLore = new ArrayList<String>();
		worldLore.add(Messager.color("&eLeft-click to set"));
		worldLore.add(Messager.color("&eto your current world"));

		final ItemStack world = new ItemStack(XMaterial.SADDLE.parseItem());
		final ItemMeta worldMeta = minPlayers.getItemMeta();
		String currentWorldName = currentMap.getWorldName();
		if (currentWorldName == null)
			currentWorldName = "none";
		worldMeta.setDisplayName(Messager.colorFormat(worldName, currentWorldName));
		worldMeta.setLore(worldLore);
		world.setItemMeta(worldMeta);
		inventory.setItem(12, world);

		final List<String> positionLore = new ArrayList<String>();
		positionLore.add(Messager.color("&eLeft-click to set"));
		positionLore.add(Messager.color("&eto your current position"));

		final ItemStack position = new ItemStack(XMaterial.SADDLE.parseItem());
		final ItemMeta positionMeta = position.getItemMeta();
		positionMeta.setDisplayName(locationName(currentArena.getLocation()));
		positionMeta.setLore(positionLore);
		position.setItemMeta(positionMeta);
		inventory.setItem(13, position);

		final ItemStack schematic = new ItemStack(XMaterial.PAPER.parseItem());
		final ItemMeta schematicMeta = schematic.getItemMeta();
		String currentSchematic = currentMap.getSchematicFilename();
		if (currentSchematic == null)
			currentSchematic = "none";
		schematicMeta.setDisplayName(Messager.colorFormat(schematicName, currentSchematic));
		schematicMeta.setLore(null);
		schematic.setItemMeta(schematicMeta);
		inventory.setItem(14, schematic);

		final ItemStack status = new ItemStack(XMaterial.GLASS.parseMaterial());
		final ItemMeta statusMeta = status.getItemMeta();
		statusMeta.setDisplayName(Messager.colorFormat(statusName, "&6&lYES"));
		status.setItemMeta(statusMeta);
		inventory.setItem(15, status);

		final List<String> spawnLore = new ArrayList<String>();
		spawnLore.add(Messager.color("&eWhen you enter &bSpawn Setup Mode&e,"));
		spawnLore.add(Messager.color("&eyou can click blocks on the arena"));
		spawnLore.add(Messager.color("&eto set spawns easily."));
		if (currentMap.getSpawns().size() > 0) {
			spawnLore.add(Messager.color(""));
			spawnLore.add(Messager.color("&cThis will delete all current spawns."));
		}

		final ItemStack spawn = new ItemStack(XMaterial.BLAZE_ROD.parseItem());
		final ItemMeta spawnMeta = spawn.getItemMeta();
		spawnMeta.setDisplayName(Messager.color(spawnName));
		spawnMeta.setLore(spawnLore);
		spawn.setItemMeta(spawnMeta);
		inventory.setItem(16, spawn);

		final List<String> calculateSpawnsLore = new ArrayList<String>();
		calculateSpawnsLore.add(Messager.color("&cThis will override current spawns."));

		final ItemStack calculateSpawns = new ItemStack(XMaterial.BEACON.parseItem());
		final ItemMeta calculateSpawnsMeta = calculateSpawns.getItemMeta();
		calculateSpawnsMeta.setDisplayName(Messager.color(calculateSpawnsName));
		calculateSpawnsMeta.setLore(calculateSpawnsLore);
		calculateSpawns.setItemMeta(calculateSpawnsMeta);
		inventory.setItem(19, calculateSpawns);

		final List<String> pasteSchematicLore = new ArrayList<String>();
		pasteSchematicLore.add(Messager.color("&cThis will regenerate the map."));

		final ItemStack pasteSchematic = new ItemStack(XMaterial.WOODEN_AXE.parseItem());
		final ItemMeta pasteSchematicMeta = pasteSchematic.getItemMeta();
		pasteSchematicMeta.setDisplayName(Messager.color(pasteSchematicName));
		pasteSchematicMeta.setLore(pasteSchematicLore);
		pasteSchematic.setItemMeta(pasteSchematicMeta);
		inventory.setItem(20, pasteSchematic);

		final ItemStack regenerateCases = new ItemStack(XMaterial.GLASS.parseItem());
		final ItemMeta regenerateCasesMeta = regenerateCases.getItemMeta();
		regenerateCasesMeta.setDisplayName(Messager.color(regenerateCasesName));
		regenerateCases.setItemMeta(regenerateCasesMeta);
		inventory.setItem(18, regenerateCases);

		final ItemStack restart = new ItemStack(XMaterial.BARRIER.parseItem());
		final ItemMeta restartMeta = restart.getItemMeta();
		restartMeta.setDisplayName(Messager.color(clearName));
		restart.setItemMeta(restartMeta);
		inventory.setItem(21, restart);

		final ItemStack teleport = new ItemStack(XMaterial.COMPASS.parseItem());
		final ItemMeta teleportMeta = teleport.getItemMeta();
		teleportMeta.setDisplayName(Messager.color(teleportName));
		teleport.setItemMeta(teleportMeta);
		inventory.setItem(22, teleport);

		final ItemStack chests = new ItemStack(XMaterial.CHEST.parseItem());
		final ItemMeta chestsMeta = chests.getItemMeta();
		chestsMeta.setDisplayName(Messager.color(chestsName));
		chests.setItemMeta(chestsMeta);
		inventory.setItem(23, chests);
	}

	public static void OpenConfigurationMenu(Player player, SkywarsMap map) {
		currentArenas.put(player, Skywars.get().getArenaAndCreateIfNotFound(map));
		final Inventory inventory = Bukkit.createInventory(null, 9 * 3, Messager.color("&a&l" + map.getName()));
		inventories.put(player, inventory);

		UpdateInventory(inventory, player);

		player.openInventory(inventory);
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
		final Inventory inventory = inventories.get(player);
		if (event.getInventory().equals(inventory)) {
			event.setCancelled(true);
			final ItemStack clicked = event.getCurrentItem();
			if (clicked == null)
				return;
			if (clicked.getItemMeta() == null)
				return;
			final String name = clicked.getItemMeta().getDisplayName();

			Arena currentArena = currentArenas.get(player);
			final SkywarsMap currentMap = currentArena.getMap();

			if (name.equals(Messager.colorFormat(minPlayersName, currentMap.getMinPlayers()))) {
				int n = currentMap.getMinPlayers() + (event.getClick() == ClickType.LEFT ? 1 : -1);
				n = Math.min(Math.max(n, 0), currentMap.getMaxPlayers());
				currentMap.setMinPlayers(n);
			}
			if (name.equals(Messager.colorFormat(maxPlayersName, currentMap.getMaxPlayers()))) {
				int n = currentMap.getMaxPlayers() + (event.getClick() == ClickType.LEFT ? 1 : -1);
				n = Math.max(n, 0);
				currentMap.setMaxPlayers(n);
			}
			String currentWorld = currentMap.getWorldName();
			if (currentWorld == null)
				currentWorld = "none";
			if (name.equals(Messager.colorFormat(worldName, currentWorld))) {
				currentMap.setWorldName(player.getWorld().getName());
				Skywars.get().sendDebugMessage("world changed to " + currentMap.getWorldName());
			}
			if (name.equals(locationName(currentArena.getLocation()))) {
				final double x = Math.round(player.getLocation().getBlockX());
				final double y = Math.round(player.getLocation().getBlockY());
				final double z = Math.round(player.getLocation().getBlockZ());
				final World world = player.getWorld();
				final Location location = new Location(world, x, y, z);
				currentArena.setLocation(location);
				currentMap.setLocation(location);
				player.sendMessage(String.format("set location of %s to %s %s %s in world %s", currentMap.getName(),
						location.getX(), location.getY(), location.getZ(), location.getWorld().getName()));
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
				lore.add(Messager.color("to remove the last set spawn."));
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
				player.sendMessage("Spawns have been calculated and saved.");
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
				player.sendMessage(
						Messager.colorFormat("Regenerated cases for %s spawns", currentMap.getSpawns().size()));
				return;
			}
			if (name.equals(Messager.color(clearName))) {
				Skywars.get().clearArena(currentArena);
				currentArenas.remove(player);
				currentArena = null;
				player.sendMessage("Cleared");
				player.closeInventory();
			}
			if (name.equals(Messager.color(teleportName))) {
				final Location loc = currentArena.getLocation();
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
			String currentSchematic = currentMap.getSchematicFilename();
			if (currentSchematic == null)
				currentSchematic = "none";
			if (name.equals(Messager.colorFormat(schematicName, currentSchematic))) {
				if (this.schematicsFolder.listFiles() == null) {
					player.closeInventory();
					player.sendMessage("&c&lThere are no schematic files!");
					player.sendMessage("&e&lYou need to put &bschematics files &ein the &bschematics folder");
					return;
				} else
					OpenSchematicsMenu(player);
			}

			final String schematicName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
			for (final File schematicFile : this.schematicsFolder.listFiles()) {
				if (schematicFile.getName().equals(schematicName)) {
					if (schematicFile.getName().endsWith(".schem")) {
						player.sendMessage("&cThe plugin does not support .schem files yet.");
						return;
					}
					currentArena.clearBlocks();
					currentMap.setSchematic(schematicName);
					currentMap.loadSchematic();
					currentArena.pasteSchematic();
					player.sendMessage(
							Messager.colorFormat("&eSchematic set to &b%s", currentMap.getSchematicFilename()));
					player.sendMessage(Messager.color("&eSchematic pasted."));
				}
			}

			currentMap.saveParametersInConfig();
			currentMap.saveConfig();

			inventories.forEach((p, inv) -> {
				if (inv == inventory) {
					UpdateInventory(inv, p);
				}
			});
		}
	}

}
