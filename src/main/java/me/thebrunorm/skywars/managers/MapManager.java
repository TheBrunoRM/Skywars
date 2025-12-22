// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.managers;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.singletons.ConfigurationUtils;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.structures.SkywarsMap;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MapManager {
	private final ArrayList<SkywarsMap> maps = new ArrayList<>();

	public static void setupWorld(World world) {
		if (world == null) {
			Skywars.get().sendDebugMessage("Can't set up world because it is null");
			return;
		}

		world.setDifficulty(Difficulty.NORMAL);
		world.setSpawnFlags(true, true);
		world.setPVP(true);
		world.setStorm(false);
		world.setThundering(false);
		world.setWeatherDuration(Integer.MAX_VALUE);
		world.setKeepSpawnInMemory(false);
		world.setTicksPerAnimalSpawns(1);
		world.setTicksPerMonsterSpawns(1);
		world.setAutoSave(false);

		world.setGameRuleValue("doMobSpawning", "false");
		world.setGameRuleValue("mobGriefing", "false");
		world.setGameRuleValue("doFireTick", "false");
		world.setGameRuleValue("showDeathMessages", "false");
		world.setGameRuleValue("announceAdvancements", "false");

		Skywars.get().sendDebugMessage("Successfully set up world settings: &b%s", world.getName());
	}

	public SkywarsMap getRandomMap() {
		return this.maps.get((int) (Math.floor(Math.random() * this.maps.size() + 1) - 1));
	}

	public boolean createMap(String name) {
		if (this.getMap(name) != null)
			return false;
		final SkywarsMap map = new SkywarsMap(name);
		final File file = new File(Skywars.mapsPath, name + ".yml");
		try {
			file.createNewFile();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		map.setConfigFile(file);
		map.saveParametersInConfig();
		map.saveConfig();
		this.maps.add(map);
		return true;
	}

	public SkywarsMap getMap(String name) {
		for (final SkywarsMap map : this.maps) {
			if (map.getName().equalsIgnoreCase(name))
				return map;
		}
		return null;
	}

	public boolean deleteMap(String name) {
		final SkywarsMap map = this.getMap(name);
		if (map == null)
			return false;
		map.getFile().delete();
		this.maps.remove(map);
		return true;
	}

	public void loadWorlds() {
		final File folder = new File(Skywars.worldsPath);
		if (!folder.exists())
			folder.mkdirs();
		final File[] worlds = folder.listFiles();
		Skywars.get().sendDebugMessage("Loading %s worlds", worlds.length);

		final HashMap<String, SkywarsMap> worldNames = new HashMap<>();
		final List<SkywarsMap> filtered = this.getMaps().stream().filter(map -> map.getWorldName() != null)
				.collect(Collectors.toList());
		for (final SkywarsMap map : filtered)
			worldNames.put(map.getWorldName(), map);

		for (final File worldFolder : worlds) {
			if (!worldFolder.isDirectory()) {
				if (worldFolder.isFile() && worldFolder.getName().endsWith(".yml")) {
					// it may be a map configuration file
					worldFolder.renameTo(new File(Skywars.mapsPath, worldFolder.getName()));
				} else {
					Skywars.get().sendMessage("&6Unknown world file found: &b%s", worldFolder.getName());
				}
			}

			if (worldNames.get(worldFolder.getName()) == null) {
				// no map has this world as worldname
				// create map config file for this world
				final File mapFile = this.createMapFileFromWorldFolder(worldFolder);
				this.loadMapFromFile(mapFile);
			}
		}
	}

	public ArrayList<SkywarsMap> getMaps() {
		return this.maps;
	}

	public File createMapFileFromWorldFolder(File worldFolder) {
		final File mapFile = new File(Skywars.mapsPath, worldFolder.getName() + ".yml");
		final YamlConfiguration mapConfig = YamlConfiguration.loadConfiguration(mapFile);
		mapConfig.set("world", worldFolder.getName());
		mapConfig.set("teamSize", 1);
		try {
			mapConfig.save(mapFile);
		} catch (final IOException e) {
			e.printStackTrace();
			Skywars.get().sendMessage("Could not save map configuration file for world: %s", worldFolder.getName());
		}
		Skywars.get().sendDebugMessage("Saved map configuration file '%s' for world: %s", mapFile.getName(),
				worldFolder.getName());
		return mapFile;
	}

	public void loadMapFromFile(File file) {
		Skywars.get().sendDebugMessage("&eLoading map file: &a%s", file.getName());
		final String name = file.getName().replaceFirst("[.][^.]+$", "");
		final YamlConfiguration mapConfig = YamlConfiguration.loadConfiguration(file);

		// create map and set values from config
		final SkywarsMap map = new SkywarsMap(name, mapConfig.getInt("teamSize"));

		map.setConfig(mapConfig);
		map.setConfigFile(file);

		final String worldName = mapConfig.getString("world");
		map.setWorldName(worldName);
		Skywars.get().sendDebugMessage("worldname for %s: %s", map.getName(), worldName);

		if (mapConfig.get("spawn") == null) {
			Skywars.get().sendDebugMessage("Found no spawns when loading map, calculating spawns...");
			map.calculateSpawns();
		} else {
			Skywars.get().sendDebugMessage("Loading spawns for map: %s", map.getName());
			for (final String spawn : mapConfig.getConfigurationSection("spawn").getKeys(false)) {
				final int i = Integer.parseInt(spawn);
				if (mapConfig.get(String.format("spawn.%s", i)) == null)
					continue;
				final double x = mapConfig.getDouble(String.format("spawn.%s.x", i));
				final double y = mapConfig.getDouble(String.format("spawn.%s.y", i));
				final double z = mapConfig.getDouble(String.format("spawn.%s.z", i));
				final Vector vector = new Vector(x, y, z);
				map.getSpawns().put(i, vector);
			}
		}

		if (mapConfig.get("chest") == null) {
			Skywars.get().sendDebugMessage("Found no chests when loading map.");
		} else {
			Skywars.get().sendDebugMessage("Loading chests for map: %s", map.getName());
			for (final String spawn : mapConfig.getConfigurationSection("chest").getKeys(false)) {
				final int i = Integer.parseInt(spawn);
				if (mapConfig.get(String.format("chest.%s", i)) == null)
					continue;
				final double x = mapConfig.getDouble(String.format("chest.%s.x", i));
				final double y = mapConfig.getDouble(String.format("chest.%s.y", i));
				final double z = mapConfig.getDouble(String.format("chest.%s.z", i));
				final Vector vector = new Vector(x, y, z);
				map.getChests().put(i, vector);
			}
		}

		this.maps.add(map);
		Skywars.get().sendDebugMessage("&eLoaded map: &a%s", map.getName());
	}

	public void loadMaps() {
		Skywars.get().sendDebugMessage("&eLoading maps...");
		this.maps.clear();

		final File folder = new File(Skywars.mapsPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		if (folder.listFiles().length <= 0) {
			Skywars.get().sendDebugMessage(MessageUtils.color("&eSetting up default map."));
			ConfigurationUtils.copyDefaultContentsToFile("maps/MiniTrees.yml",
					new File(Skywars.mapsPath, "MiniTrees.yml"));
		}

		// TODO replace with world map instead of schematic file
		final File schematics = new File(Skywars.schematicsPath);
		if (!schematics.exists())
			schematics.mkdirs();
		if (schematics.listFiles().length <= 0) {
			Skywars.get().sendDebugMessage(MessageUtils.color("&eSetting up default schematic."));
			ConfigurationUtils.copyDefaultContentsToFile("schematics/mini_trees.schematic",
					new File(Skywars.schematicsPath, "mini_trees.schematic"));
		}

		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				Skywars.get().sendDebugMessage("Loading world in maps folder: %s", file.getName());
				file.renameTo(new File(Skywars.worldsPath, file.getName()));
				continue;
			}
			this.loadMapFromFile(file);
		}
		Skywars.get().sendDebugMessage("&eFinished loading maps.");
	}
}
