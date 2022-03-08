package me.brunorm.skywars.structures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.jnbt.ListTag;
import org.jnbt.Tag;

import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;

public class SkywarsMap {

	String name;
	int minPlayers;
	int maxPlayers;
	int centerRadius = 15;
	int teamSize = 1;
	String schematicFilename;
	Schematic schematic;
	YamlConfiguration config;
	File file;

	// this is in case the arenas method is set to SINGLE_ARENA
	Location location;
	String worldName;

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public String getWorldName() {
		return this.worldName;
	}

	HashMap<Integer, Vector> spawns = new HashMap<Integer, Vector>();

	public SkywarsMap(String name, String schematicFile, int minPlayers, int maxPlayers, int teamSize) {
		this.name = name;
		this.schematicFilename = schematicFile;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
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
		this.config.set("minPlayers", this.getMinPlayers());
		this.config.set("maxPlayers", this.getMaxPlayers());
		this.config.set("schematic", this.getSchematicFilename());
		this.config.set("centerRadius", this.getCenterRadius());
		this.config.set("worldName", this.getWorldName());
		if (this.getLocation() != null)
			this.setVectorConfig("location", this.getLocation().toVector());
		if (this.getSpawns() != null) {
			// Skywars.get().sendDebugMessage("setting spawns");
			this.config.set("spawn", null); // clear all the previous set spawns
			for (int i = 0; i < this.getSpawns().size(); i++) {
				// Skywars.get().sendDebugMessage("setting spawn " + i);
				final Vector spawn = this.spawns.get(i);
				this.setVectorConfig("spawn." + i, spawn);
			}
		} else {
			Skywars.get().sendDebugMessage("warning: spawns is null");
		}
	}

	public void saveConfig() {
		if (this.getFile() == null) {
			final File mapFile = new File(Skywars.mapsPath, String.format("%s.yml", this.getName()));
			if (!mapFile.exists()) {
				try {
					mapFile.createNewFile();
				} catch (final IOException e) {
				}
			}
			this.setFile(mapFile);
		}
		try {
			this.getConfig().save(this.file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void calculateSpawns() {

		this.getSchematic();
		if (this.schematic == null)
			return;
		Skywars.get().sendDebugMessage("Calculating spawns for map " + this.name);

		// get schematic data and beacon locations
		final Vector offset = this.schematic.getOffset();
		final ListTag tileEntities = this.schematic.getTileEntities();
		final ArrayList<Vector> beaconLocations = new ArrayList<Vector>();

		for (final Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			final Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Beacon")) {
				final Vector vector = SchematicHandler.calculatePositionWithOffset(values, offset)
						.add(new Vector(0, 1, 0));
				beaconLocations.add(vector);
			}
		}

		// saving the number of beacons
		final int totalBeacons = beaconLocations.size();

		// check if there are beacons before resetting spawns
		if (totalBeacons <= 0) {
			Skywars.get().sendDebugMessage("no beacons");
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
		Skywars.get().sendDebugMessage("spawns calculated");
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

	public int getMinPlayers() {
		return this.minPlayers;
	}

	public void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}

	public int getMaxPlayers() {
		return this.maxPlayers;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public File getFile() {
		return this.file;
	}

	public void setFile(File file) {
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

	public void setSchematic(String schematic) {
		this.schematicFilename = schematic;
	}

	public void loadSchematic() {
		if (this.schematicFilename == null)
			return;
		final File schematicFile = Skywars.get().getSchematicFile(this.schematicFilename);
		if (schematicFile == null) {
			Skywars.get().sendDebugMessage("Could not get schematic file for map " + this.getName() + "! (Maybe it doesnt exist?)");
		}
		try {
			this.schematic = SchematicHandler.loadSchematic(schematicFile);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public Schematic getSchematic() {
		if (this.schematic == null)
			this.loadSchematic();
		return this.schematic;
	}
}
