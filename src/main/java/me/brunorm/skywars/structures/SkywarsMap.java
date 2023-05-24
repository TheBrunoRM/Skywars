package me.brunorm.skywars.structures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.managers.ArenaManager;

public class SkywarsMap {

	String name;
	int centerRadius = 15;
	int teamSize = 1;

	YamlConfiguration config;
	File file;

	String worldName;

	public String getWorldName() {
		return this.worldName;
	}

	HashMap<Integer, Vector> spawns = new HashMap<Integer, Vector>();
	HashMap<Integer, Vector> chests = new HashMap<Integer, Vector>();

	public SkywarsMap(String name, int teamSize) {
		this.name = name;
		this.teamSize = teamSize;
	}

	public SkywarsMap(String name) {
		this.name = name;
	}

	public void setLocationConfig(String string, Location location) {
		if (location == null) {
			this.config.set(string, null);
		} else {
			this.config.set(string + ".x", location.getBlockX());
			this.config.set(string + ".y", location.getBlockY());
			this.config.set(string + ".z", location.getBlockZ());
		}
	}

	public void setVectorConfig(String string, Vector vector) {
		if (vector == null) {
			this.config.set(string, null);
		} else {
			this.config.set(string + ".x", vector.getBlockX());
			this.config.set(string + ".y", vector.getBlockY());
			this.config.set(string + ".z", vector.getBlockZ());
		}
	}

	public void saveParametersInConfig() {
		if (this.config == null)
			this.config = YamlConfiguration.loadConfiguration(this.getFile());
		// Skywars.get().sendDebugMessage("saving parameters in config");
		this.config.set("teamSize", this.getTeamSize());
		this.config.set("centerRadius", this.getCenterRadius());
		this.config.set("world", this.getWorldName());
		if (this.getSpawns() == null) {
			Skywars.get().sendDebugMessage("warning: spawns is null");
			return;
		}
		// Skywars.get().sendDebugMessage("setting spawns");
		this.config.set("spawn", null); // clear all the previous set spawns
		for (int i = 0; i < this.getSpawns().size(); i++) {
			// Skywars.get().sendDebugMessage("setting spawn " + i);
			final Vector spawn = this.spawns.get(i);
			this.setVectorConfig("spawn." + i, spawn);
		}
	}

