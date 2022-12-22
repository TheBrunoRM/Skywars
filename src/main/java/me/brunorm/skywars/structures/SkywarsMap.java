package me.brunorm.skywars.structures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.managers.ArenaManager;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class SkywarsMap {

	String name;
	int centerRadius = 15;
	int teamSize = 1;

	YamlConfiguration config;
	File file;

	// this is in case the map uses an schematic file to get the map
	String schematicFilename;
	Schematic schematic;
	boolean schematicError = false;

	// this is in case the arenas method is set to SINGLE_ARENA
	Location location;
	String worldName;

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return this.location;
	}

	public String getWorldName() {
		if (this.worldName != null)
			return this.worldName;
		final Location loc = this.getLocation();
		if (loc == null)
			return null;
		final World world = loc.getWorld();
		if (world == null)
			return null;
		return world.getName();
	}

	HashMap<Integer, Vector> spawns = new HashMap<Integer, Vector>();

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
		this.config.set("schematic", this.getSchematicFilename());
		this.config.set("centerRadius", this.getCenterRadius());
		if (this.getLocation() == null) {
			this.config.set("location", null);

			// saves only the world for reloading the whole world
			this.config.set("world", this.getWorldName());
		} else {
			this.config.set("world", null);

			// saves location coordinates for schematic pasting
			this.config.set("location.world", this.getWorldName());
			this.setVectorConfig("location", this.getLocation().toVector());
		}
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

		Skywars.get().sendDebugMessage("Calculating spawns for map " + this.name);

		final ArrayList<Vector> beaconLocations = new ArrayList<Vector>();

		if (this.getSchematicFilename() != null) {
			// get beacon locations from schematic data
			final Vector offset = this.schematic.getOffset();
			final ListTag<CompoundTag> blockEntities = this.schematic.getBlockEntities().asCompoundTagList();

			for (final CompoundTag tag : blockEntities) {
				String type = tag.getString("id");
				if (type == null)
					type = tag.getString("Id");
				if (type.equalsIgnoreCase("beacon")) {
					final int[] posArray = tag.getIntArray("Pos");
					final Vector pos = posArray.length > 0 ? SchematicHandler.getVector(posArray)
							: SchematicHandler.getVector(tag);
					final Vector vector = pos.add(offset).add(new Vector(0, 1, 0));
					beaconLocations.add(vector);
				}
			}
		} else {
			// get beacon locations from world

			final Arena arena = ArenaManager.getArenaAndCreateIfNotFound(this);
			Skywars.get().sendDebugMessage("[debug, skywarsmap-calculateSpawns] arena: " + arena);
			for (final BlockState state : arena.getAllBlockStatesInMap(Material.BEACON)) {
				beaconLocations.add(state.getLocation().toVector());
				Skywars.get().sendDebugMessage("added beacon location from world: " + state.getLocation().toVector());
			}
		}

		// saving the number of beacons
		final int totalBeacons = beaconLocations.size();

		// check if there are beacons before resetting spawns
		if (totalBeacons <= 0) {
			Skywars.get().sendDebugMessage("warning: no beacons to set the spawns to");
			return;
		}

		// clear spawns
		this.spawns.clear();
		this.config.set("spawn", null);

		// set the first spawn
		this.spawns.put(0, beaconLocations.get(0));
		beaconLocations.remove(0);

		for (int i = 1; i < totalBeacons; i++) {
			final Vector previousSpawn = this.spawns.get(i - 1);
			Vector closest = beaconLocations.get(0);
			if (beaconLocations.size() > 1) {
				for (final Vector currentSpawn : beaconLocations) {
					if (SkywarsUtils.distance(previousSpawn, currentSpawn) < SkywarsUtils.distance(previousSpawn,
							closest)) {
						closest = currentSpawn;
					}
				}
				beaconLocations.remove(closest);
			}
			this.spawns.put(i, closest);
		}

		this.saveParametersInConfig();
		this.saveConfig();
		Skywars.get().sendDebugMessage("spawns calculated and saved in config");
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

	public String getSchematicFilename() {
		return this.schematicFilename;
	}

	public void setSchematicFilename(String schematic) {
		this.schematicFilename = schematic;
	}

	public void loadSchematic() {
		if (this.schematicError)
			return;
		if (this.schematicFilename == null)
			return;
		final File schematicFile = Skywars.get().getSchematicFile(this.schematicFilename);
		if (schematicFile == null) {
			Skywars.get().sendDebugMessage(
					"Could not get schematic file for map " + this.getName() + "! (Maybe it doesnt exist?)");
		}
		try {
			this.schematic = SchematicHandler.loadSchematic(schematicFile);
		} catch (final IOException e) {
			Skywars.get().sendMessage("&cCould not load schematic for map &b" + this.getName());
			this.schematicError = true;
		}
	}

	public Schematic getSchematic() {
		if (this.schematic == null)
			this.loadSchematic();
		return this.schematic;
	}

	public void setWorldName(String name) {
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
