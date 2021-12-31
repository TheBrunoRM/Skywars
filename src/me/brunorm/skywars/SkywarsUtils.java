package me.brunorm.skywars;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsEvent;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class SkywarsUtils {
	
	public static String url = getUrl();
	public static String[] colorSymbols = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "f" };
	
	public static String format(String text, Player player, Arena arena, SkywarsPlayer swp) {
		return format(text, player, arena, swp, false);
	}
	
	public static String format(String text, Player player, Arena arena, SkywarsPlayer swp, boolean status) {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(Skywars.get().getConfig().getString("dateFormat"));
		String strDate = formatter.format(date);
		text = text.replaceAll(getVariableCode("date"), strDate)
				.replaceAll(getVariableCode("url"), url);
		
		if(player != null) {
			
			String balance = null;
			
			if(Skywars.get().getEconomy() != null) {
				balance = SkywarsUtils.formatDouble(Skywars.get().getEconomy().getBalance(player));
			} else {
				balance = "Vaultn't";
			}
			text = text.replaceAll(getVariableCode("coins"), balance)
					.replaceAll(getVariableCode("totalkills"),
				Integer.toString(Skywars.get().getPlayerTotalKills(player)))
					.replaceAll(getVariableCode("kit"),
							Skywars.get().getPlayerKit(player).getDisplayName());
		}
		
		if(arena != null) {
			// this prevents a stack overflow error
			if(!status) text = text.replaceAll(getVariableCode("status"),
					format(SkywarsUtils.getStatus(arena), player, arena, swp, true));
			
			SkywarsEvent event = arena.getNextEvent();
			String eventText;
			if(event != null) eventText = String.format("%s (%s)", event.getType(), event.getTime());
			else eventText = "No event";
			
			text = text.replaceAll(getVariableCode("map"), arena.getMap().getName())
					.replaceAll(getVariableCode("arena"), arena.getMap().getName())
					.replaceAll(getVariableCode("event"), eventText)
					.replaceAll(getVariableCode("players"), Integer.toString(arena.getPlayerCount()))
					.replaceAll(getVariableCode("maxplayers"), Integer.toString(arena.getMap().getMaxPlayers()))
					.replaceAll(getVariableCode("seconds"), Integer.toString(arena.getCountdown()));
		}
		
		if(swp != null) {
			text = text.replaceAll(getVariableCode("kills"), Integer.toString(swp.getKills()));
		}
		
		return text;
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
			return "www.skywars.com";
	}
	
	public static String parseItemName(String text) {
		YamlConfiguration config = Skywars.get().langConfig;
		String name = config.getString("items." + text);
		if(config.getBoolean("items.show_context") == true) {
			String context = config.getString("items.context");
			if(context != null) {
				name = name + " " + Messager.color(context);
			}
		}
		return Messager.color(name);
	}

	public static String getStatus(Arena arena) {
		YamlConfiguration config = Skywars.get().langConfig;
		
		switch(arena.getStatus()) {
			case WAITING:
				return config.getString("status.waiting");
			case STARTING:
				return config.getString("status.starting");
			case PLAYING:
				return config.getString("status.playing");
			case ENDING:
				return config.getString("status.ending");
			default:
				return "";
		}
	}
	
	public static void TeleportPlayerBack(Player player) {
		Location lobby = Skywars.get().getLobby();
		Location lastLocation = Skywars.get().playerLocations.get(player);
		if(lobby != null)
			player.getPlayer().teleport(lobby);
		else if (lastLocation != null) {	
			player.getPlayer().teleport(lastLocation);
			Skywars.get().playerLocations.remove(player);
		}
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
		
		player.resetPlayerTime();
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
		if (arena.getAllPlayersIncludingAliveAndSpectators().size() >= arena.getMap().getMaxPlayers()) {
			if (player != null)
				player.sendMessage("too much players");
			return false;
		}
		return true;
	}
	
	public static Location getCenteredLocation(Location loc) {
		return loc.clone().add(new Vector(0.5,0,0.5));
	}
	
	Location calculateClosestLocation(Location loc, ArrayList<Location> locations) {
		if(locations.size() <= 1) return loc;
		Location closest = locations.get(0);
		for(Location l : locations) {
			if(distance(loc.toVector(), l.toVector())
					< distance(loc.toVector(), closest.toVector())) {
				closest = l;
			}
		}
		return closest;
	}

	public static double distance(Vector vec1, Vector vec2) {
		double dx = vec2.getX() - vec1.getX();
		double dy = vec2.getY() - vec1.getY();
		double dz = vec2.getZ() - vec1.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	public static Block getTargetBlock(Player player, int range) {
		BlockIterator iter = new BlockIterator(player, range);
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
		return Color.fromRGB(
        		(int) Math.floor(Math.random() * 255),
        		(int) Math.floor(Math.random() * 255),
        		(int) Math.floor(Math.random() * 255));
	}
	
	public static void spawnRandomFirework(Location location) {
		if(location == null) return;
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect.Builder builder = FireworkEffect.builder();
        builder.withTrail().withFlicker().with(FireworkEffect.Type.BALL_LARGE)
        .withFade(SkywarsUtils.getRandomColor(),
        		SkywarsUtils.getRandomColor(),
        		SkywarsUtils.getRandomColor())
        .withColor(SkywarsUtils.getRandomColor(),
        		SkywarsUtils.getRandomColor(),
        		SkywarsUtils.getRandomColor());
        meta.addEffect(builder.build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
	}

	public static String formatDouble(double d) {
		return new DecimalFormat(Skywars.get().getConfig().getString("decimalFormat")).format(d);
	}
	
	public static void setPlayerInventory(Player player, String category) {
		ConfigurationSection itemsSection =
				Skywars.get().getConfig().getConfigurationSection("items." + category);
		
		ConfigurationSection itemTypes =
				Skywars.get().getConfig().getConfigurationSection("item_types");
		
		for(String slotName : itemsSection.getKeys(false)) {
			Object itemName = itemsSection.get(slotName);
			int slot = Integer.parseInt(slotName);
			String itemType = itemTypes.getString((String) itemName);
			Material material = Material.getMaterial(itemType);
			ItemStack item = new ItemStack(material);
			ItemMeta itemMeta = item.getItemMeta();
			String configName = Skywars.get().langConfig.getString("items." + itemName + ".name");
			if(Skywars.get().langConfig.getBoolean("items.show_context") == true) {
				String context = Skywars.get().langConfig.getString("items.context");
				if(context != null) {
					configName = configName + " " + Messager.color(context);
				}
			}
			itemMeta.setDisplayName(Messager.color(configName));
			List<String> itemLore = new ArrayList<String>();
			for(String loreLine : Skywars.get().langConfig.
				getStringList("items." + itemName + ".description")) {
				itemLore.add(Messager.color(loreLine));
			}
			itemMeta.setLore(itemLore);
			item.setItemMeta(itemMeta);
			player.getInventory().setItem(slot, item);
		}
	}
}
