package me.brunorm.skywars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.commands.ForceStartCommand;
import me.brunorm.skywars.commands.LeaveCommand;
import me.brunorm.skywars.commands.MainCommand;
import me.brunorm.skywars.commands.StartCommand;
import me.brunorm.skywars.commands.WhereCommand;
import me.brunorm.skywars.events.DisableWeather;
import me.brunorm.skywars.events.Events;
import me.brunorm.skywars.events.InteractEvent;
import me.brunorm.skywars.events.MessageSound;
import me.brunorm.skywars.events.ProjectileTrails;
import me.brunorm.skywars.events.SetupEvents;
import me.brunorm.skywars.events.SignEvents;
import me.brunorm.skywars.holograms.DecentHologramsController;
import me.brunorm.skywars.holograms.HologramController;
import me.brunorm.skywars.holograms.HolographicDisplaysNewController;
import me.brunorm.skywars.holograms.HolographicDisplaysOldController;
import me.brunorm.skywars.menus.ConfigMenu;
import me.brunorm.skywars.menus.GameOptionsMenu;
import me.brunorm.skywars.menus.GamesMenu;
import me.brunorm.skywars.menus.KitsMenu;
import me.brunorm.skywars.menus.MapMenu;
import me.brunorm.skywars.menus.PlayerInventoryManager;
import me.brunorm.skywars.nms.ReflectionNMS;
import me.brunorm.skywars.schematics.SchematicHandler;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.Kit;
import me.brunorm.skywars.structures.SkywarsMap;
import me.brunorm.skywars.structures.SkywarsUser;
import net.milkbowl.vault.economy.Economy;

@SuppressWarnings("deprecation")
public class Skywars extends JavaPlugin {

	// get plugin data
	PluginDescriptionFile pdf = this.getDescription();
	public String name = this.pdf.getName();
	public String version = this.pdf.getVersion();
	public List<String> authors = this.pdf.getAuthors();
	private final String prefix = Messager.colorFormat("&6[&e%s&6]&e", this.name);
	private final String debugPrefix = Messager.colorFormat("&7[&c%s&7]&e", this.name);
	public static String kitsPath;
	public static String worldsPath;
	public static String mapsPath;
	public static String schematicsPath;
	public static String playersPath;

	public static boolean holograms = false;
	HologramController hologramController;
	public static boolean economyEnabled;
	Economy economy;
	RegisteredServiceProvider<Economy> economyProvider;

	public Economy getEconomy() {
		return this.economy;
	}

	public static YamlConfiguration config;
	public static YamlConfiguration scoreboardConfig;
	public static YamlConfiguration langConfig;
	public static YamlConfiguration lobbyConfig;

	public HashMap<Player, Location> playerLocations = new HashMap<Player, Location>();

	private String packageName;
	private String serverPackageVersion;
	private ReflectionNMS nmsHandler;

	public static Skywars plugin;

	public static Skywars get() {
		return plugin;
	}

	public Skywars() {
		super();
	}

	protected Skywars(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
		super(loader, description, dataFolder, file);
	}

	public File file() {
		return this.getFile();
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
		if (lobbyConfig == null || lobbyConfig.get("lobby") == null) {
			this.lobby = null;
			return;
		}
		final String worldName = lobbyConfig.getString("lobby.world");
		if (worldName != null) {
			final World world = Bukkit.getWorld(worldName);
			if (world != null) {
				final double x = lobbyConfig.getDouble("lobby.x");
				final double y = lobbyConfig.getDouble("lobby.y");
				final double z = lobbyConfig.getDouble("lobby.z");
				this.lobby = new Location(world, x, y, z);
			} else
				this.sendMessage("&cLobby world (&b%s&c) does not exist!", worldName);
		}
	}

	boolean updated = false;