	public void saveConfig() {
		if (this.getFile() == null) {
			final File mapFile = new File(Skywars.mapsPath, String.format("%s.yml", this.getName()));
			if (!mapFile.exists()) {
				try {
					mapFile.createNewFile();
				} catch (final IOException e) {
					Skywars.get().sendMessage("Could not create map file: " + mapFile.getPath());
				}
			}
			this.setConfigFile(mapFile);
		}
		try {
			this.getConfig().save(this.file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void calculateSpawns() {

		Skywars.get().sendDebugMessage("&bCalculating spawns for map &6" + this.name);

		final ArrayList<Vector> spawnLocations = new ArrayList<Vector>();

		// get beacon locations from world

		final Arena arena = ArenaManager.getArenaByMap(this, true);
		Skywars.get().sendDebugMessage("[debug, skywarsmap-calculateSpawns] arena: " + arena);
		for (final BlockState state : arena.getAllBlockStatesInMap(XMaterial.BEACON.parseMaterial())) {
			spawnLocations.add(state.getLocation().toVector());
			Skywars.get().sendDebugMessage("added beacon location from world: " + state.getLocation().toVector());
		}

		// get spawns from cages

		final Material glass = XMaterial.GLASS.parseMaterial();
		for (final Block block : arena.getAllBlocksInMap(glass)) {
			if (block.getLocation().add(1, 1, 0).getBlock().getState().getType() == glass
					&& block.getLocation().add(-1, 1, 0).getBlock().getState().getType() == glass
					&& block.getLocation().add(0, 1, 1).getBlock().getState().getType() == glass
					&& block.getLocation().add(0, 1, -1).getBlock().getState().getType() == glass
					&& block.getLocation().add(1, 2, 0).getBlock().getState().getType() == glass
					&& block.getLocation().add(-1, 2, 0).getBlock().getState().getType() == glass
					&& block.getLocation().add(0, 2, 1).getBlock().getState().getType() == glass
					&& block.getLocation().add(0, 2, -1).getBlock().getState().getType() == glass) {
				spawnLocations.add(block.getLocation().toVector().add(new Vector(0, 1, 0)));
				Skywars.get()
						.sendDebugMessage("added spawn location from glass cage: " + block.getLocation().toVector());
			}
		}

		// clear spawns
		this.spawns.clear();
		this.config.set("spawn", null);

		// saving the number of spawn locations
		// before modifying the list
		final int totalSpawnLocations = spawnLocations.size();
		Skywars.get().sendDebugMessage("got %s spawns", totalSpawnLocations);

		if (totalSpawnLocations <= 0)
			return;

		// set the first spawn
		this.spawns.put(0, spawnLocations.get(0));
		spawnLocations.remove(0);

		// make the spawns be in circular order
		for (int i = 1; i < totalSpawnLocations; i++) {
			final Vector previousSpawn = this.spawns.get(i - 1);
			Vector closest = spawnLocations.get(0);
			if (spawnLocations.size() > 1) {
				for (final Vector currentSpawn : spawnLocations) {
					if (SkywarsUtils.distance(previousSpawn, currentSpawn) < SkywarsUtils.distance(previousSpawn,
							closest)) {
						closest = currentSpawn;
					}
				}
				spawnLocations.remove(closest);
			}
			this.spawns.put(i, closest);
		}

		this.saveParametersInConfig();
		this.saveConfig();
		Skywars.get().sendDebugMessage("spawns calculated and saved in config");
	}

	public HashMap<Integer, Vector> getChests() {
		return this.chests;
	}

	public HashMap<Integer, Vector> getSpawns() {
		return this.spawns;
	}

	public Vector getSpawn(Object key) {
		return this.spawns.get(key);
	}

	public void setSpawn(int spawn, Vector vector) {
		this.spawns.put(spawn, vector);
	}

	public YamlConfiguration getConfig() {
		return this.config;
	}

	public void setConfig(YamlConfiguration config) {
		this.config = config;
	}

	public String getName() {
		return this.name;
	}

	public int getMaxPlayers() {
		return this.getSpawns().size();
	}

	public int getTeamSize() {
		return this.teamSize;
	}

	public File getFile() {
		return this.file;
	}

	public void setConfigFile(File file) {
		this.file = file;
	}

	public int getCenterRadius() {
		return this.centerRadius;
	}

	public void setCenterRadius(int centerRadius) {
		this.centerRadius = centerRadius;
	}

	public void setWorldName(String name) {
		if (name == null)
			return;
		Skywars.get()
				.sendDebugMessage("[debug] setting world name for map: " + this.getName() + " world name: " + name);
		this.worldName = name;
		final File baseWorld = new File(Bukkit.getWorldContainer(), name);
		if (!baseWorld.isDirectory())
			return;
		Skywars.get().sendDebugMessage("making backup of world '%s' for map: %s", name, this.getName());
		try {
			final File worldFile = new File(Skywars.worldsPath, name);
			if (!worldFile.canWrite()) {
				Skywars.get().sendDebugMessage("can not write to location: %s", baseWorld.getAbsolutePath());
				return;
			}
			worldFile.mkdirs();
			Skywars.get().sendDebugMessage("copying: %s", baseWorld.getAbsolutePath());
			Skywars.get().sendDebugMessage("to location: %s", worldFile.getAbsolutePath());
			FileUtils.copyDirectory(baseWorld, worldFile);
		} catch (final IOException e) {
			e.printStackTrace();
			Skywars.get().sendMessage("Could not make backup of world '%s' for map: %s", name, this.getName());
		}
	}

	public void setTeamSize(int n) {
		this.teamSize = n;
	}
}
