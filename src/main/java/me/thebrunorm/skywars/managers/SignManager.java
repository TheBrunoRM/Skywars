package me.thebrunorm.skywars.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;

public class SignManager implements Listener {

	HashMap<Location, SkywarsMap> signs = new HashMap<Location, SkywarsMap>();

	public HashMap<Location, SkywarsMap> getSigns() {
		return this.signs;
	}

	public static String formatElement(Location loc, SkywarsMap map) {
		return loc.getWorld().getName() + ";" + //
				loc.getBlockX() + ";" + //
				loc.getBlockY() + ";" + //
				loc.getBlockZ() + ";" + //
				map.getName();
	}

	@EventHandler
	void onSignChange(SignChangeEvent event) {
		int titleLine = -1;
		int i = -1;
		for (final String line : event.getLines()) {
			i++;
			if (line.equalsIgnoreCase("[SkyWars]")) {
				titleLine = i;
				break;
			}
		}
		if (titleLine < 0)
			return;

		final String mapName = event.getLine(titleLine + 1);
		final Player player = event.getPlayer();
		if (mapName == null) {
			player.sendMessage(Messager.getMessage("SIGN_NEED_MAP_NAME"));
			return;
		}
		final SkywarsMap map = Skywars.get().getMapManager().getMap(mapName);
		if (map == null) {
			player.sendMessage(Messager.getFormattedMessage("SIGN_MAP_NOT_FOUND", player, null, null, mapName));
			return;
		}

		final YamlConfiguration config = this.loadSignConfig();
		if (config == null) {
			player.sendMessage(Messager.getMessage("SIGN_COULD_NOT_SET_UP"));
			return;
		}
		final List<String> signsConfig = config.getStringList("signs");

		final Location loc = event.getBlock().getLocation();

		signsConfig.add(formatElement(loc, map));

		config.set("signs", signsConfig);
		this.saveSigns(config);

		this.signs.put(loc, map);

		player.sendMessage(Messager.getFormattedMessage("SIGN_SUCCESSFULLY_SET_UP", player, null, null, map.getName()));
		event.setCancelled(true);
		this.updateSign((Sign) event.getBlock().getState(), map);
	}

	public void saveSigns(YamlConfiguration config) {
		try {
			config.save(this.getSignConfigFile());
		} catch (final IOException e) {
			e.printStackTrace();
			Skywars.get().sendMessage(Messager.getMessage("SIGN_COULD_NOT_SAVE_SIGNS"));
		}
	}

	public void loadSigns() {
		final YamlConfiguration config = this.loadSignConfig();
		final List<String> signs = config.getStringList("signs");
		final List<String> newSigns = new ArrayList<String>(signs);
		Skywars.get().sendDebugMessage("&bLoading %s signs...", signs.size());
		for (final String s : signs) {
			final String[] splitted = s.split("[^\\w\\s-]");
			if (splitted.length < 4) {
				Skywars.get().sendDebugMessage(Messager.getFormattedMessage("SIGN_LOADING_ERROR_FORMAT", null, null, null, splitted.length)
						+ String.join(" - ", splitted));
				continue;
			}
			final String mapName = splitted[4];
			final String worldName = splitted[0];
			final World world = Bukkit.getWorld(worldName);
			if (world == null) {
				Skywars.get().sendMessage(Messager.getFormattedMessage("SIGN_COULD_NOT_FIND_WORLD", null, null, null, worldName));
				continue;
			}
			final Location loc = new Location( //
					world, //
					Double.parseDouble(splitted[1]), //
					Double.parseDouble(splitted[2]), //
					Double.parseDouble(splitted[3]));
			if (!(loc.getBlock().getState() instanceof Sign)) {
				newSigns.remove(s);
				Skywars.get().sendMessage(Messager.getFormattedMessage("SIGN_IS_NOT_SIGN", null, null, null, loc.toString()));
				continue;
			}

			if (this.signs.get(loc) != null) {
				Skywars.get().sendDebugMessage(Messager.getFormattedMessage("SIGN_DUPLICATED_SIGN", null, null, null, loc.toString()));
				continue;
			}

			final SkywarsMap map = Skywars.get().getMapManager().getMap(mapName);
			if (map == null) {
				newSigns.remove(s);
				Skywars.get().sendMessage(Messager.getFormattedMessage("SIGN_COULD_NOT_FIND_MAP", null, null, null, mapName));
				continue;
			}
			this.signs.put(loc, map);
			Skywars.get().sendDebugMessage(Messager.getFormattedMessage("SIGN_LOADED_SIGN", null, null, null, s));
		}

		config.set("signs", newSigns);
		this.saveSigns(config);
		this.updateSigns();
	}

	File getSignConfigFile() {
		return new File(Skywars.get().getDataFolder(), "signs.yml");
	}

	public YamlConfiguration loadSignConfig() {
		final File signsFile = this.getSignConfigFile();
		if (!signsFile.exists()) {
			try {
				signsFile.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
				Skywars.get().sendMessage(Messager.getMessage("SIGN_COULD_NOT_CREATE_SIGNS_FILE"));
				return null;
			}
		}
		return YamlConfiguration.loadConfiguration(signsFile);
	}

	public void updateSigns() {
		for (final Entry<Location, SkywarsMap> sign : this.signs.entrySet()) {
			final BlockState state = sign.getKey().getBlock().getState();
			if (!(state instanceof Sign)) {
				Skywars.get().sendDebugMessage(Messager.getFormattedMessage("SIGN_UPDATE_WARNING", null, null, null, sign.getKey().toString()));
			}
			final Sign signState = (Sign) state;
			this.updateSign(signState, sign.getValue());
		}
	}

	public void updateSign(Sign sign, SkywarsMap map) {
		Skywars.get().sendDebugMessage(Messager.getFormattedMessage("SIGN_UPDATING_SIGN", null, null, null, map.getName(), sign.getLocation().toString()));
		final ArrayList<Arena> arenas = ArenaManager.getArenasByMap(map);
		sign.setLine(0, Messager.color("&a%s", map.getName()));
		sign.setLine(1, Messager.color("&b%s &earenas", arenas.size()));
		sign.setLine(2, Messager.color("&b%s &eplayers",
				arenas.stream().map(arena -> arena.getAlivePlayerCount()).reduce(0, (a, b) -> a + b)));
		final Arena joinable = ArenaManager.getJoinableArenaByMap(map);
		final int count = joinable != null ? joinable.getAlivePlayerCount() : 0;
		if (joinable != null && count > 0) {
			sign.setLine(3, Messager.color("&b%s &eof &c%s &eplayers waiting", count, map.getMaxPlayers()));
		} else {
			sign.setLine(3, Messager.color("&bClick to join!"));
		}
		sign.update(true);
	}

}