	@Override
	public void onEnable() {

		// set stuff
		plugin = this;
		worldsPath = this.getDataFolder() + "/worlds";
		mapsPath = this.getDataFolder() + "/maps";
		kitsPath = this.getDataFolder() + "/kits";
		schematicsPath = this.getDataFolder() + "/schematics";
		playersPath = this.getDataFolder() + "/players";

		this.packageName = this.getServer().getClass().getPackage().getName();
		this.serverPackageVersion = this.packageName.substring(this.packageName.lastIndexOf('.') + 1);

		// load stuff
		if (!this.loadConfig()) {
			this.sendMessage("Could not load configuration files! Disabling plugin.");
			this.setEnabled(false);
			return;
		}

		if (!config.getBoolean("disableUpdates") && SkywarsUpdater.update(config.getBoolean("autoUpdate"))) {
			this.updated = true;
			this.setEnabled(false);
			return;
		}

		this.loadCommands();
		this.loadEvents();
		this.loadMaps();
		this.loadWorlds();
		this.loadKits();

		this.nmsHandler = new ReflectionNMS();
		SchematicHandler.initializeReflection();

		if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
			this.sendDebugMessage("Found DecentHolograms");
			this.hologramController = new DecentHologramsController();
		} else if (Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
			this.sendDebugMessage("Found HolographicDisplays");
			if (SkywarsUtils.checkClass("com.gmail.filoghost.holographicdisplays.api.HologramsAPI"))
				this.hologramController = new HolographicDisplaysOldController();
			else if (SkywarsUtils.checkClass("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI"))
				this.hologramController = new HolographicDisplaysNewController();
			else
				this.sendDebugMessage("Unknown version of HolographicDisplays");
		} else {
			this.sendDebugMessage("Did not found any holograms plugin.");
		}
		if (this.hologramController == null)
			this.hologramController = new HologramController() {
				@Override
				public void removeHologram(Object id) {
				}

				@Override
				public String createHologram(Object id, Location location, String text) {
					return null;
				}

				@Override
				public boolean changeHologram(Object id, String text, int line) {
					return false;
				}
			};
		else {
			holograms = true;
			this.sendDebugMessage("Holograms API: " + this.hologramController.getClass().getName());
		}

		economyEnabled = Skywars.get().getConfig().getBoolean("economy.enabled");
		if (economyEnabled)
			try {
				if (this.setupEconomy()) {
					this.sendMessage("&eHooked with Vault!");
					this.sendMessage("&eEconomy service provider: &a%s", this.economyProvider.getPlugin().getName());
				}
			} catch (final Exception e) {
				this.sendMessage("&cCould not hook with Vault!");
				e.printStackTrace();
			}

		// done
		this.sendMessage("&ahas been enabled: &bv%s", this.version);

