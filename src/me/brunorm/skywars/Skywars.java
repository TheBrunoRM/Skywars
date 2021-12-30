package me.brunorm.skywars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.NMS.NMSHandler;
import me.brunorm.skywars.commands.ForceStartCommand;
import me.brunorm.skywars.commands.LeaveCommand;
import me.brunorm.skywars.commands.MainCommand;
import me.brunorm.skywars.commands.StartCommand;
import me.brunorm.skywars.commands.WhereCommand;
import me.brunorm.skywars.events.DisableWeather;
import me.brunorm.skywars.events.Events;
import me.brunorm.skywars.events.InteractEvent;
import me.brunorm.skywars.events.MapSetup;
import me.brunorm.skywars.events.MessageSound;
import me.brunorm.skywars.events.ProjectileTrails;
import me.brunorm.skywars.events.SignEvents;
import me.brunorm.skywars.menus.GamesMenu;
import me.brunorm.skywars.menus.KitsMenu;
import me.brunorm.skywars.menus.MapMenu;
import me.brunorm.skywars.menus.MapSetupMenu;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.Kit;
import me.brunorm.skywars.structures.SkywarsMap;
import me.brunorm.skywars.structures.SkywarsPlayer;
import net.milkbowl.vault.economy.Economy;

@SuppressWarnings("deprecation")
public class Skywars extends JavaPlugin {
	
	// get plugin data
	public String name = getDescription().getName();
	public String version = getDescription().getVersion();
	public List<String> authors = getDescription().getAuthors();
	private String prefix = Messager.colorFormat("&6[&e%s&6]&e", name);
	public static String kitsPath;
	public static String mapsPath;
	public static String schematicsPath;
	public static String playersPath;
	
	public static boolean economyEnabled;
	Economy economy;
	RegisteredServiceProvider<Economy> economyProvider;
	
	public Economy getEconomy() {
		return economy;
	}
	
	public static YamlConfiguration config;
	
	public YamlConfiguration scoreboardConfig;
	public YamlConfiguration langConfig;
	public YamlConfiguration lobbyConfig;

	public HashMap<Player, Location> playerLocations =
			new HashMap<Player, Location>();
	
    private String packageName;
    private String serverPackageVersion;
    private NMSHandler nmsHandler;

	public static Skywars plugin;
	public static Skywars get() {
		return plugin;
	}

	private Location lobby;

	public void setLobby(Location lobby) {
		lobbyConfig.set("lobby.x", lobby.getX());
		lobbyConfig.set("lobby.y", lobby.getY());
		lobbyConfig.set("lobby.z", lobby.getZ());
		lobbyConfig.set("lobby.world", lobby.getWorld().getName());
		ConfigurationUtils.saveConfiguration(lobbyConfig, "lobby.yml");
		this.lobby = lobby;
	}

	public Location getLobby() {
		return this.lobby;
	}
	
	public void setLobbyFromConfig() {
		if (lobbyConfig.get("lobby") != null) {
			String worldName = lobbyConfig.getString("lobby.world");
			if(worldName != null) {				
				World world = Bukkit.getWorld(worldName);
				if(world != null) {					
					double x = lobbyConfig.getDouble("lobby.x");
					double y = lobbyConfig.getDouble("lobby.y");
					double z = lobbyConfig.getDouble("lobby.z");
					this.lobby = new Location(world, x, y, z);
				} else sendMessage("&cCould not set main lobby in world &b%s", worldName);
			}
		} else this.lobby = null;
	}
	
