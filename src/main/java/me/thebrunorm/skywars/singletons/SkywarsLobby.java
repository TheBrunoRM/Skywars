// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.singletons;

import me.thebrunorm.skywars.Skywars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public enum SkywarsLobby {
	;

	static YamlConfiguration lobbyConfig;
	private static Location lobby;

	public static void saveLobby(Location lobby) {
		lobbyConfig.set("lobby.x", lobby.getX());
		lobbyConfig.set("lobby.y", lobby.getY());
		lobbyConfig.set("lobby.z", lobby.getZ());
		lobbyConfig.set("lobby.world", lobby.getWorld().getName());
		ConfigurationUtils.saveConfiguration(lobbyConfig, "lobby.yml");
		SkywarsLobby.lobby = lobby;
	}

	public static Location getLobby() {
		return lobby;
	}

	public static void loadLobbyFromConfig(YamlConfiguration lobbyConfig) {
		SkywarsLobby.lobbyConfig = lobbyConfig;
		if (lobbyConfig == null || lobbyConfig.get("lobby") == null) {
			lobby = null;
			return;
		}
		final String worldName = lobbyConfig.getString("lobby.world");
		if (worldName == null) return;

		final World world = Bukkit.getWorld(worldName);
		if (world == null) {
			Skywars.get().sendMessage("&cLobby world (&b%s&c) does not exist!", worldName);
			return;
		}
		final double x = lobbyConfig.getDouble("lobby.x");
		final double y = lobbyConfig.getDouble("lobby.y");
		final double z = lobbyConfig.getDouble("lobby.z");
		lobby = new Location(world, x, y, z);
	}
}
