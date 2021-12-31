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
import me.brunorm.skywars.events.MapSpawnSetup;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;
import mrblobman.sounds.Sounds;

public class MapSetupMenu implements Listener {

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
	static String restartName = "&c&lRestart";
	static String teleportName = "&6&lTeleport";
	
	public static HashMap<Player, Inventory> inventories = new HashMap<Player, Inventory>();
	public static HashMap<Player, Location> playerLocations = new HashMap<Player, Location>();
	public static HashMap<Player, Arena> currentArenas = new HashMap<Player, Arena>();
	File schematicsFolder = new File(Skywars.schematicsPath);
	
	static void OpenSchematicsMenu(Player player) {
		File folder = new File(Skywars.get().getDataFolder() + "/schematics");
		Inventory inventory = Bukkit.createInventory(null, 9 * 6, Messager.color("&aSchematic files"));
		inventories.put(player, inventory);
		
		int index = 10;
		for(File schematicFile : folder.listFiles()) {
			List<String> lore = new ArrayList<String>();
			lore.clear();
			
			boolean alreadyUsing = false;;
			for(SkywarsMap map : Skywars.get().getMaps()) {
				String mapSchematic = map.getSchematicFilename();
				if(mapSchematic != null && mapSchematic.equals(schematicFile.getName())) {
					if(map == currentArenas.get(player).getMap()) {
						lore.add(Messager.colorFormat("&6Current schematic file", map.getName()));
					} else {
						lore.add(Messager.colorFormat("&cWarning! %s already uses this file", map.getName()));
					}
					alreadyUsing = true;
					break;
				}
			}
			
			if(!alreadyUsing)
				lore.add(Messager.color("&eClick to select this file"));

			ItemStack item = new ItemStack(XMaterial.PAPER.parseItem());
			ItemMeta meta = item.getItemMeta();
			
			meta.setDisplayName(Messager.colorFormat("&a%s", schematicFile.getName()));
			meta.setLore(lore);
			item.setItemMeta(meta);
			inventory.setItem(index, item);
			index++;
		}

		player.openInventory(inventory);
	}
	
