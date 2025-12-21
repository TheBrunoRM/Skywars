/* (C) 2021 Bruno */
package me.thebrunorm.skywars.managers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;

public class ArenaManager {

	public static Arena getJoinableArenaByMap(SkywarsMap map) {
		if (map == null)
			return null;
		for (final Arena arena : Skywars.get().getArenas()) {
			if (arena.started())
				continue;
			if (!arena.isJoinable())
				continue;
			if (arena.getMap() != map)
				continue;
			return arena;
		}
		return null;
	}

	public static ArrayList<Arena> getArenasByMap(SkywarsMap map) {
		final ArrayList<Arena> list = new ArrayList<Arena>();
		for (final Arena arena : Skywars.get().getArenas()) {
			if (arena.getMap() == map)
				list.add(arena);
		}
		return list;
	}

	public static Arena getArenaByMap(SkywarsMap map) {
		Skywars.get().sendDebugMessage("all arenas: %s", Skywars.get().getArenas().size());
		if (map == null)
			return null;
		for (final Arena arena : Skywars.get().getArenas()) {
			if (arena.getMap() == map)
				return arena;
		}
		return null;
	}

	public static Arena getArenaByMap(SkywarsMap map, boolean createIfNotFound) {
		if (map == null)
			return null;
		final Arena arena = getArenaByMap(map);
		if (arena != null)
			return arena;
		if (!createIfNotFound)
			return null;
		return createNewArena(map);
	}

	public static boolean joinMap(SkywarsMap map, Player player) {
		if(map == null) {
			Skywars.get().sendDebugMessage("could not find or create arena for a null map");
			return false;
		}
		final Arena arena = getArenaByMap(map, true);
		if (arena == null) {
			Skywars.get().sendDebugMessage("could not find or create arena for map: " + map.getName());
			return false;
		}
		Skywars.get().sendDebugMessage("joining player to map");
		arena.joinPlayer(player);
		return true;
	}

	public static void joinRandomMap(Player player) {
		final SkywarsMap map = Skywars.get().getMapManager().getRandomMap();
		joinMap(map, player);
	}

	public static Arena createNewArena(SkywarsMap map) {
		final Arena arena = new Arena(map);
		Skywars.get().getArenas().add(arena);
		Skywars.get().sendDebugMessage("created new arena: " + map.getName());
		return arena;
	}

	public static void removeArena(Arena arena) {
		Skywars.get().sendDebugMessage("removing arena " + arena.getMap().getName());
		Skywars.get().getArenas().remove(arena);
		unloadAndDeleteWorldForMap(arena.getWorld(), arena.getMap());
		Skywars.get().getSignManager().updateSigns();
	}

	public static boolean unloadAndDeleteWorldForMap(World world, SkywarsMap map) {
		if (Bukkit.getWorld(world.getName()) == null) {
			Skywars.get().sendDebugMessage("Can't unload world, it does not exist: %s", world.getName());
			return false;
		}

		// teleport all players outside the world
		// before unloading it
		final List<Player> players = world.getPlayers();
		if (players.size() > 0) {
			Skywars.get().sendDebugMessage("There are %s players in the world,"
					+ " teleporting them back to the lobby or to their last location...", players.size());
			for (final Player p : players)
				SkywarsUtils.teleportPlayerBackToTheLobbyOrToTheirLastLocationIfTheLobbyIsNotSet(p, true);
		}

		Skywars.get().sendDebugMessage("Unloading world '%s' for map '%s'", world.getName(), map.getName());
		boolean unloaded = false;
		int tries = 0;
		while (!unloaded) {
			if (unloaded || tries >= 5) {
				break;
			}
			tries++;
			Skywars.get().sendDebugMessage("Trying to unload world: %s (tries: %s)", world.getName(), tries);
			for (final Player p : world.getPlayers()) {
				Skywars.get().sendDebugMessage("Teleporting player %s to another world", p.getName());
				p.teleport(Bukkit.getWorlds().stream().filter(w -> w.getName() != world.getName()).findFirst().get()
						.getSpawnLocation());
			}
			unloaded = Bukkit.unloadWorld(world, false);
			if (unloaded) {
				Skywars.get().sendDebugMessage("Successfully unloaded world '%s' for map '%s'", world.getName(),
						map.getName());
			}
		}

		if (!unloaded)
			Skywars.get().sendMessage("Could not unload world '%s' for map '%s'", world.getName(), map.getName());

		try {
			FileUtils.deleteDirectory(world.getWorldFolder());
			Skywars.get().sendDebugMessage("Sucessfully deleted world '%s' for map '%s'", world.getName(),
					map.getName());
		} catch (final Exception e) {
			e.printStackTrace();
			Skywars.get().sendMessage("Could not delete world '%s' for map '%s'", world.getName(), map.getName());
		}

		return unloaded;
	}
}
