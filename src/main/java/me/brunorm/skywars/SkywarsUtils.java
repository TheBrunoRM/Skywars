package me.brunorm.skywars;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.holograms.HologramController;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsUser;
import mrblobman.sounds.Sounds;

public class SkywarsUtils {

	public static final String URL = getUrl();
	public static final String[] COLOR_SYMBOLS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
			"f" };

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
		text = text.replaceAll(getVariableCode("date"), strDate).replaceAll(getVariableCode("url"), URL);

		if (player != null) {

			final String balance = Skywars.get().getEconomy() != null
					? SkywarsUtils.formatDouble(Skywars.get().getEconomy().getBalance(player))
					: "Vaultn't";
			text = text.replaceAll(getVariableCode("coins"), balance)
					.replaceAll(getVariableCode("souls"), Skywars.get().getPlayerSouls(player).toString())
					.replaceAll(getVariableCode("totalwins"), Skywars.get().getPlayerTotalWins(player).toString())
					.replaceAll(getVariableCode("totalkills"), Skywars.get().getPlayerTotalKills(player).toString())
					.replaceAll(getVariableCode("totaldeaths"), Skywars.get().getPlayerTotalDeaths(player).toString())
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
					.replaceAll(getVariableCode("maxplayers"), Integer.toString(arena.getMap().getMaxPlayers()))
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
		return "www.skywars.com";
	}

	public static String parseItemName(String text) {
		final YamlConfiguration config = Skywars.langConfig;
		String name = config.getString("items." + text);
		if (config.getBoolean("items.show_context") == true) {
			final String context = config.getString("items.context");
			if (context != null) {
				name = name + " " + Messager.color(context);
			}
		}
		return Messager.color(name);
	}

	public static String getStatus(Arena arena) {
		final YamlConfiguration config = Skywars.langConfig;

		switch (arena.getStatus()) {
		case WAITING:
			return config.getString("status.waiting");
		case STARTING:
			return config.getString("status.starting");
		case PLAYING:
			return config.getString("status.playing");
		case RESTARTING:
			return config.getString("status.restarting");
		default:
			return "";
		}
	}

	public static void teleportPlayerBack(Player player) {
		final Location lobby = Skywars.get().getLobby();
		final Location lastLocation = Skywars.get().playerLocations.get(player);
		if (lobby != null)
			player.getPlayer().teleport(lobby);
		else if (lastLocation != null) {
			player.getPlayer().teleport(lastLocation);
			Skywars.get().playerLocations.remove(player);
		}
	}

	@Deprecated
	public static void giveBedItem(Player player) {
		final ItemStack bed = new ItemStack(XMaterial.RED_BED.parseMaterial());
		final ItemMeta meta = bed.getItemMeta();
		meta.setDisplayName(SkywarsUtils.parseItemName("leave"));
		bed.setItemMeta(meta);
		player.getInventory().setItem(8, bed);
	}

	public static void resetPlayerServer(Player player) {
		player.resetPlayerTime();
		player.resetPlayerWeather();
	}

	public static void clearPlayer(Player player, boolean sync) {
		clearPlayer(player);
		if (sync)
			resetPlayerServer(player);
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

		// clear potion effects
		for (final PotionEffect e : player.getActivePotionEffects()) {
			player.removePotionEffect(e.getType());
		}

		// synchronizes the player's time with the server time
		player.resetPlayerTime();
	}

	public static boolean joinableCheck(Arena arena) {
		return joinableCheck(arena, null);
	}

	public static boolean joinableCheck(Arena arena, Player player) {
		// TODO add messages
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
		if (arena.getStatus() == ArenaStatus.RESTARTING) {
			if (player != null)
				player.sendMessage("arena is ending");
			return false;
		}
		if (arena.getStatus() == ArenaStatus.PLAYING) {
			if (player != null)
				player.sendMessage("arena is playing");
			return false;
		}
		if (arena.getMap().getMaxPlayers() <= 0) {
			if (player != null)
				player.sendMessage("max players not set");
			return false;
		}
		if (arena.getWorld() == null) {
			if (player != null)
				player.sendMessage("world not set");
			return false;
		}
		if (arena.getUsers().size() >= arena.getMap().getMaxPlayers()) {
			if (player != null)
				player.sendMessage("too much players");
			return false;
		}
		return true;
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

	public static String getItemNameFromConfig(String itemName) {
		String configName = Skywars.langConfig.getString("items." + itemName + ".name");
		if (Skywars.langConfig.getBoolean("items.show_context") == true) {
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
			final String itemName = itemsSection.getString(slotName);

			switch (itemName) {
			case "START_GAME":
				if (!player.hasPermission("skywars.start"))
					continue;
			case "STOP_GAME":
				if (!player.hasPermission("skywars.stop"))
					continue;
			}

			final int slot = Integer.parseInt(slotName);
			final String itemType = itemTypes.getString(itemName);
			final Material material = XMaterial.matchXMaterial(itemType).get().parseMaterial();
			if (material == null) {
				Skywars.get().sendMessage("material is null for inventory item: %s", itemType);
				continue;
			}
			final ItemStack item = new ItemStack(material);
			final ItemMeta itemMeta = item.getItemMeta();
			final String configName = getItemNameFromConfig(itemName);
			itemMeta.setDisplayName(Messager.color(configName));
			final List<String> itemLore = new ArrayList<String>();
			for (final String loreLine : Skywars.langConfig.getStringList("items." + itemName + ".description")) {
				itemLore.add(Messager.color(loreLine));
			}
			itemMeta.setLore(itemLore);
			item.setItemMeta(itemMeta);
			player.getInventory().setItem(slot, item);
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