	public void onEnable() {
		
		// set stuff
		plugin = this;
		mapsPath = getDataFolder() + "/maps";
		kitsPath = getDataFolder() + "/kits";
		schematicsPath = getDataFolder() + "/schematics";
		playersPath = getDataFolder() + "/players";
		
		packageName = this.getServer().getClass().getPackage().getName();
		serverPackageVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		nmsHandler = new NMSHandler();
		
		// load stuff
		loadConfig();
		loadCommands();
		loadEvents();
		loadMaps();
		loadKits();
		
		economyEnabled = Skywars.get().getConfig().getBoolean("economy.enabled");
		if(economyEnabled && setupEconomy()) {
			sendMessage("&eHooked with Vault!");
			sendMessage("&eEconomy service provider: &a%s",
					economyProvider.getPlugin().getName());
		}
		
		// done
		sendMessage("&ahas been enabled: &bv%s", version);
		
		if(!Skywars.get().getConfig().getBoolean("debug.disableUpdateTask"))
			Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					for (Player player : Bukkit.getOnlinePlayers()) {
						SkywarsScoreboard.update(player);
						SkywarsActionbar.update(player);
					}
				}
			}, 0L, Skywars.get().getConfig().getLong("taskUpdateInterval")*20);
	}
	
	public void onDisable() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			Arena arena = getPlayerArena(player);
			if (arena != null) {
				arena.kick(player);
			}
		}
		MapSetupMenu.currentMaps.forEach((player, arena) -> {
			player.getInventory().removeItem(MapSetup.item);
			SkywarsUtils.TeleportPlayerBack(player);
		});
		getLogger().info(Messager.colorFormat("%s &eStopping arenas...", prefix));
		for (Arena arena : arenas) {
			arena.restart();
		}
		arenas.clear();
	}

	public NMSHandler NMS() {
		return nmsHandler;
	}
	
	public String getServerPackageVersion() {
		return serverPackageVersion;
	}
	
	public void Reload() {
		loadConfig();
		loadMaps();
		loadKits();
	}

    private boolean setupEconomy() {
    	if(!economyEnabled) return false;
        economyProvider = getServer().getServicesManager()
        		.getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)   	
        	economy = economyProvider.getProvider();
        return economy != null;
    }
    
	public void loadEvents() {
		FileConfiguration config = getConfig();
		PluginManager pluginManager = getServer().getPluginManager();
		if(config.getBoolean("signsEnabled")) {
			pluginManager.registerEvents(new SignEvents(), this);
		}
		if(config.getBoolean("messageSounds")) {
			pluginManager.registerEvents(new MessageSound(), this);
		}
		if(config.getBoolean("disableWeather")) {			
			pluginManager.registerEvents(new DisableWeather(), this);
		}
		if(config.getBoolean("debug.projectileTests")) {
			pluginManager.registerEvents(new ProjectileTrails(), this);
		}
		Listener[] listeners = {
				new InteractEvent(),
				new Events(),
				new GamesMenu(),
				new MapMenu(),
				new KitsMenu(),
				new MapSetup(),
				new ChestManager(),
				new MapSetupMenu(),
				new PlayerInventoryManager(),
		};
		for(Listener listener : listeners) {			
			pluginManager.registerEvents(listener, this);
		}
	}
	
	public void loadCommands() {
		HashMap<String, CommandExecutor> cmds = new HashMap<String, CommandExecutor>();
		cmds.put("skywars", new MainCommand());
		cmds.put("where", new WhereCommand());
		cmds.put("start", new StartCommand());
		cmds.put("forcestart", new ForceStartCommand());
		cmds.put("leave", new LeaveCommand());
		for(String cmd : cmds.keySet()) {
			if(!getConfig().getStringList("disabledCommands").contains(cmd)) {
				Bukkit.getConsoleSender().sendMessage(
					Messager.colorFormat("%s &eLoading command &a%s&e...", prefix, cmd));
				this.getCommand(cmd).setExecutor(cmds.get(cmd));
			} else Bukkit.getConsoleSender().sendMessage(
				Messager.colorFormat("%s &7Skipping command &c%s&e...", prefix, cmd));
		}
	}
	
	// maps
	
	private ArrayList<SkywarsMap> maps = new ArrayList<SkywarsMap>();
	
	public SkywarsMap getMap(String name) {
		for(SkywarsMap map : maps) {
			if(map.getName().equalsIgnoreCase(name)) return map;
		}
		return null;
	}
	
	public SkywarsMap getRandomMap() {
		return maps.get((int) (Math.floor(Math.random() * maps.size()+1)-1));
	}
	
	public ArrayList<SkywarsMap> getMaps() {
		return maps;
	}
	
	public Arena getArenaByMap(SkywarsMap map) {
		for(Arena arena : arenas) {
			if(arena.getStatus() != ArenaStatus.WAITING &&
					arena.getStatus() != ArenaStatus.STARTING) continue;
			if(!arena.isJoinable()) continue;
			if(arena.getMap() == map) return arena;
		}
		return null;
	}
	
	public ArrayList<Arena> getArenasByMap(SkywarsMap map) {
		return getArenas().stream()
				.filter(arena -> arena.getMap() == map)
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public void joinMap(SkywarsMap map, Player player) {
		Arena arena = getArenaByMap(map);
		if(arena == null) {
			arena = createNewArena(map);
		}
		System.out.println("joining player to map");
		arena.joinPlayer(player);
	}
	
	public void joinRandomMap(Player player) {
		SkywarsMap map = getRandomMap();
		joinMap(map, player);
	}
	
	Arena createNewArena(SkywarsMap map) {
		Arena arena = new Arena(map);
		arena.setWorld(getWorld(map));
		arena.setLocation(getNextFreeLocation());
		arena.pasteSchematic();
		arenas.add(arena);
		System.out.println("creating new arena for map " + map.getName());
		return arena;
	}
	
	public void clearArena(Arena arena) {
		System.out.println("removing arena " + arena.getMap().getName());
		arenas.remove(arena);
		arena = null;
	}

	public Location getNextFreeLocation() {
		int x = 0;
		int z = 0;

		for (int i = 0; i < arenas.size(); i++) {	
			if (i % 2 == 0) {
				z += 1;
			} else {
				x += 1;
			}
		}
		return new Location(Bukkit.getWorld(config.getString("arenas.world")),
				x*config.getInt("arenas.separation"),
				config.getInt("arenas.Y"),
				z*config.getInt("arenas.separation"));
	}
	
	public void loadMaps() {
		String arenasMethod = config.getString("arenasMethod");
		maps.clear();
		File folder = new File(mapsPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		if(folder.listFiles().length <= 0) {
			Bukkit.getConsoleSender().sendMessage(Messager.color("&eSetting up default map."));
			ConfigurationUtils.copyDefaultContentsToFile("maps/MiniTrees.yml", new File(mapsPath, "MiniTrees.yml"));
		}
		File schematics = new File(schematicsPath);
		if(!schematics.exists()) schematics.mkdir();
		if(schematics.listFiles().length <= 0) {
			Bukkit.getConsoleSender().sendMessage(Messager.color("&eSetting up default schematic."));
			ConfigurationUtils.copyDefaultContentsToFile("schematics/mini_trees.schematic",
					new File(schematicsPath, "mini_trees.schematic"));
		}
		for (File file : folder.listFiles()) {
			String name = file.getName().replaceFirst("[.][^.]+$", "");
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			ConfigurationUtils.createMissingKeys(config,
					ConfigurationUtils.getDefaultConfig("maps/MiniTrees.yml"));
			
			// create map and set values from config
			SkywarsMap map = new SkywarsMap(name,
					config.getString("schematic"),
					config.getInt("minPlayers"),
					config.getInt("maxPlayers"),
					config.getInt("teamSize"));
			
			map.setConfig(config);
			
			if(arenasMethod.equalsIgnoreCase("SINGLE_ARENA")) {
				
				map.setWorldName(config.getString("world"));
				map.setSchematic(config.getString("schematic"));
				map.setLocation(
						ConfigurationUtils.getLocationConfig(map.getWorldName(),
								config.getConfigurationSection("location")));
			}
			
			World world = getWorld(map);
			
			// this will ignore spawns that are outside max players range
			for (int i = 0; i < map.getMaxPlayers(); i++) {
				if (config.get(String.format("spawn.%s", i)) == null)
					continue;
				double x = config.getDouble(String.format("spawn.%s.x", i));
				double y = config.getDouble(String.format("spawn.%s.y", i));
				double z = config.getDouble(String.format("spawn.%s.z", i));
				Location location = new Location(world, x, y, z);
				map.getSpawns().put(i, location);
			}
			
			maps.add(map);
			sendMessage("&eLoaded map: &a%s", map.getName());
		}
	}
	
	private World getWorld(SkywarsMap map) {
		String worldName = map.getWorldName();
		if(worldName == null)
			worldName = Skywars.config.getString("arenas.world");
		return Bukkit.getWorld(worldName);
	}

	// kits
	
	private ArrayList<Kit> kits = new ArrayList<Kit>();
	
	public ArrayList<Kit> getKits() {
		return this.kits;
	}
	
	public void loadKits() {
		kits.clear();
		File folder = new File(kitsPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		if(folder.listFiles().length <= 0) {
			Bukkit.getConsoleSender().sendMessage(Messager.color("&eSetting up default kit."));
			ConfigurationUtils.copyDefaultContentsToFile("kits/default.yml", new File(kitsPath, "default.yml"));
		}
		for (File file : folder.listFiles()) {
			String name = file.getName().replaceFirst("[.][^.]+$", "");
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			ConfigurationUtils.createMissingKeys(config,
					ConfigurationUtils.getDefaultConfig("kits/default.yml"));
			
			// create kit and set values from config
			Kit kit = new Kit(name);
			kit.setConfig(config);
			kit.setFile(file);
			kit.setDisplayName(config.getString("name"));
			
			// load items
			List<String> configItems = config.getStringList("items");
			ItemStack[] items = new ItemStack[configItems.size()];
			for(int i = 0; i < configItems.size(); i++) {
				String string = configItems.get(i);
				if(string.split(";").length > 1) {
					String item = string.split(";")[0];
					int amount = Integer.parseInt(string.split(";")[1]);
					items[i] = new ItemStack(XMaterial.valueOf(item).parseMaterial(), amount);
				} else {
					items[i] = XMaterial.valueOf(string).parseItem();
				}
			}
			
			kit.setItems(items);
			kit.setIcon(XMaterial.valueOf(config.getString("icon")).parseItem());
			kit.setPrice(config.getInt("price"));
			
			// add kit to the arena list
			kits.add(kit);
			sendMessage("&eLoaded kit: &a%s", kit.getName());
		}
	}
	
	public static void createBigCase(Location location, XMaterial material) {
		int[][] blocks = {
				// base
				{-1,-1,-1},{0,-1,-1},{1,-1,-1},
				{-1,-1,0},{0,-1,0},{1,-1,0},
				{-1,-1,1},{0,-1,1},{1,-1,1},
				
				// top
				{-1,3,1},{0,3,-1},{1,3,-1},
				{-1,3,0},{0,3,0},{1,3,0},
				{-1,3,1},{0,3,1},{1,3,1},
				
				// left wall
				{2,0,-1},{2,0,0},{2,0,1},
				{2,1,-1},{2,1,0},{2,1,1},
				{2,2,-1},{2,2,0},{2,2,1},
				
				// front wall
				{-1,0,2},{0,0,2},{1,0,2},
				{-1,1,2},{0,1,2},{1,1,2},
				{-1,2,2},{0,2,2},{1,2,2},
				
				// right wall
				{-2,0,-1},{-2,0,0},{-2,0,1},
				{-2,1,-1},{-2,1,0},{-2,1,1},
				{-2,2,-1},{-2,2,0},{-2,2,1},
				
				// back wall
				{-1,0,-2},{0,0,-2},{1,0,-2},
				{-1,1,-2},{0,1,-2},{1,1,-2},
				{-1,2,-2},{0,2,-2},{1,2,-2},
		};
		int[][] airBlocks = {
				{-1,0,-1},{0,0,-1},{1,0,-1},
				{-1,0,0},{0,0,0},{1,0,0},
				{-1,0,1},{0,0,1},{1,0,1},
				
				{-1,1,-1},{0,1,-1},{1,1,-1},
				{-1,1,0},{0,1,0},{1,1,0},
				{-1,1,1},{0,1,1},{1,1,1},
				
				{-1,2,-1},{0,2,-1},{1,2,-1},
				{-1,2,0},{0,2,0},{1,2,0},
				{-1,2,1},{0,2,1},{1,2,1},
		};
		for (int[] relative : airBlocks) {
			Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(XMaterial.AIR.parseMaterial());
		}
		for (int[] relative : blocks) {
			Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(material.parseMaterial());
			if(!XMaterial.isNewVersion()) {					
				block.setData(material.getData());
			}
		}
	}
	
	public static void createCase(Location location, XMaterial material) {
		if(config.getBoolean("debug.bigCases")) {
			createBigCase(location, material);
			return;
		}
		int[][] blocks = {
				// first layer
				{ -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 },
				// second layer
				{ -1, 1, 0 }, { 1, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 },
				// third layer
				{ -1, 2, 0 }, { 1, 2, 0 }, { 0, 2, -1 }, { 0, 2, 1 },
				// base and top
				{ 0, -1, 0 }, { 0, 3, 0 },
				// base joints
				{ -1, -1, 0 }, { 1, -1, 0 }, { 0, -1, -1 }, { 0, -1, 1 },
				// top joints
				{ -1, 3, 0 }, { 1, 3, 0 }, { 0, 3, -1 }, { 0, 3, 1 },
		};
		int[][] airBlocks = {
				{ 0, 0, 0 }, { 0, 1, 0 }, { 0, 2, 0 }
		};
		for (int[] relative : airBlocks) {
			Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(XMaterial.AIR.parseMaterial());
		}
		for (int[] relative : blocks) {
			Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(material.parseMaterial());
			if(!XMaterial.isNewVersion()) {					
				block.setData(material.getData());
			}
		}
	}
	
	// arenas
	
	private ArrayList<Arena> arenas = new ArrayList<Arena>();

	public ArrayList<Arena> getArenas() {
		return this.arenas;
	}
	
	public Arena getRandomJoinableArena() {
		for (Arena arena : arenas) {
			if (arena.isJoinable()) {
				return arena;
			}
		}
		return null;
	}
	
	public Arena getPlayerArena(Player player) {
		for (int i = 0; i < arenas.size(); i++) {
			List<SkywarsPlayer> players = arenas.get(i).getAllPlayersIncludingAliveAndSpectators();
			for (int j = 0; j < players.size(); j++) {
				if (players.get(j).getPlayer().equals(player)) {
					return arenas.get(i);
				}
			}
		}
		return null;
	}
	
	public HashMap<Player, YamlConfiguration> playerConfigurations =
			new HashMap<Player, YamlConfiguration>();
	
	public File getPlayerConfigFile(Player player) {
		return new File(playersPath, player.getUniqueId() + ".yml");
	}
	
	public File getSchematicFile(String schematicName) {
		File schematic = new File(schematicsPath, schematicName);
		if(!schematic.exists())
			try {
				schematic.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return schematic;
	}
	
	public YamlConfiguration getPlayerConfig(Player player) {
		if(playerConfigurations.get(player) != null)
			return playerConfigurations.get(player);
		File folder = new File(playersPath);
		if(!folder.exists()) folder.mkdir();
		File file = getPlayerConfigFile(player);
		if(!file.exists())
			ConfigurationUtils.copyDefaultContentsToFile("players/default.yml", file);
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		playerConfigurations.put(player, config);
		return config;
	}
	
	public void savePlayerConfig(Player player) {
		try {
			File file = getPlayerConfigFile(player);
			YamlConfiguration config = getPlayerConfig(player);
			config.save(file);
			ConfigurationUtils.createMissingKeys(config,
					ConfigurationUtils.getDefaultConfig("players/default.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Kit getKit(String name) {
		for (int i = 0; i < kits.size(); i++) {
			if (kits.get(i).getName().equals(name) || kits.get(i).getDisplayName().equals(name)) {
				return kits.get(i);
			}
		}
		return null;
	}

	public void setPlayerKit(Player player, Kit kit) {
		getPlayerConfig(player).set("kit", kit.getName());
		savePlayerConfig(player);
	}
	
	public Kit getPlayerKit(Player player) {
		String kitName = getPlayerConfig(player).getString("kit");
		Kit kit = getKit(kitName);
		return kit;
	}

	public Integer getPlayerTotalKills(Player player) {
		return getPlayerConfig(player).getInt("stats.solo.kills");
	}
	
	public void setPlayerTotalKills(Player player, int kills) {
		getPlayerConfig(player).set("stats.solo.kills", kills);
	}
	
	public void incrementPlayerTotalKills(Player player) {
		setPlayerTotalKills(player, getPlayerTotalKills(player)+1);
	}
	
	public Integer getPlayerTotalDeaths(Player player) {
		return getPlayerConfig(player).getInt("stats.solo.deaths");
	}
	
	public void setPlayerTotalDeaths(Player player, int kills) {
		getPlayerConfig(player).set("stats.solo.deaths", kills);
	}
	
	public void incrementPlayerTotalDeaths(Player player) {
		setPlayerTotalKills(player, getPlayerTotalKills(player)+1);
	}
	
	public void loadConfig() {
		
		// config.yml
		config = ConfigurationUtils.loadConfiguration("config.yml", "resources/config.yml");
		
		// scoreboard.yml
		scoreboardConfig = ConfigurationUtils.loadConfiguration("scoreboard.yml", "resources/scoreboard.yml");
		
		// lang.yml
		langConfig = ConfigurationUtils.loadConfiguration("lang.yml", "resources/lang.yml");
		
		// lobby.yml
		lobbyConfig = ConfigurationUtils.loadConfiguration("lobby.yml", "resources/lobby.yml");
		
		// load lobby
		setLobbyFromConfig();
	}
    
    public void sendMessage(String text, Object... format) {
    	Bukkit.getConsoleSender().sendMessage(prefix + " " + Messager.colorFormat(text, format));
    }
}
