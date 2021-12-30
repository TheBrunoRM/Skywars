package me.brunorm.skywars.structures;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import me.brunorm.skywars.Skywars;
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
	
	HashMap<Integer, Location> spawns = new HashMap<Integer, Location>();
	
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
	
	public void saveParametersInConfig() {
		if(config == null) return;
		config.set("minPlayers", this.getMinPlayers());
		config.set("maxPlayers", this.getMaxPlayers());
		config.set("schematic", this.getSchematicFilename());
		config.set("centerRadius", this.getCenterRadius());
		if (getSpawns() != null) {
			for (int i = 0; i < this.getSpawns().size(); i++) {
				Location spawn = spawns.get(i);
				setLocationConfig("spawn." + i, spawn);
			}
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
		// get the beacon locations from the schematic
		// and set them in the arena config file
		throw new NotImplementedException();
	}
	
	public HashMap<Integer, Location> getSpawns() {
		return spawns;
	}
	
	public Location getSpawn(Object key) {
		return spawns.get(key);
	}
	
	public void setSpawn(int spawn, Location location) {
		spawns.put(spawn, location);
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
