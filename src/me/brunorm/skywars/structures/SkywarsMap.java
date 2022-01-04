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
		return location;
	}
	
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}
	
	public String getWorldName() {
		return worldName;
	}
	
	HashMap<Integer, Vector> spawns = new HashMap<Integer, Vector>();
	
	public SkywarsMap(String name, String schematicFile, int minPlayers, int maxPlayers, int teamSize) {
		this.name = name;
		this.schematicFilename = schematicFile;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
		this.teamSize = teamSize;
	}
	
	public void setLocationConfig(String string, Location location) {
		if (location == null) {
			config.set(string, null);
		} else {
			config.set(string + ".x", location.getBlockX());
			config.set(string + ".y", location.getBlockY());
			config.set(string + ".z", location.getBlockZ());
		}
	}
	
	public void setVectorConfig(String string, Vector vector) {
		if (vector == null) {
			config.set(string, null);
		} else {
			config.set(string + ".x", vector.getBlockX());
			config.set(string + ".y", vector.getBlockY());
			config.set(string + ".z", vector.getBlockZ());
		}
	}
	
	public void saveParametersInConfig() {
		if(config == null) return;
		System.out.println("saving parameters in config");
		config.set("minPlayers", this.getMinPlayers());
		config.set("maxPlayers", this.getMaxPlayers());
		config.set("schematic", this.getSchematicFilename());
		config.set("centerRadius", this.getCenterRadius());
		if (getSpawns() != null) {
			System.out.println("setting spawns");
			config.set("spawn", null); // clear all the previous set spawns
			for (int i = 0; i < this.getSpawns().size(); i++) {
				System.out.println("setting spawn " + i);
				Vector spawn = spawns.get(i);
				setVectorConfig("spawn." + i, spawn);
			}
		} else {
			System.out.println("warning: spawns is null");
		}
	}
	
	public void saveConfig() {
		if(getFile() == null) {			
			File mapFile = new File(Skywars.mapsPath, String.format("%s.yml", this.getName()));
			if (!mapFile.exists()) {
				try {
					mapFile.createNewFile();
				} catch (IOException e) {
				}
			}
			setFile(mapFile);
		}
		try {
			getConfig().save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void calculateSpawns() {
		
		getSchematic();
		
		// clear spawns
		spawns.clear();
		config.set("spawn", null);
		
		Vector offset = schematic.getOffset();
		ListTag tileEntities = schematic.getTileEntities();
		System.out.println("Calculating spawns for map " + this.name);
	
		ArrayList<Vector> beaconLocations = new ArrayList<Vector>();
	
		for (Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Beacon")) {
				Vector vector = SchematicHandler.calculatePositionWithOffset(values, offset)
						.add(new Vector(0,1,0));
				beaconLocations.add(vector);
			}
		}
		
		// saving the number of beacons
		int totalBeacons = beaconLocations.size();
		
		// set the first spawn
		spawns.put(0, beaconLocations.get(0));
		beaconLocations.remove(0);
		
		for(int i = 1; i < totalBeacons; i++) {
			Vector previousSpawn = spawns.get(i-1);
			Vector closest = beaconLocations.get(0);
			if(beaconLocations.size() > 1) {
				for(Vector currentSpawn : beaconLocations) {
					if(SkywarsUtils.distance(previousSpawn, currentSpawn)
							< SkywarsUtils.distance(previousSpawn, closest)) {
						closest = currentSpawn;
					}
				}
				beaconLocations.remove(closest);
			}
			spawns.put(i, closest);
		}
		
		saveParametersInConfig();
		saveConfig();
		System.out.println("spawns calculated");
	}
	
	public HashMap<Integer, Vector> getSpawns() {
		return spawns;
	}
	
	public Vector getSpawn(Object key) {
		return spawns.get(key);
	}
	
	public void setSpawn(int spawn, Vector vector) {
		spawns.put(spawn, vector);
	}
	
	public YamlConfiguration getConfig() {
		return config;
	}

	public void setConfig(YamlConfiguration config) {
		this.config = config;
	}
	
	public String getName() {
		return name;
	}
	
	public int getMinPlayers() {
		return minPlayers;
	}

	public void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	public int getCenterRadius() {
		return centerRadius;
	}

	public void setCenterRadius(int centerRadius) {
		this.centerRadius = centerRadius;
	}
	
	public String getSchematicFilename() {
		return schematicFilename;
	}

	public void setSchematic(String schematic) {
		this.schematicFilename = schematic;
	}
	
	void loadSchematic() {
		File schematicFile = Skywars.get().getSchematicFile(schematicFilename);
		if(schematicFile == null) {
			System.out.println("Could not get schematic file for map "
					+ getName() + "! (Maybe it doesnt exist?)");
		}
		try {
			schematic = SchematicHandler.loadSchematic(schematicFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Schematic getSchematic() {
		if(schematic == null) loadSchematic();
		return schematic;
	}
}