		if (!Skywars.get().getConfig().getBoolean("taskUpdate.disabled"))
			Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					for (final Player player : Bukkit.getOnlinePlayers()) {
						SkywarsScoreboard.update(player);
						SkywarsActionbar.update(player);
					}
				}
			}, 0L, Skywars.get().getConfig().getLong("taskUpdate.interval") * 20);
	}

	@Override
	public void onDisable() {
		if (this.updated) {
			this.sendMessage("&6The plugin has been updated and disabled.");
			return;
		}

		for (final Player player : Bukkit.getOnlinePlayers()) {
			final Arena arena = this.getPlayerArena(player);
			if (arena != null) {
				arena.exitPlayer(player);
			}
		}

		final ItemStack item = SetupEvents.item;
		ConfigMenu.currentArenas.forEach((player, arena) -> {
			if (item == null)
				return;
			player.getInventory().removeItem(item);
			SkywarsUtils.teleportPlayerBackToTheLobbyOrToTheirLastLocationIfTheLobbyIsNotSet(player);
		});

		this.sendDebugMessage("Stopping arenas...");
		for (final Arena arena : this.arenas) {
			arena.clear(false);
		}
		this.arenas.clear();
	}

	public ReflectionNMS NMS() {
		return this.nmsHandler;
	}

	public String getServerPackageVersion() {
		return this.serverPackageVersion;
	}

	public void Reload() {
		this.loadConfig();
		this.loadMaps();
		this.loadKits();
	}

	private boolean setupEconomy() {
		if (!economyEnabled)
			return false;
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			this.sendDebugMessage("No Vault plugin found!");
			return false;
		}

		this.economyProvider = this.getServer().getServicesManager().getRegistration(Economy.class);
		if (this.economyProvider == null) {
			this.sendDebugMessage("No registered service provider!");
			return false;
		}
		this.economy = this.economyProvider.getProvider();
		return this.economy != null;
	}

	public void loadEvents() {
		final FileConfiguration config = this.getConfig();
		final PluginManager pluginManager = this.getServer().getPluginManager();
		if (config.getBoolean("signsEnabled")) {
			pluginManager.registerEvents(new SignEvents(), this);
		}
		if (config.getBoolean("messageSounds.enabled")) {
			pluginManager.registerEvents(new MessageSound(), this);
		}
		if (config.getBoolean("disableWeather")) {
			pluginManager.registerEvents(new DisableWeather(), this);
		}
		if (config.getBoolean("debug.projectileTests")) {
			pluginManager.registerEvents(new ProjectileTrails(), this);
		}
		final Listener[] listeners = { new InteractEvent(), new Events(), new GamesMenu(), new MapMenu(),
				new KitsMenu(), new SetupEvents(), new ChestManager(), new ConfigMenu(), new GameOptionsMenu(),
				new PlayerInventoryManager(), };
		for (final Listener listener : listeners) {
			pluginManager.registerEvents(listener, this);
		}
	}

	public void loadCommands() {
		final HashMap<String, CommandExecutor> cmds = new HashMap<>();
		cmds.put("skywars", new MainCommand());
		cmds.put("where", new WhereCommand());
		cmds.put("start", new StartCommand());
		cmds.put("forcestart", new ForceStartCommand());
		cmds.put("leave", new LeaveCommand());
		for (final String cmd : cmds.keySet()) {
			if (!this.getConfig().getStringList("disabledCommands").contains(cmd)) {
				this.sendDebugMessage("&eLoading command &a%s&e...", cmd);
				this.getCommand(cmd).setExecutor(cmds.get(cmd));
			} else
				this.sendDebugMessage("&7Skipping command &c%s&e...", cmd);
		}
	}

	// maps

	private final ArrayList<SkywarsMap> maps = new ArrayList<>();

	public SkywarsMap getMap(String name) {
		for (final SkywarsMap map : this.maps) {
			if (map.getName().equalsIgnoreCase(name))
				return map;
		}
		return null;
	}

	public SkywarsMap getRandomMap() {
		return this.maps.get((int) (Math.floor(Math.random() * this.maps.size() + 1) - 1));
	}

	public ArrayList<SkywarsMap> getMaps() {
		return this.maps;
	}

	public boolean createMap(String name) {
		if (this.getMap(name) != null)
			return false;
		final SkywarsMap map = new SkywarsMap(name);
		final File file = new File(mapsPath, name + ".yml");
		try {
			file.createNewFile();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		map.setConfigFile(file);
		map.saveParametersInConfig();
		map.saveConfig();
		this.maps.add(map);
		return true;
	}

	public boolean deleteMap(String name) {
		final SkywarsMap map = this.getMap(name);
		if (map == null)
			return false;
		map.getFile().delete();
		this.maps.remove(map);
		return true;
	}

	public void loadWorlds() {
		final File folder = new File(worldsPath);
		if (!folder.exists())
			folder.mkdirs();
		final File[] worlds = folder.listFiles();
		this.sendDebugMessage("Loading %s worlds", worlds.length);

		final HashMap<String, SkywarsMap> worldNames = new HashMap<String, SkywarsMap>();
		final List<SkywarsMap> filtered = this.getMaps().stream().filter(map -> map.getWorldName() != null)
				.collect(Collectors.toList());
		for (final SkywarsMap map : filtered)
			worldNames.put(map.getWorldName(), map);

		for (final File world : worlds) {
			if (!world.isDirectory()) {
				if (world.isFile() && world.getName().endsWith(".yml")) {
					// it may be a map configuration file
					world.renameTo(new File(mapsPath, world.getName()));
				} else {
					this.sendMessage("&6Unknown world file found: &b%s", world.getName());
				}
			}

			if (worldNames.get(world.getName()) == null) {
				// no map has this world as worldname
				// create map config file for this world
				final File mapFile = new File(mapsPath, world.getName() + ".yml");
				final YamlConfiguration mapConfig = YamlConfiguration.loadConfiguration(mapFile);
				mapConfig.set("world", world.getName());
				mapConfig.set("teamSize", 1);
				try {
					mapConfig.save(mapFile);
				} catch (final IOException e) {
					e.printStackTrace();
					this.sendMessage("Could not save map configuration file for world: %s", world.getName());
				}
				this.sendDebugMessage("Saved map configuration file '%s' for world: %s", mapFile.getName(),
						world.getName());
				this.loadMapFromFile(mapFile);
			}
		}
	}

	public void loadMapFromFile(File file) {
		this.sendDebugMessage("&eLoading map file: &a%s", file.getName());
		final String name = file.getName().replaceFirst("[.][^.]+$", "");
		final YamlConfiguration mapConfig = YamlConfiguration.loadConfiguration(file);

		// create map and set values from config
		final SkywarsMap map = new SkywarsMap(name, mapConfig.getInt("teamSize"));

		map.setConfig(mapConfig);
		map.setConfigFile(file);

		if (config.getString("arenasMethod").equalsIgnoreCase("SINGLE_ARENA")) {
			final String worldName = mapConfig.getString("world");
			this.sendDebugMessage("worldname for %s: %s", map.getName(), worldName);
			if (worldName != null)
				map.setWorldName(worldName);
			else
				map.setLocation(
						ConfigurationUtils.getLocationConfig(mapConfig.getConfigurationSection("location"), mapConfig));
		} else {
			Skywars.get().sendDebugMessage("[debug] arenas method is not single arena");
		}

		if (mapConfig.get("spawn") == null) {
			this.sendDebugMessage("Found no spawns when loading map, calculating spawns...");
			map.calculateSpawns();
		} else {
			Skywars.get().sendDebugMessage("Loading spawns for map: %s", map.getName());
			for (final String spawn : mapConfig.getConfigurationSection("spawn").getKeys(false)) {
				final int i = Integer.parseInt(spawn);
				if (mapConfig.get(String.format("spawn.%s", i)) == null)
					continue;
				final double x = mapConfig.getDouble(String.format("spawn.%s.x", i));
				final double y = mapConfig.getDouble(String.format("spawn.%s.y", i));
				final double z = mapConfig.getDouble(String.format("spawn.%s.z", i));
				final Vector vector = new Vector(x, y, z);
				map.getSpawns().put(i, vector);
			}
		}

		this.maps.add(map);
		this.sendDebugMessage("&eLoaded map: &a%s", map.getName());
	}

	public void loadMaps() {
		final String arenasMethod = config.getString("arenasMethod");
		this.sendDebugMessage("&eLoading maps (&b%s&e)", arenasMethod.toUpperCase());
		if (arenasMethod.equalsIgnoreCase("MULTI_ARENA")) {
			boolean aborted = false;
			final String worldName = config.getString("arenas.world");
			if (worldName == null) {
				this.sendMessage("World for arenas in config (&barenas.world&e) is not set!");
				aborted = true;
			} else if (Bukkit.getWorld(worldName) == null) {
				this.sendMessage("World for arenas in config (&barenas.world&e) does not exist!");
				aborted = true;
			}
			if (aborted) {
				this.sendMessage("&cCancelled map loading. &eUse &b/skywars reload &eto reload the plugin!");
				return;
			}
		}

		this.maps.clear();

		// TODO replace with world map instead of schematic file

		final File folder = new File(mapsPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		if (folder.listFiles().length <= 0) {
			this.sendDebugMessage(Messager.color("&eSetting up default map."));
			ConfigurationUtils.copyDefaultContentsToFile("maps/MiniTrees.yml", new File(mapsPath, "MiniTrees.yml"));
		}

		final File schematics = new File(schematicsPath);
		if (!schematics.exists())
			schematics.mkdirs();
		if (schematics.listFiles().length <= 0) {
			this.sendDebugMessage(Messager.color("&eSetting up default schematic."));
			ConfigurationUtils.copyDefaultContentsToFile("schematics/mini_trees.schematic",
					new File(schematicsPath, "mini_trees.schematic"));
		}

		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				Skywars.get().sendDebugMessage("Loading world in maps folder: %s", file.getName());
				file.renameTo(new File(worldsPath, file.getName()));
				continue;
			}
			this.loadMapFromFile(file);
		}
		this.sendDebugMessage("&eFinished loading maps.");
	}

	public void setupWorld(World world) {
		world.setDifficulty(Difficulty.NORMAL);
		world.setSpawnFlags(true, true);
		world.setPVP(true);
		world.setStorm(false);
		world.setThundering(false);
		world.setWeatherDuration(Integer.MAX_VALUE);
		world.setKeepSpawnInMemory(false);
		world.setTicksPerAnimalSpawns(1);
		world.setTicksPerMonsterSpawns(1);
		world.setAutoSave(false);

		world.setGameRuleValue("doMobSpawning", "false");
		world.setGameRuleValue("mobGriefing", "false");
		world.setGameRuleValue("doFireTick", "false");
		world.setGameRuleValue("showDeathMessages", "false");
		world.setGameRuleValue("announceAdvancements", "false");

		Skywars.get().sendDebugMessage("Successfully set up world settings: &b%s", world.getName());
	}

	// kits

	private final List<Kit> kits = new ArrayList<Kit>();

	public List<Kit> getKits() {
		return this.kits;
	}

	public void loadKits() {
		this.kits.clear();
		final File folder = new File(kitsPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		if (folder.listFiles().length <= 0) {
			this.sendDebugMessage("&eSetting up default kit.");
			ConfigurationUtils.copyDefaultContentsToFile("kits/default.yml", new File(kitsPath, "default.yml"));
		}
		for (final File file : folder.listFiles()) {
			final String name = file.getName().replaceFirst("[.][^.]+$", "");
			final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			ConfigurationUtils.createMissingKeys(config, ConfigurationUtils.getDefaultConfig("kits/default.yml"));

			// create kit and set values from config
			final Kit kit = new Kit(name);
			kit.setConfig(config);
			kit.setFile(file);
			kit.setDisplayName(config.getString("name"));

			// load items
			final List<String> configItems = config.getStringList("items");
			final ItemStack[] items = new ItemStack[configItems.size()];
			for (int i = 0; i < configItems.size(); i++) {
				final String string = configItems.get(i);
				if (string.split(";").length > 1) {
					final String item = string.split(";")[0];
					final int amount = Integer.parseInt(string.split(";")[1]);
					items[i] = new ItemStack(XMaterial.valueOf(item).parseMaterial(), amount);
				} else {
					items[i] = XMaterial.valueOf(string).parseItem();
				}
			}

			kit.setItems(items);
			String iconItem = config.getString("icon");
			if (iconItem == null)
				iconItem = "BEDROCK";
			kit.setIcon(XMaterial.valueOf(iconItem).parseItem());
			kit.setPrice(config.getDouble("price"));

			// add kit to the arena list
			this.kits.add(kit);
			this.sendDebugMessage("&eLoaded kit: &a%s", kit.getName());
		}
		this.sendDebugMessage("Finished loading kits.");
	}

	public static void createBigCase(Location location, XMaterial material) {
		final int[][] blocks = {
				// base
				{ -1, -1, -1 }, { 0, -1, -1 }, { 1, -1, -1 }, { -1, -1, 0 }, { 0, -1, 0 }, { 1, -1, 0 }, { -1, -1, 1 },
				{ 0, -1, 1 }, { 1, -1, 1 },

				// top
				{ -1, 3, 1 }, { 0, 3, -1 }, { 1, 3, -1 }, { -1, 3, 0 }, { 0, 3, 0 }, { 1, 3, 0 }, { -1, 3, 1 },
				{ 0, 3, 1 }, { 1, 3, 1 },

				// left wall
				{ 2, 0, -1 }, { 2, 0, 0 }, { 2, 0, 1 }, { 2, 1, -1 }, { 2, 1, 0 }, { 2, 1, 1 }, { 2, 2, -1 },
				{ 2, 2, 0 }, { 2, 2, 1 },

				// front wall
				{ -1, 0, 2 }, { 0, 0, 2 }, { 1, 0, 2 }, { -1, 1, 2 }, { 0, 1, 2 }, { 1, 1, 2 }, { -1, 2, 2 },
				{ 0, 2, 2 }, { 1, 2, 2 },

				// right wall
				{ -2, 0, -1 }, { -2, 0, 0 }, { -2, 0, 1 }, { -2, 1, -1 }, { -2, 1, 0 }, { -2, 1, 1 }, { -2, 2, -1 },
				{ -2, 2, 0 }, { -2, 2, 1 },

				// back wall
				{ -1, 0, -2 }, { 0, 0, -2 }, { 1, 0, -2 }, { -1, 1, -2 }, { 0, 1, -2 }, { 1, 1, -2 }, { -1, 2, -2 },
				{ 0, 2, -2 }, { 1, 2, -2 }, };
		final int[][] airBlocks = { { -1, 0, -1 }, { 0, 0, -1 }, { 1, 0, -1 }, { -1, 0, 0 }, { 0, 0, 0 }, { 1, 0, 0 },
				{ -1, 0, 1 }, { 0, 0, 1 }, { 1, 0, 1 },

				{ -1, 1, -1 }, { 0, 1, -1 }, { 1, 1, -1 }, { -1, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 }, { -1, 1, 1 },
				{ 0, 1, 1 }, { 1, 1, 1 },

				{ -1, 2, -1 }, { 0, 2, -1 }, { 1, 2, -1 }, { -1, 2, 0 }, { 0, 2, 0 }, { 1, 2, 0 }, { -1, 2, 1 },
				{ 0, 2, 1 }, { 1, 2, 1 }, };
		for (final int[] relative : airBlocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(XMaterial.AIR.parseMaterial());
		}
		for (final int[] relative : blocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(material.parseMaterial());
			if (!XMaterial.isNewVersion()) {
				block.setData(material.getData());
			}
		}
	}

	public static void createCase(Location location, XMaterial material) {
		if (config.getBoolean("debug.bigCases")) {
			createBigCase(location, material);
			return;
		}
		final int[][] blocks = {
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
				{ -1, 3, 0 }, { 1, 3, 0 }, { 0, 3, -1 }, { 0, 3, 1 }, };
		final int[][] airBlocks = { { 0, 0, 0 }, { 0, 1, 0 }, { 0, 2, 0 } };
		for (final int[] relative : airBlocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(XMaterial.AIR.parseMaterial());
		}
		for (final int[] relative : blocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(material.parseMaterial());
			if (!XMaterial.isNewVersion()) {
				block.setData(material.getData());
			}
		}
	}

	// arenas

	private final ArrayList<Arena> arenas = new ArrayList<>();

	public List<Arena> getArenas() {
		return this.arenas;
	}

	public Arena getRandomJoinableArena() {
		for (final Arena arena : this.arenas) {
			if (!arena.isJoinable())
				continue;
			return arena;
		}
		return null;
	}

	// TODO make this a cached hashmap
	public Arena getPlayerArena(Player player) {
		for (int i = 0; i < this.arenas.size(); i++) {
			final List<SkywarsUser> players = this.arenas.get(i).getUsers();
			for (int j = 0; j < players.size(); j++) {
				if (players.get(j).getPlayer().equals(player)) {
					return this.arenas.get(i);
				}
			}
		}
		return null;
	}

	public File getSchematicFile(String schematicName) {
		final File schematic = new File(schematicsPath, schematicName);
		return schematic;
	}

	// player config

	public File getPlayerConfigFile(Player player) {
		return new File(playersPath, player.getUniqueId() + ".yml");
	}

	public YamlConfiguration getPlayerConfig(Player player) {
		final File folder = new File(playersPath);
		if (!folder.exists())
			folder.mkdir();
		final File file = this.getPlayerConfigFile(player);
		if (!file.exists())
			ConfigurationUtils.copyDefaultContentsToFile("players/default.yml", file);
		return YamlConfiguration.loadConfiguration(file);
	}

	public void savePlayerConfig(Player player, YamlConfiguration config) {
		try {
			final File file = this.getPlayerConfigFile(player);
			config.save(file);
			ConfigurationUtils.createMissingKeys(config, ConfigurationUtils.getDefaultConfig("players/default.yml"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	// kits

	public Kit getKit(String name) {
		for (int i = 0; i < this.kits.size(); i++) {
			if (this.kits.get(i).getName().equalsIgnoreCase(name)
					|| this.kits.get(i).getDisplayName().equalsIgnoreCase(name)) {
				return this.kits.get(i);
			}
		}
		return null;
	}

	public void setPlayerKit(Player player, Kit kit) {
		final YamlConfiguration conf = this.getPlayerConfig(player);
		conf.set("kit", kit.getName());
		this.savePlayerConfig(player, conf);
	}

	public Kit getPlayerKit(Player player) {
		final String kitName = this.getPlayerConfig(player).getString("kit");
		if (kitName == null)
			return null;
		return this.getKit(kitName);
	}

	// cases

	public void setPlayerCaseMaterial(Player player, String name) {
		final YamlConfiguration conf = this.getPlayerConfig(player);
		conf.set("case", name);
		this.savePlayerConfig(player, conf);
	}

	public XMaterial getPlayerCaseXMaterial(Player player) {
		final String glass = this.getPlayerConfig(player).getString("case");
		if (glass == null)
			return XMaterial.GLASS;
		final XMaterial mat = XMaterial.valueOf(glass);
		if (mat == null)
			return XMaterial.GLASS;
		return mat;
	}

	// kills

	public Integer getPlayerTotalKills(Player player) {
		return this.getPlayerConfig(player).getInt("stats.solo.kills");
	}

	public void setPlayerTotalKills(Player player, int kills) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("stats.solo.kills", kills);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerTotalKills(Player player) {
		this.setPlayerTotalKills(player, this.getPlayerTotalKills(player) + 1);
	}

	// souls

	public Integer getPlayerSouls(Player player) {
		return this.getPlayerConfig(player).getInt("souls");
	}

	public void setPlayerSouls(Player player, int kills) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("souls", kills);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerSouls(Player player) {
		this.setPlayerSouls(player, this.getPlayerSouls(player) + 1);
	}

	// deaths

	public Integer getPlayerTotalDeaths(Player player) {
		return this.getPlayerConfig(player).getInt("stats.solo.deaths");
	}

	public void setPlayerTotalDeaths(Player player, int deaths) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("stats.solo.deaths", deaths);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerTotalDeaths(Player player) {
		this.setPlayerTotalDeaths(player, this.getPlayerTotalDeaths(player) + 1);
	}

	// wins

	public Integer getPlayerTotalWins(Player player) {
		return this.getPlayerConfig(player).getInt("stats.solo.wins");
	}

	public void setPlayerTotalWins(Player player, int wins) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("stats.solo.wins", wins);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerTotalWins(Player player) {
		this.setPlayerTotalWins(player, this.getPlayerTotalWins(player) + 1);
	}

	//

	public boolean loadConfig() {

		this.sendDebugMessage("Loading configuration...");

		config = ConfigurationUtils.loadConfiguration("config.yml", "config.yml");

		scoreboardConfig = ConfigurationUtils.loadConfiguration("scoreboard.yml", "scoreboard.yml");

		final String lang = Skywars.get().getConfig().getString("locale");
		langConfig = ConfigurationUtils.loadConfiguration("lang/" + lang + ".yml", "lang/" + lang + ".yml",
				"lang/en.yml");

		this.sendDebugMessage("Loaded locale: " + lang + " - " + langConfig.getString("language_name"));

		lobbyConfig = ConfigurationUtils.loadConfiguration("lobby.yml", "lobby.yml");

		if (config == null || scoreboardConfig == null || langConfig == null || lobbyConfig == null)
			return false;

		// load lobby
		this.setLobbyFromConfig();

		this.sendDebugMessage("Finished loading configuration.");
		return true;
	}

	public void sendDebugMessage(String text, Object... format) {
		if (Skywars.config != null && !Skywars.config.getBoolean("debug.enabled"))
			return;
		this.sendMessageWithPrefix(this.debugPrefix, text, format);
	}

	public void sendDebugMessageWithPrefix(String prefix, String text, Object... format) {
		if (Skywars.config != null && !Skywars.config.getBoolean("debug.enabled"))
			return;
		this.sendMessageWithPrefix(prefix, text, format);
	}

	public void sendMessage(String text, Object... format) {
		this.sendMessageWithPrefix(Messager.color(this.prefix), Messager.colorFormat(text, format));
	}

	public void sendMessageWithPrefix(String prefix, String text, Object... format) {
		Bukkit.getConsoleSender().sendMessage(Messager.color(prefix) + " " + Messager.colorFormat(text, format));
	}

	public HologramController getHologramController() {
		return this.hologramController;
	}
}
