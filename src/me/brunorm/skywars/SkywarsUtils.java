package me.brunorm.skywars;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import com.cryptomorin.xseries.XMaterial;

public class SkywarsUtils {

	static String url = getUrl();

	public static String parseItemName(String text) {
		String name = Skywars.get().langConfig.getString("items." + text);
		if(Skywars.get().langConfig.getBoolean("items.show_context") == true) {
			String context = Skywars.get().langConfig.getString("items.context");
			if(context != null) {
				name = name + " " + Messager.color(context);
			}
		}
		return Messager.color(name);
	}
	
	public static void format(String text, Player player, Arena arena, SkywarsPlayer swp) {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		String strDate = formatter.format(date);

		text = text.replaceAll(getVariableCode("date"), strDate) //
				.replaceAll(getVariableCode("url"), url);

		if (arena != null) {
			List<SkywarsPlayer> players = new ArrayList<>(arena.getPlayers());
			players.removeIf(p -> p.isSpectator());
			text = text.replaceAll(getVariableCode("map"), arena.getName())
					.replaceAll(getVariableCode("players"), Integer.toString(players.size()))
					.replaceAll(getVariableCode("maxplayers"), Integer.toString(arena.getMaxPlayers()))
					.replaceAll(getVariableCode("minplayers"), Integer.toString(arena.getMinPlayers()))
					.replaceAll(getVariableCode("seconds"), Integer.toString(arena.countdown))
					.replaceAll(getVariableCode("status"), getStatus(arena));
		}

		if (swp != null) {
			text = text.replaceAll(getVariableCode("kills"), Integer.toString(swp.getKills()));
		}

	}

	public static String getVariableCode(String thing) {
		return String.format("%%%s%%", thing);
	}

	public static String getUrl() {
		FileConfiguration config = Skywars.get().getConfig();
		url = config.getString("url");
		if (url != null)
			return url;
		else
			return "www.example.com";
	}

	public static String getStatus(Arena arena) {
		YamlConfiguration config = Skywars.get().langConfig;
		String status;

		if (arena.getStatus() == ArenaStatus.STARTING) {
			status = config.getString("status.starting");
		} else if (arena.getStatus() == ArenaStatus.WAITING) {
			status = config.getString("status.waiting");
		} else {
			status = config.getString("status.ending");
		}

		return status;
	}

	public static void TeleportToLobby(Player player) {
		Location lobby = Skywars.get().getLobby();
		if (lobby == null) {
			return;
		}
		/*
		 * FileConfiguration config = Skywars.get().getConfig(); if(config.get("lobby")
		 * == null) { System.out.println("lobby not set"); return; } double x =
		 * config.getDouble("lobby.x"); double y = config.getDouble("lobby.y"); double z
		 * = config.getDouble("lobby.z"); String worldName =
		 * config.getString("lobby.world"); World world = Bukkit.getWorld(worldName);
		 * Location lobby = new Location(world, x, y, z);
		 */
		player.teleport(lobby);
	}

	public static void GiveBedItem(Player player) {
		ItemStack bed = new ItemStack(XMaterial.RED_BED.parseMaterial());
		ItemMeta meta = bed.getItemMeta();
		meta.setDisplayName(SkywarsUtils.parseItemName("leave"));
		bed.setItemMeta(meta);
		player.getInventory().setItem(8, bed);
	}

	@SuppressWarnings("deprecation")
	public static void ClearPlayer(Player player) {

		// make visible
		for (Player players : Bukkit.getOnlinePlayers()) {
			players.showPlayer(player);
		}

		// clear inventory
		player.getInventory().clear();
		player.getEquipment().clear();
		player.getInventory().setArmorContents(null);
		player.updateInventory();

		// clear player
		player.setGameMode(GameMode.ADVENTURE);
		player.setExp(0);
		player.setLevel(0);
		player.setFoodLevel(20);
		player.setHealth(20);
		player.setMaxHealth(20);
		player.setFlying(false);
		player.setAllowFlight(false);
		if(player.getFireTicks() > 0)
			player.setFireTicks(0);

		// clear potion effects
		for (PotionEffect e : player.getActivePotionEffects()) {
			player.removePotionEffect(e.getType());
		}
	}

	public static boolean JoinableCheck(Arena arena) {
		return JoinableCheck(arena, null);
	}

	public static boolean JoinableCheck(Arena arena, Player player) {
		if (player != null)
			if (Skywars.get().getPlayerArena(player) != null) {
				player.sendMessage("already joined");
				return false;
			}
		if (arena == null) {
			if (player != null)
				player.sendMessage("arena doesnt exist");
			return false;
		}
		if (arena.getStatus() == ArenaStatus.DISABLED) {
			if (player != null)
				player.sendMessage("arena is disabled");
			return false;
		}
		if (arena.getStatus() == ArenaStatus.ENDING) {
			if (player != null)
				player.sendMessage("arena is ending");
			return false;
		}
		if (arena.getStatus() == ArenaStatus.PLAYING) {
			if (player != null)
				player.sendMessage("arena is playing");
			return false;
		}
		if (arena.getMaxPlayers() <= 0) {
			if (player != null)
				player.sendMessage("max players not set");
			return false;
		}
		if (arena.getWorldName() == null) {
			if (player != null)
				player.sendMessage("world not set");
			return false;
		}
		if (arena.getPlayers().size() >= arena.getMaxPlayers()) {
			if (player != null)
				player.sendMessage("too much players");
			return false;
		}
		return true;
	}
}
