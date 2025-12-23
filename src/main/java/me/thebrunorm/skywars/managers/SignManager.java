// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.managers;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class SignManager implements Listener {

	HashMap<Location, SkywarsMap> signs = new HashMap<>();

	public HashMap<Location, SkywarsMap> getSigns() {
		return this.signs;
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
		if (mapName == null || mapName.isEmpty()) {
			player.sendMessage(MessageUtils.color("&cYou need to specify the map name in the line below!"));
			return;
		}
		final SkywarsMap map = Skywars.get().getMapManager().getMap(mapName);
		if (map == null) {
			player.sendMessage("&cCould not find a map with the name: &e" + mapName);
			return;
		}

		final YamlConfiguration config = this.loadSignConfig();
		if (config == null) {
			player.sendMessage(MessageUtils.color("&cCould not set up sign."));
			return;
		}
		final List<String> signsConfig = config.getStringList("signs");

		final Location loc = event.getBlock().getLocation();

		signsConfig.add(formatElement(loc, map));

		config.set("signs", signsConfig);
		this.saveSigns(config);

		this.signs.put(loc, map);

		player.sendMessage(MessageUtils.color("&eSuccessfully set up sign for map &b" + map.getName()));
		event.setCancelled(true);
		this.updateSign((Sign) event.getBlock().getState(), map);
	}

	public YamlConfiguration loadSignConfig() {
		final File signsFile = this.getSignConfigFile();
		if (!signsFile.exists()) {
			try {
				signsFile.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
				Skywars.get().sendMessage("&cCould not create &bsigns.yml &cfile!");
				return null;
			}
		}
		return YamlConfiguration.loadConfiguration(signsFile);
	}

	public static String formatElement(Location loc, SkywarsMap map) {
		return loc.getWorld().getName() + ";" + //
				loc.getBlockX() + ";" + //
				loc.getBlockY() + ";" + //
				loc.getBlockZ() + ";" + //
				map.getName();
	}

	public void saveSigns(YamlConfiguration config) {
		try {
			config.save(this.getSignConfigFile());
		} catch (final IOException e) {
			e.printStackTrace();
			Skywars.get().sendMessage("&cCould not save signs!");
		}
	}

	public void updateSign(Sign sign, SkywarsMap map) {
		Skywars.get().sendDebugMessage("&eUpdating sign (&a%s&e): &b%s", map.getName(), sign.getLocation());
		final ArrayList<Arena> arenas = ArenaManager.getArenasByMap(map);
		sign.setLine(0, MessageUtils.color("&a%s", map.getName()));
		sign.setLine(1, MessageUtils.color("&b%s &earenas", arenas.size()));
		sign.setLine(2, MessageUtils.color("&b%s &eplayers",
				arenas.stream().map(Arena::getAlivePlayerCount).reduce(0, Integer::sum)));
		final Arena joinable = ArenaManager.getJoinableArenaByMap(map);
		final int count = joinable != null ? joinable.getAlivePlayerCount() : 0;
		if (joinable != null && count > 0) {
			sign.setLine(3, MessageUtils.color("&b%s &eof &c%s &eplayers waiting", count, map.getMaxPlayers()));
		} else {
			sign.setLine(3, MessageUtils.color("&bClick to join!"));
		}
		sign.update(true);
	}

	File getSignConfigFile() {
		return new File(Skywars.get().getDataFolder(), "signs.yml");
	}

	public void loadSigns() {
		final YamlConfiguration config = this.loadSignConfig();
		final List<String> signs = config.getStringList("signs");
		final List<String> newSigns = new ArrayList<>(signs);
		Skywars.get().sendDebugMessage("&bLoading %s signs...", signs.size());
		for (final String s : signs) {
			final String[] splitted = s.split("[^\\w\\s-]");
			if (splitted.length < 4) {
				Skywars.get().sendDebugMessage("&4Error loading signs.yml: &cincorrectly formatted sign (&4%s&c): &b"
						+ String.join(" - ", splitted), splitted.length);
				continue;
			}
			final String mapName = splitted[4];
			final String worldName = splitted[0];
			final World world = Bukkit.getWorld(worldName);
			if (world == null) {
				Skywars.get().sendMessage("&4Error loading signs.yml: &ccould not find world: &b" + worldName);
				continue;
			}
			final Location loc = new Location( //
					world, //
					Double.parseDouble(splitted[1]), //
					Double.parseDouble(splitted[2]), //
					Double.parseDouble(splitted[3]));
			if (!(loc.getBlock().getState() instanceof Sign)) {
				newSigns.remove(s);
				Skywars.get().sendMessage("&4Error loading signs.yml: &csign is not sign: &b" + loc);
				continue;
			}

			if (this.signs.get(loc) != null) {
				Skywars.get().sendDebugMessage("&4Error loading signs.yml: &cduplicated sign: &b" + loc);
				continue;
			}

			final SkywarsMap map = Skywars.get().getMapManager().getMap(mapName);
			if (map == null) {
				newSigns.remove(s);
				Skywars.get().sendMessage("&4Error loading signs.yml: &ccould not find map: &b" + mapName);
				continue;
			}
			this.signs.put(loc, map);
			Skywars.get().sendDebugMessage("&eLoaded sign: &b", s);
		}

		config.set("signs", newSigns);
		this.saveSigns(config);
		this.updateSigns();
	}

	public void updateSigns() {
		for (final Entry<Location, SkywarsMap> sign : this.signs.entrySet()) {
			final BlockState state = sign.getKey().getBlock().getState();
			if (!(state instanceof Sign signState)) {
				Skywars.get().sendDebugMessage("&4Error updating signs: &csign is not sign: &b" + sign.getKey());
				continue;
			}
			this.updateSign(signState, sign.getValue());
		}
	}

}
