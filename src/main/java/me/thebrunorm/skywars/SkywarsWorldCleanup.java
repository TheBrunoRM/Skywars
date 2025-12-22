package me.thebrunorm.skywars;

import me.thebrunorm.skywars.structures.Arena;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public enum SkywarsWorldCleanup {
	;

	final static File worldsToDeleteFile = new File(Skywars.get().getDataFolder(), "delete_worlds.yml");

	public static void cleanupWorlds() {
		if (!worldsToDeleteFile.exists()) return;

		final YamlConfiguration deleteWorldsConfig = YamlConfiguration.loadConfiguration(worldsToDeleteFile);
		final List<String> list = deleteWorldsConfig.getStringList("worlds");
		for (final String worldName : new ArrayList<>(list)) {
			final World world = Bukkit.getWorld(worldName);
			if (world != null) {
				for (final Player p : world.getPlayers())
					SkywarsUtils.teleportPlayerLobbyOrLastLocation(p, true);
				Bukkit.unloadWorld(worldName, false);
			}
			final File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
			if (worldFolder.exists() && worldFolder.isDirectory())
				try {
					FileUtils.deleteDirectory(worldFolder);
				} catch (final IOException e) {
					e.printStackTrace();
					Skywars.get().sendMessage("&cCould not delete world folder: &b" + worldFolder.getPath());
				}
			list.remove(worldName);
		}
		deleteWorldsConfig.set("worlds", list);
		try {
			deleteWorldsConfig.save(worldsToDeleteFile);
		} catch (final IOException e) {
			e.printStackTrace();
			Skywars.get().sendMessage("Could not save the world deletion list to file: " + worldsToDeleteFile.getPath());
		}
	}
	
	public static void saveWorldList() {
		final List<String> worldNames = new ArrayList<>(Skywars.get().getArenas().size());
		for (final Arena arena : Skywars.get().getArenas()) {
			arena.clear(false);
			worldNames.add(arena.getWorldName());
		}
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(worldsToDeleteFile);
		worldNames.addAll(config.getStringList("worlds"));
		config.set("worlds", worldNames);
		try {
			if (!worldsToDeleteFile.exists())
				if (!worldsToDeleteFile.createNewFile())
					Skywars.get().sendMessage("Could not create world list file.");
			config.save(worldsToDeleteFile);
		} catch (final IOException e) {
			e.printStackTrace();
			Skywars.get().sendMessage("Could not write world list to file.");
		}
	}
}