	static void UpdateInventory(Inventory inventory, Player player) {
		Arena currentArena = currentArenas.get(player);
		SkywarsMap currentMap = currentArena.getMap();
		
		List<String> intLore = new ArrayList<String>();
		intLore.add(Messager.color("&eLeft-click to add"));
		intLore.add(Messager.color("&eRight-click to remove"));

		ItemStack minPlayers = new ItemStack(XMaterial.SADDLE.parseItem());
		ItemMeta minPlayersMeta = minPlayers.getItemMeta();
		minPlayersMeta.setDisplayName(Messager.colorFormat(minPlayersName, currentMap.getMinPlayers()));
		minPlayersMeta.setLore(intLore);
		minPlayers.setItemMeta(minPlayersMeta);
		inventory.setItem(10, minPlayers);

		ItemStack maxPlayers = new ItemStack(XMaterial.SADDLE.parseItem());
		ItemMeta maxPlayersMeta = minPlayers.getItemMeta();
		maxPlayersMeta.setDisplayName(Messager.colorFormat(maxPlayersName, currentMap.getMaxPlayers()));
		maxPlayersMeta.setLore(intLore);
		maxPlayers.setItemMeta(maxPlayersMeta);
		inventory.setItem(11, maxPlayers);
		
		List<String> worldLore = new ArrayList<String>();
		worldLore.add(Messager.color("&eLeft-click to set"));
		worldLore.add(Messager.color("&eto your current world"));

		ItemStack world = new ItemStack(XMaterial.SADDLE.parseItem());
		ItemMeta worldMeta = minPlayers.getItemMeta();
		String currentWorld = currentArena.getWorld().getName();
		if(currentWorld == null) currentWorld = "none";
		worldMeta.setDisplayName(Messager.colorFormat(worldName, currentWorld));
		worldMeta.setLore(worldLore);
		world.setItemMeta(worldMeta);
		inventory.setItem(12, world);
		
		List<String> positionLore = new ArrayList<String>();
		positionLore.add(Messager.color("&eLeft-click to set"));
		positionLore.add(Messager.color("&eto your current position"));

		ItemStack position = new ItemStack(XMaterial.SADDLE.parseItem());
		ItemMeta positionMeta = position.getItemMeta();
		positionMeta.setDisplayName(locationName(currentArena.getLocation()));
		positionMeta.setLore(positionLore);
		position.setItemMeta(positionMeta);
		inventory.setItem(13, position);
		
		ItemStack schematic = new ItemStack(XMaterial.PAPER.parseItem());
		ItemMeta schematicMeta = schematic.getItemMeta();
		String currentSchematic = currentMap.getSchematicFilename();
		if(currentSchematic == null) currentSchematic = "none";
		schematicMeta.setDisplayName(Messager.colorFormat(schematicName, currentSchematic));
		schematicMeta.setLore(null);
		schematic.setItemMeta(schematicMeta);
		inventory.setItem(14, schematic);
		
		ItemStack status = new ItemStack(XMaterial.GLASS.parseMaterial());
		ItemMeta statusMeta = status.getItemMeta();
		statusMeta.setDisplayName(Messager.colorFormat(statusName, "&6&lYES"));
		status.setItemMeta(statusMeta);
		inventory.setItem(15, status);

		List<String> spawnLore = new ArrayList<String>();
		spawnLore.add(Messager.color("&eWhen you enter &bSpawn Setup Mode&e,"));
		spawnLore.add(Messager.color("&eyou can click blocks on the arena"));
		spawnLore.add(Messager.color("&eto set the arena's spawns."));

		ItemStack spawn = new ItemStack(XMaterial.BLAZE_ROD.parseItem());
		ItemMeta spawnMeta = spawn.getItemMeta();
		spawnMeta.setDisplayName(Messager.color(spawnName));
		spawnMeta.setLore(spawnLore);
		spawn.setItemMeta(spawnMeta);
		inventory.setItem(16, spawn);

		List<String> calculateSpawnsLore = new ArrayList<String>();
		calculateSpawnsLore.add(Messager.color("&cThis will override current spawns."));
		
		ItemStack calculateSpawns = new ItemStack(XMaterial.BEACON.parseItem());
		ItemMeta calculateSpawnsMeta = calculateSpawns.getItemMeta();
		calculateSpawnsMeta.setDisplayName(Messager.color(calculateSpawnsName));
		calculateSpawnsMeta.setLore(calculateSpawnsLore);
		calculateSpawns.setItemMeta(calculateSpawnsMeta);
		inventory.setItem(19, calculateSpawns);
		
		List<String> pasteSchematicLore = new ArrayList<String>();
		pasteSchematicLore.add(Messager.color("&cThis will regenerate the map."));
		
		ItemStack pasteSchematic = new ItemStack(XMaterial.WOODEN_AXE.parseItem());
		ItemMeta pasteSchematicMeta = pasteSchematic.getItemMeta();
		pasteSchematicMeta.setDisplayName(Messager.color(pasteSchematicName));
		pasteSchematicMeta.setLore(pasteSchematicLore);
		pasteSchematic.setItemMeta(pasteSchematicMeta);
		inventory.setItem(20, pasteSchematic);
		
		ItemStack regenerateCases = new ItemStack(XMaterial.GLASS.parseItem());
		ItemMeta regenerateCasesMeta = regenerateCases.getItemMeta();
		regenerateCasesMeta.setDisplayName(Messager.color(regenerateCasesName));
		regenerateCases.setItemMeta(regenerateCasesMeta);
		inventory.setItem(18, regenerateCases);
		
		ItemStack restart = new ItemStack(XMaterial.BARRIER.parseItem());
		ItemMeta restartMeta = restart.getItemMeta();
		restartMeta.setDisplayName(Messager.color(restartName));
		restart.setItemMeta(restartMeta);
		inventory.setItem(21, restart);
		
		ItemStack teleport = new ItemStack(XMaterial.COMPASS.parseItem());
		ItemMeta teleportMeta = restart.getItemMeta();
		teleportMeta.setDisplayName(Messager.color(teleportName));
		teleport.setItemMeta(teleportMeta);
		inventory.setItem(22, teleport);
	}
	
	public static void OpenConfigurationMenu(Player player, SkywarsMap map) {
		currentArenas.put(player, Skywars.get().createNewArena(map));
		Inventory inventory = Bukkit.createInventory(null, 9 * 3, Messager.color("&a&l" + map.getName()));
		inventories.put(player, inventory);
		
		UpdateInventory(inventory, player);

		player.openInventory(inventory);
	}

	static String locationName(Location location) {
		if (location == null)
			return Messager.colorFormat(positionName, "none");
		String coords = String.format("%s, %s, %s",
				Math.floor(location.getBlockX()),
				Math.floor(location.getBlockY()),
				Math.floor(location.getBlockZ()));
		return Messager.colorFormat(positionName, coords);
	}

