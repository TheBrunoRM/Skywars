package me.brunorm.skywars.managers;

import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;

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
		final Arena arena = getArenaByMap(map, true);
		if (arena != null) {
			Skywars.get().sendDebugMessage("joining player to map");
			arena.joinPlayer(player);
			return true;
		}
		Skywars.get().sendDebugMessage("could not find or create arena for map: " + map.getName());
		return false;
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
		unloadWorld(arena.getWorld(), arena.getMap());
		arena = null;
	}

	public static boolean unloadWorld(World world, SkywarsMap map) {
		Skywars.get().sendDebugMessage("Unloading world '%s' for map '%s'", world.getName(), map.getName());
		boolean unloaded = false;
		int tries = 0;
		while (!unloaded) {
			if (tries >= 5) {
				break;
			}
			Skywars.get().sendDebugMessage("Trying to unload world: %s (tries: %s)", world.getName(), tries);
			for (final Player p : world.getPlayers()) {
				Skywars.get().sendDebugMessage("Teleporting player %s to another world", p.getName());
				p.teleport(Bukkit.getWorlds().stream().filter(w -> w.getName() != world.getName()).findFirst().get()
						.getSpawnLocation());
			}
			unloaded = Bukkit.unloadWorld(world, false);
			tries++;
		}
		if (!unloaded) {
			Skywars.get().sendMessage("Could not unload world '%s' for map '%s'", world.getName(), map.getName());
		} else {
			Skywars.get().sendDebugMessage("Successfully unloaded world '%s' for map '%s'", world.getName(),
					map.getName());
			try {
				FileUtils.deleteDirectory(world.getWorldFolder());
				Skywars.get().sendDebugMessage("Sucessfully deleted world '%s' for map '%s'", world.getName(),
						map.getName());
			} catch (final Exception e) {
				e.printStackTrace();
				Skywars.get().sendMessage("Could not delete world '%s' for map '%s'", world.getName(), map.getName());
			}
		}
		return unloaded;
	}
}
