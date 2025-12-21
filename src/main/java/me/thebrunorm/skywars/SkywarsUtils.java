/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import com.cryptomorin.xseries.XMaterial;
import me.clip.placeholderapi.PlaceholderAPI;
import me.thebrunorm.skywars.holograms.HologramController;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import mrblobman.sounds.Sounds;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SkywarsUtils {

	public static final String URL = getUrl();
	public static final String[] COLOR_SYMBOLS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
		"f"};

	public static String format(String text, Player player, Arena arena, SkywarsUser swp) {
		return format(text, player, arena, swp, false);
	}

	public static String format(String text, Player player, Arena arena, SkywarsUser swp, boolean status) {
		if (text == null)
			return "";
		final Date date = new Date();
		final String format = Skywars.get().getConfig().getString("dateFormat");
		if (format == null)
			return "";
		final SimpleDateFormat formatter = new SimpleDateFormat(format);
		final String strDate = formatter.format(date);
		if (Skywars.placeholders)
			text = PlaceholderAPI.setPlaceholders(player, text);
		text = text.replaceAll(getVariableCode("date"), strDate).replaceAll(getVariableCode("url"), URL);

		if (player != null) {

			final String balance = Skywars.get().getEconomy() != null
				? SkywarsUtils.formatDouble(Skywars.get().getEconomy().getBalance(player))
				: "Vaultn't";
			text = text.replaceAll(getVariableCode("coins"), balance).replaceAll(getVariableCode("money"), balance)
				.replaceAll(getVariableCode("balance"), balance).replaceAll(getVariableCode("economy"), balance)
				.replaceAll(getVariableCode("souls"), String.valueOf(Skywars.get().getPlayerSouls(player)))
				.replaceAll(getVariableCode("totalwins"), String.valueOf(Skywars.get().getPlayerTotalWins(player)))
				.replaceAll(getVariableCode("totalkills"),
					String.valueOf(Skywars.get().getPlayerTotalKills(player)))
				.replaceAll(getVariableCode("totaldeaths"),
					String.valueOf(Skywars.get().getPlayerTotalDeaths(player)))
				.replaceAll(getVariableCode("kit"), Skywars.get().getPlayerKit(player).getDisplayName());
		}

		if (arena != null) {
			// this prevents a stack overflow error
			if (!status)
				text = text.replaceAll(getVariableCode("status"),
					format(SkywarsUtils.getStatus(arena), player, arena, swp, true));

			text = text.replaceAll(getVariableCode("map"), arena.getMap().getName())
				.replaceAll(getVariableCode("arena"), arena.getMap().getName())
				.replaceAll(getVariableCode("event"), arena.getNextEventText())
				.replaceAll(getVariableCode("players"), Integer.toString(arena.getAlivePlayerCount()))
				.replaceAll(getVariableCode("maxplayers"), Integer.toString(arena.getMap().getSpawns().size()))
				.replaceAll(getVariableCode("seconds"), Integer.toString(arena.getCountdown()))
				.replaceAll(getVariableCode("count"), Integer.toString(arena.getCountdown()));
		}

		if (swp != null) {
			text = text.replaceAll(getVariableCode("kills"), Integer.toString(swp.getKills()));
		}

		return text;
	}

	public static String getVariableCode(String thing) {
		return String.format("%%%s%%", thing);
	}

	public static String getUrl() {
		final FileConfiguration config = Skywars.get().getConfig();
		final String url = config.getString("url");
		if (url != null)
			return url;
		return "www.example.com";
	}

	public static String parseItemName(String text) {
		final YamlConfiguration config = Skywars.langConfig;
		String name = config.getString("items." + text);
		if (config.getBoolean("items.show_context")) {
			final String context = config.getString("items.context");
			if (context != null) {
				name = name + " " + Messager.color(context);
			}
		}
		return Messager.color(name);
	}

	public static String getStatus(Arena arena) {
		switch (arena.getStatus()) {
			case WAITING:
				return Messager.get("status.waiting");
			case STARTING:
				return Messager.get("status.starting");
			case PLAYING:
				return Messager.get("status.playing");
			case RESTARTING:
				return Messager.get("status.restarting");
			default:
				return Messager.get("status.unknown");
		}
	}

	public static void teleportPlayerLobbyOrLastLocation(Player player) {
		teleportPlayerLobbyOrLastLocation(player, false);
	}

	public static boolean teleportPlayerLobbyOrLastLocation(Player player,
															boolean force) {
		final Location lobby = Skywars.get().getLobby();
		if (lobby != null) {
			player.getPlayer().teleport(lobby);
			return true;
		}

		final Location lastLocation = Skywars.get().playerLocations.get(player);
		if (lastLocation == null) {
			if (force) {
				player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
				return true;
			} else {
				player.sendMessage(Messager.get("could_not_send_back"));
				return false;
			}
		}
		player.getPlayer().teleport(lastLocation);
		Skywars.get().playerLocations.remove(player);
		return true;
	}

	public static void resetPlayerServer(Player player) {
		// synchronizes the player's time and weather with the server time and weather
		player.resetPlayerTime();
		player.resetPlayerWeather();
	}

	public static void clearPlayer(Player player, boolean sync) {
		clearPlayer(player);
		if (sync) {
			resetPlayerServer(player);
			clearPlayerScreen(player);
		}
	}

	public static void clearPlayerScreen(Player player) {
		Skywars.get().NMS().sendTitle(player, "", "", 0, 0, 0);
		Skywars.get().NMS().sendActionbar(player, "");
	}

	public static void clearPlayer(Player player) {

		// make visible
		for (final Player players : Bukkit.getOnlinePlayers()) {
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
		if (player.getFireTicks() > 0)
			player.setFireTicks(0);
		player.setFallDistance(0);
		player.setVelocity(new Vector(0, 0f, 0));

		// clear potion effects
		for (final PotionEffect e : player.getActivePotionEffects()) {
			player.removePotionEffect(e.getType());
		}
	}

	public static JoinProblem joinableCheck(Arena arena) {
		return getJoinProblems(arena, null);
	}

	public static JoinProblem getJoinProblems(Arena arena, Player player) {
		// TODO add messages
		if (player != null)
			if (Skywars.get().getPlayerArena(player) != null) {
				player.sendMessage("already joined");
				return JoinProblem.ALREADY_JOINED;
			}
		if (arena == null) {
			if (player != null)
				player.sendMessage("arena doesnt exist");
			return JoinProblem.ARENA_DOES_NOT_EXIST;
		}
		if (arena.getStatus() == ArenaStatus.DISABLED) {
			if (player != null)
				player.sendMessage("arena is disabled");
			return JoinProblem.ARENA_IS_DISABLED;
		}
		if (arena.getStatus() == ArenaStatus.RESTARTING) {
			if (player != null)
				player.sendMessage("arena is ending");
			return JoinProblem.ARENA_IS_ENDING;
		}
		if (arena.getStatus() == ArenaStatus.PLAYING) {
			if (player != null)
				player.sendMessage("arena is playing");
			return JoinProblem.ARENA_IS_PLAYING;
		}
		if (arena.getWorld() == null) {
			if (player != null)
				player.sendMessage("world not set");
			return JoinProblem.WORLD_NOT_SET;
		}
		final int spawns = arena.getMap().getSpawns().size();
		if (arena.getAlivePlayerCount() >= spawns) {
			if (player != null)
				player.sendMessage(
					Messager.color("this arena is full! (%s/%s players)", arena.getAlivePlayerCount(), spawns));
			return JoinProblem.ARENA_IS_FULL;
		}
		return null;
	}

	public static Location getCenteredLocation(Location loc) {
		return loc.clone().add(new Vector(0.5, 0, 0.5));
	}

	Location calculateClosestLocation(Location loc, ArrayList<Location> locations) {
		if (locations.size() <= 1)
			return loc;
		Location closest = locations.get(0);
		for (final Location l : locations) {
			if (distance(loc.toVector(), l.toVector()) < distance(loc.toVector(), closest.toVector())) {
				closest = l;
			}
		}
		return closest;
	}

	public static double distance(Vector vec1, Vector vec2) {
		final double dx = vec2.getX() - vec1.getX();
		final double dy = vec2.getY() - vec1.getY();
		final double dz = vec2.getZ() - vec1.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public static Block getTargetBlock(Player player, int range) {
		final BlockIterator iter = new BlockIterator(player, range);
		Block lastBlock = iter.next();
		while (iter.hasNext()) {
			lastBlock = iter.next();
			if (lastBlock.getType() == XMaterial.AIR.parseMaterial()) {
				continue;
			}
			break;
		}
		return lastBlock;
	}

	public static int getRandomSlot(Inventory inventory) {
		return (int) Math.floor(Math.random() * inventory.getSize() + 1) - 1;
	}

	public static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	public static Color getRandomColor() {
		return Color.fromRGB((int) Math.floor(Math.random() * 255), (int) Math.floor(Math.random() * 255),
			(int) Math.floor(Math.random() * 255));
	}

	public static void spawnRandomFirework(Location location) {
		if (location == null)
			return;
		final Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
		final FireworkMeta meta = firework.getFireworkMeta();
		final FireworkEffect.Builder builder = FireworkEffect.builder();
		builder.withTrail().withFlicker().with(FireworkEffect.Type.BALL_LARGE)
			.withFade(SkywarsUtils.getRandomColor(), SkywarsUtils.getRandomColor(), SkywarsUtils.getRandomColor())
			.withColor(SkywarsUtils.getRandomColor(), SkywarsUtils.getRandomColor(), SkywarsUtils.getRandomColor());
		meta.addEffect(builder.build());
		meta.setPower(1);
		firework.setFireworkMeta(meta);
	}

	public static String formatDouble(double d) {
		return new DecimalFormat(Skywars.get().getConfig().getString("decimalFormat")).format(d);
	}

	public static String getItemNameFromConfig(SkywarsItemType itemType) {
		String configName = Skywars.langConfig.getString("items." + itemType.name() + ".name");
		if (Skywars.langConfig.getBoolean("items.show_context")) {
			final String context = Skywars.langConfig.getString("items.context");
			if (context != null) {
				configName = configName + " " + Messager.color(context);
			}
		}
		return Messager.color(configName);
	}

	public static void setPlayerInventory(Player player, String category) {
		final ConfigurationSection itemsSection = Skywars.get().getConfig()
			.getConfigurationSection("items." + category);

		final ConfigurationSection itemTypes = Skywars.get().getConfig().getConfigurationSection("item_types");

		for (final String slotName : itemsSection.getKeys(false)) {
			String itemName = itemsSection.getString(slotName);
			if (itemName == null) {
				Skywars.get().sendDebugMessage("Item is null in slot %s for item_types", slotName);
				continue;
			}
			try {
				SkywarsItemType itemType = SkywarsItemType.valueOf(itemName);

				switch (itemType) {
					case START_GAME:
						if (!player.hasPermission("skywars.start"))
							continue;
						break;
					case STOP_GAME:
						if (!player.hasPermission("skywars.stop"))
							continue;
						break;
				}

				int slot;
				try {
					slot = Integer.parseInt(slotName);
				} catch (NumberFormatException ex) {
					Skywars.get().sendDebugMessage("Invalid inventory slot '%s' for items.%s", slotName, category);
					slot = player.getInventory().firstEmpty();
				}
				final String materialName = itemTypes.getString(itemType.name());
				Material material = XMaterial.matchXMaterial(materialName)
					.map(XMaterial::parseMaterial)
					.orElse(null);
				if (material == null) {
					Skywars.get().sendDebugMessage("Material is not defined in config for item_types.%s", itemType);
					// fallback
					material = XMaterial.BEDROCK.parseMaterial();
				}
				final ItemStack item = new ItemStack(material);
				final ItemMeta itemMeta = item.getItemMeta();
				final String configName = getItemNameFromConfig(itemType);
				itemMeta.setDisplayName(Messager.color(configName));
				final List<String> itemLore = new ArrayList<String>();
				for (final String loreLine : Skywars.langConfig.getStringList("items." + itemType.name() + ".description")) {
					itemLore.add(Messager.color(loreLine));
				}
				itemMeta.setLore(itemLore);
				item.setItemMeta(itemMeta);
				player.getInventory().setItem(slot, item);
			} catch (IllegalArgumentException ex) {
				Skywars.get().sendDebugMessage("Invalid item type: %s", itemName);
			}
		}
	}

	public static void playSound(Player player, String sound) {
		final String[] splitted = sound.split(";");
		player.playSound(player.getLocation(), Sounds.valueOf(splitted[0]).bukkitSound(),
			splitted.length > 1 ? Float.parseFloat(splitted[1]) : 1,
			splitted.length > 2 ? Float.parseFloat(splitted[2]) : 1);
	}

	public static void playSoundsFromConfig(Player player, String configLocation) {
		final List<String> list = Skywars.config.getStringList(configLocation);
		final String singleSound = Skywars.config.getString(configLocation);
		if (list.size() > 0)
			for (final String sound : list) {
				playSound(player, sound);
			}
		else if (singleSound != null) {
			playSound(player, singleSound);
		}
	}

	public static boolean checkClass(String name) {
		try {
			Class.forName(name);
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}

	public static String getHologramsAPIName(HologramController hologramController) {
		if (hologramController == null)
			return "None";
		final String name = hologramController.getClass().getName();
		if (name == null)
			return "None";
		final String[] bits = name.split(".");
		if (bits.length < 1)
			return "None";
		return bits[bits.length - 1];
	}

	public static <E> E mostFrequentElement(Iterable<E> iterable) {
		final Map<E, Integer> freqMap = new HashMap<>();
		E mostFreq = null;
		int mostFreqCount = -1;
		for (final E e : iterable) {
			Integer count = freqMap.get(e);
			freqMap.put(e, count = (count == null ? 1 : count + 1));
			// maintain the most frequent in a single pass.
			if (count > mostFreqCount) {
				mostFreq = e;
				mostFreqCount = count;
			}
		}
		return mostFreq;
	}
}