	@EventHandler
	void onClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		Inventory inventory = inventories.get(player);
		if (event.getInventory().equals(inventory)) {
			event.setCancelled(true);
			ItemStack clicked = event.getCurrentItem();
			if(clicked == null) return;
			if (clicked.getItemMeta() == null)
				return;
			String name = clicked.getItemMeta().getDisplayName();
			
			Arena currentArena = currentArenas.get(player);
			SkywarsMap currentMap = currentArena.getMap();
			
			if (name.equals(Messager.colorFormat(minPlayersName, currentMap.getMinPlayers()))) {
				int n = currentMap.getMinPlayers() + (event.getClick() == ClickType.LEFT ? 1 : -1);
				currentMap.setMinPlayers(n);
			}
			if (name.equals(Messager.colorFormat(maxPlayersName, currentMap.getMaxPlayers()))) {
				int n = currentMap.getMaxPlayers() + (event.getClick() == ClickType.LEFT ? 1 : -1);
				currentMap.setMaxPlayers(n);
			}
			String currentWorld = currentMap.getWorldName();
			if(currentWorld == null) currentWorld = "none";
			if (name.equals(Messager.colorFormat(worldName, currentWorld))) {
				currentMap.setWorldName(player.getWorld().getName());
				System.out.println("world changed to " + currentMap.getWorldName());
			}
			if (name.equals(locationName(currentArena.getLocation()))) {
				double x = Math.round(player.getLocation().getBlockX());
				double y = Math.round(player.getLocation().getBlockY());
				double z = Math.round(player.getLocation().getBlockZ());
				World world = player.getWorld();
				Location location = new Location(world, x, y, z);
				currentArena.setLocation(location);
				player.sendMessage(String.format("set location of %s to %s %s %s in world %s",
						currentMap.getName(), location.getX(), location.getY(), location.getZ(),
						location.getWorld().getName()));
			}
			if (name.equals(Messager.color(spawnName))) {
				Arena arena = currentArenas.get(player);
				ItemStack item = new ItemStack(XMaterial.BLAZE_ROD.parseItem());
				ItemMeta meta = item.getItemMeta();
				List<String> lore = new ArrayList<String>();
				lore.add(Messager.color("&eclick the blocks that"));
				lore.add(Messager.color("&eyou want to add spawns for"));
				lore.add(Messager.color("&eyou can also rightclick to remove the last spawn you set"));
				meta.setDisplayName("yes xd");
				meta.setLore(lore);
				item.setItemMeta(meta);

				MapSpawnSetup.item = item;
				player.getInventory().setItem(player.getInventory().getHeldItemSlot(), MapSpawnSetup.item);
				player.closeInventory();

				playerLocations.put(player, player.getLocation());
				player.teleport(arena.getLocation().add(new Vector(0, 5, 0)));
				player.setVelocity(new Vector(0, 1f, 0));

				player.setAllowFlight(true);
				player.setFlying(true);

				Skywars.get().NMS().sendTitle(player, "&a&lENABLED", "&eSpawn edit mode");
				player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), 3, 2);

				player.sendMessage(Messager.color("&e&lYou are now in &a&lspawn edit mode"));
				player.sendMessage(Messager.color("&e&lUse the &6&lblaze rod &e&lto &b&lset and remove spawns"));
				player.sendMessage(Messager.color("&e&lYou can &a&lright-click &e&la block to &a&ladd an spawn"));
				player.sendMessage(Messager
						.color("&e&lYou can &c&lright-click &e&la block to &c&lremove &4&lthe last spawn you set"));
				player.sendMessage(Messager.color("&e&lTo exit, &b&ldrop the blaze rod"));
				return;
			}
			if(name.equals(Messager.color(calculateSpawnsName))) {
				currentMap.calculateSpawns();
				player.sendMessage("Spawns have been calculated and saved.");
			}
			if(name.equals(Messager.color(pasteSchematicName))) {
				currentArena.pasteSchematic();
				player.sendMessage("Pasted schematic.");
			}
			if(name.equals(Messager.color(regenerateCasesName))) {
				currentArena.resetCases();
				player.sendMessage("Regenerated cases.");
			}
			if(name.equals(Messager.color(restartName))) {
				currentArena.restart();
				player.sendMessage("Restarted.");
			}
			if(name.equals(Messager.color(teleportName))) {
				player.teleport(currentArena.getLocation());
				player.sendMessage("Teleported.");
			}
			String currentSchematic = currentMap.getSchematicFilename();
			if(currentSchematic == null) currentSchematic = "none";
			if(name.equals(Messager.colorFormat(schematicName, currentSchematic))) {
				if(schematicsFolder.listFiles() == null) {
					player.closeInventory();
					player.sendMessage("&c&lThere are no schematic files!");
					player.sendMessage("&e&lYou need to put schematics files in the schematics folder");
					return;
				}
			}
			
			String schematicName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
			for(File schematicFile : schematicsFolder.listFiles()) {
				if(schematicFile.getName().equals(schematicName)) {
					currentMap.setSchematic(schematicName);
					System.out.println("schematic changed to " + currentMap.getSchematicFilename());
				}
			}
			
			inventories.forEach((p, inv) -> {
				if(inv == inventory) {					
					UpdateInventory(inv, p);
				}
			});
		}
	}

}
