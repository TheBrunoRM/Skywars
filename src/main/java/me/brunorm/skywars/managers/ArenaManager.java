package me.brunorm.skywars.managers;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

	public static Arena getArenaByMap(SkywarsMap map) {
		Skywars.get().sendDebugMessage("all arenas: %s", Skywars.get().getArenas().size());
		for (final Arena arena : Skywars.get().getArenas()) {
			if (arena.getMap() == map)
				return arena;
		}
		return null;
	}

	public static ArrayList<Arena> getArenasByMap(SkywarsMap map) {
		return Skywars.get().getArenas().stream().filter(arena -> arena.getMap() == map)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public static Arena getArenaAndCreateIfNotFound(SkywarsMap map) {
		if (map == null)
			return null;

		switch (Skywars.config.getString("arenasMethod")) {
		case "MULTI_ARENA":
			return createNewArena(map);
		case "SINGLE_ARENA":
			final Arena arena = getArenaByMap(map);
			if (arena != null) {
				Skywars.get().sendDebugMessage("arena already exists, returning existing: %s", map.getName());
				return arena;
			}
			return createNewArena(map);
		}

		return null;
	}

	public static boolean joinMap(SkywarsMap map, Player player) {
		final Arena arena = getArenaAndCreateIfNotFound(map);
		if (arena != null) {
			Skywars.get().sendDebugMessage("joining player to map");
			arena.joinPlayer(player);
			return true;
		}
		Skywars.get().sendDebugMessage("could not find or create arena for map: " + map.getName());
		return false;
	}

	public static void joinRandomMap(Player player) {
		final SkywarsMap map = Skywars.get().getRandomMap();
		joinMap(map, player);
	}

	public static Arena createNewArena(SkywarsMap map) {
		if (Skywars.config.getString("arenasMethod").equalsIgnoreCase("MULTI_ARENA")) {
			Skywars.get().sendDebugMessage("&ccreating &bnew arena &6for map " + map.getName());
			final Arena arena = new Arena(map);
			arena.setLocation(getNextFreeLocation());
			arena.pasteSchematic();
			arena.resetCases();
			Skywars.get().getArenas().add(arena);
			return arena;
		} else if (Skywars.config.getString("arenasMethod").equalsIgnoreCase("SINGLE_ARENA")) {
			Arena arena = getJoinableArenaByMap(map);
			if (arena != null)
				return arena;

			arena = getArenaByMap(map);
			if (arena != null)
				return arena;

			Skywars.get().sendDebugMessage("&ccreating &bsingle arena &6for map " + map.getName());
			arena = new Arena(map);
			Skywars.get().sendDebugMessage("worldname for map %s: %s", map.getName(), map.getWorldName());
			if (map.getWorldName() != null) {
				final World world = arena.getWorldAndLoadIfItIsNotLoaded();
				if (world != null)
					Skywars.get().sendDebugMessage("world: ", world.getName());
			} else {
				arena.setLocation(map.getLocation());
				Skywars.get().sendDebugMessage("location: " + map.getLocation());
			}
			arena.pasteSchematic();
			Skywars.get().getArenas().add(arena);

			return arena;
		}
		return null;
	}

	public static void removeArenaFromListAndDeleteArena(Arena arena) {
		Skywars.get().sendDebugMessage("removing arena " + arena.getMap().getName());
		Skywars.get().getArenas().remove(arena);
		arena = null;
	}

	public static Location getNextFreeLocation() {
		if (!Skywars.config.getString("arenasMethod").equalsIgnoreCase("MULTI_ARENA")) {
			Skywars.get().sendDebugMessage("warning: getNextFreeLocation called though MULTI_ARENA is disabled!");
			return null;
		}

		int x = 0;
		int z = 0;

		for (int i = 0; i < Skywars.get().getArenas().size(); i++) {
			if (i % 2 == 0) {
				z += 1;
			} else {
				x += 1;
			}
		}
		return new Location(Bukkit.getWorld(Skywars.config.getString("arenas.world")),
				x * Skywars.config.getInt("arenas.separation"), Skywars.config.getInt("arenas.Y"),
				z * Skywars.config.getInt("arenas.separation"));
	}

}
