package me.brunorm.skywars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
import me.brunorm.skywars.handlers.SkywarsActionbar;
import me.brunorm.skywars.handlers.SkywarsScoreboard;
import me.brunorm.skywars.handlers.SkywarsTablist;
import me.brunorm.skywars.holograms.DecentHologramsController;
import me.brunorm.skywars.holograms.HologramController;
import me.brunorm.skywars.holograms.HolographicDisplaysNewController;
import me.brunorm.skywars.holograms.HolographicDisplaysOldController;
import me.brunorm.skywars.managers.ChestManager;
import me.brunorm.skywars.managers.MapManager;
import me.brunorm.skywars.managers.SignManager;
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
import me.brunorm.skywars.structures.SkywarsUser;
import net.milkbowl.vault.economy.Economy;

@SuppressWarnings("deprecation")
public class Skywars extends JavaPlugin {

	// get plugin data
	PluginDescriptionFile pdf = this.getDescription();
	public String name = this.pdf.getName();
	public String version = this.pdf.getVersion();
	public List<String> authors = this.pdf.getAuthors();
	private final String prefix = Messager.color("&6[&e%s&6]&e", this.name);
	private final String debugPrefix = Messager.color("&7[&c%s&7]&e", this.name);
	public static String kitsPath;
	public static String worldsPath;
	public static String mapsPath;
	public static String schematicsPath;
	public static String playersPath;
	public static String chestsPath;
	public static SkywarsConfiguration configuration;

	public static boolean holograms = false;
	public static boolean placeholders = false;
	HologramController hologramController;
	public static boolean economyEnabled;
	Economy economy;
	RegisteredServiceProvider<Economy> economyProvider;
	SignManager signManager;

	public SignManager getSignManager() {
		return this.signManager;
	}

	public Economy getEconomy() {
		return this.economy;
	}

	public static YamlConfiguration config;
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

	public void Reload() {
		this.loadConfig();
		this.mapManager.loadMaps();
		this.chestManager.loadChests();
		this.signManager.loadSigns();
		this.loadKits();
	}

	@Override
	public void onEnable() {

		// set stuff
		plugin = this;
		worldsPath = this.getDataFolder() + "/worlds";
		mapsPath = this.getDataFolder() + "/maps";
		kitsPath = this.getDataFolder() + "/kits";
		schematicsPath = this.getDataFolder() + "/schematics";
		playersPath = this.getDataFolder() + "/players";
		chestsPath = this.getDataFolder() + "/chests";

		this.packageName = this.getServer().getClass().getPackage().getName();
		this.serverPackageVersion = this.packageName.substring(this.packageName.lastIndexOf('.') + 1);

		this.sendDebugMessage("&bServer version: &e%s (&a%s&e)", this.packageName, this.serverPackageVersion);

		final File worldsToDeleteFile = new File(this.getDataFolder(), "delete_worlds.yml");
		if (worldsToDeleteFile.exists()) {
			final YamlConfiguration deleteWorldsConfig = YamlConfiguration.loadConfiguration(worldsToDeleteFile);
			final List<String> list = deleteWorldsConfig.getStringList("worlds");
			for (final String worldName : new ArrayList<String>(list)) {
				final World world = Bukkit.getWorld(worldName);
				if (world != null) {
					for (final Player p : world.getPlayers())
						SkywarsUtils.teleportPlayerBackToTheLobbyOrToTheirLastLocationIfTheLobbyIsNotSet(p, true);
					Bukkit.unloadWorld(worldName, false);
				}
				final File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
				if (worldFolder.exists() && worldFolder.isDirectory())
					try {
						FileUtils.deleteDirectory(worldFolder);
					} catch (final IOException e) {
						e.printStackTrace();
						Skywars.get().sendMessage("&cCould not delete world folder: &b" + worldFolder.getPath());
					}
				list.remove(worldName);
			}
			deleteWorldsConfig.set("worlds", list);
			try {
				deleteWorldsConfig.save(worldsToDeleteFile);
			} catch (final IOException e) {
				e.printStackTrace();
				this.sendMessage("Could not save the world deletion list to file: " + worldsToDeleteFile.getPath());
			}
		}

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

		this.mapManager.loadMaps();
		this.mapManager.loadWorlds();
		this.chestManager.loadChests();
		this.signManager.loadSigns();
		this.loadKits();

		this.nmsHandler = new ReflectionNMS();
		SchematicHandler.initializeReflection();

		this.sendMessage("&b&l--- PLUGIN HOOKS ---");
		if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
			this.hologramController = new DecentHologramsController();
		} else if (Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
			if (SkywarsUtils.checkClass("com.gmail.filoghost.holographicdisplays.api.HologramsAPI"))
				this.hologramController = new HolographicDisplaysOldController();
			else if (SkywarsUtils.checkClass("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI"))
				this.hologramController = new HolographicDisplaysNewController();
		}
		if (this.hologramController == null) {
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
			this.sendMessage("&eHolograms: &cno supported holograms plugin found!");
		} else {
			holograms = true;
			this.sendMessage("&eHolograms: &a" + this.hologramController.getClass().getSimpleName());
		}

		economyEnabled = Skywars.get().getConfig().getBoolean("economy.enabled");
		if (economyEnabled)
			try {
				if (this.setupEconomy()) {
					this.sendMessage("&eEconomy (Vault): &a" + this.economyProvider.getPlugin().getName());
				}
			} catch (final Exception e) {
				this.sendMessage("&eEconomy (Vault): &ccould not hook.");
				e.printStackTrace();
			}
		else
			this.sendMessage("&eEconomy (Vault): &6disabled in config.");

		// placeholder api
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			placeholders = true;
		}
		this.sendMessage("&ePlaceholderAPI: " + (placeholders ? "&ahooked." : "&cnot found."));

		// done
		this.sendMessage("&ahas been enabled: &bv%s", this.version);

		if (placeholders)
			new SkywarsPlaceholderExpansion().register();

		if (!Skywars.get().getConfig().getBoolean("taskUpdate.disabled"))
			Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					for (final Player player : Bukkit.getOnlinePlayers()) {
						SkywarsScoreboard.update(player);
						SkywarsActionbar.update(player);
						SkywarsTablist.update(player);
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
		// add world names to a file
		// to try to delete those worlds
		// the next time the plugin starts up
		final List<String> worldNames = new ArrayList<String>(this.arenas.size());
		for (final Arena arena : this.arenas) {
			arena.clear(false);
			worldNames.add(arena.getWorldName());
		}
		final File file = new File(this.getDataFolder(), "delete_worlds.yml");
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		worldNames.addAll(config.getStringList("worlds"));
		config.set("worlds", worldNames);
		try {
			if (!file.exists())
				file.createNewFile();
			config.save(file);
		} catch (final IOException e) {
			e.printStackTrace();
			this.sendMessage("Could not write world list to file.");
		}
		this.arenas.clear();
	}

	public ReflectionNMS NMS() {
		return this.nmsHandler;
	}

	public String getServerPackageVersion() {
		return this.serverPackageVersion;
	}

	private boolean setupEconomy() {
		if (!economyEnabled)
			return false;
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			this.sendMessage("&eEconomy (Vault): &6plugin not found.");
			return false;
		}

		this.economyProvider = this.getServer().getServicesManager().getRegistration(Economy.class);
		if (this.economyProvider == null) {
			this.sendMessage("&eEconomy (Vault): &6plugin found &cbut no registered service provider!");
			return false;
		}
		this.economy = this.economyProvider.getProvider();
		return this.economy != null;
	}

	public void loadEvents() {
		final FileConfiguration config = this.getConfig();
		final PluginManager pluginManager = this.getServer().getPluginManager();
		if (config.getBoolean("signsEnabled")) {
			this.signManager = new SignManager();
			pluginManager.registerEvents(this.signManager, this);
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
				new KitsMenu(), new SetupEvents(), new ConfigMenu(), new GameOptionsMenu(),
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

	ChestManager chestManager = new ChestManager();

	public ChestManager getChestManager() {
		return this.chestManager;
	}

	MapManager mapManager = new MapManager();

	public MapManager getMapManager() {
		return this.mapManager;
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
			ConfigurationUtils.createMissingKeys(config, ConfigurationUtils.getDefaultConfig("kits/default.yml"),
					file.getPath());

			// create kit and set values from config
			final Kit kit = new Kit(name);
			kit.setConfig(config);
			kit.setFile(file);
			kit.setDisplayName(config.getString("name"));

			// load items
			final List<ItemStack> items = new ArrayList<ItemStack>();

			for (final Object a : config.getList("items")) {
				final ItemStack item = ConfigurationUtils.parseItemFromConfig(a);
				if (item == null)
					continue;
				items.add(item);
			}

			kit.setItems(items.toArray(new ItemStack[0]));
			String iconItem = config.getString("icon");
			if (iconItem == null)
				iconItem = "BEDROCK";
			kit.setIcon(XMaterial.valueOf(iconItem).parseItem());
			kit.setPrice(config.getDouble("price", 0));

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
				{ -1, 3, -1 }, { 0, 3, -1 }, { 1, 3, -1 }, { -1, 3, 0 }, { 0, 3, 0 }, { 1, 3, 0 }, { -1, 3, 1 },
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
				try {
					block.getClass().getMethod("setData", byte.class).invoke(block, material.getData());
				} catch (final Exception e) {
					e.printStackTrace();
				}
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
				try {
					block.getClass().getMethod("setData", byte.class).invoke(block, material.getData());
				} catch (final Exception e) {
					e.printStackTrace();
				}
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

	public SkywarsUser getSkywarsUser(Player player) {
		for (int i = 0; i < this.arenas.size(); i++) {
			final List<SkywarsUser> players = this.arenas.get(i).getUsers();
			for (int j = 0; j < players.size(); j++) {
				if (players.get(j).getPlayer().equals(player)) {
					return players.get(j);
				}
			}
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

	public File getPlayerConfigFile(OfflinePlayer player) {
		return new File(playersPath, player.getUniqueId() + ".yml");
	}

	public YamlConfiguration getPlayerConfig(OfflinePlayer player) {
		final File folder = new File(playersPath);
		if (!folder.exists())
			folder.mkdir();
		final File file = this.getPlayerConfigFile(player);
		if (!file.exists())
			ConfigurationUtils.copyDefaultContentsToFile("players/default.yml", file);
		return YamlConfiguration.loadConfiguration(file);
	}

	public void savePlayerConfig(OfflinePlayer player, YamlConfiguration config) {
		try {
			final File file = this.getPlayerConfigFile(player);
			config.save(file);
			ConfigurationUtils.createMissingKeys(config, ConfigurationUtils.getDefaultConfig("players/default.yml"),
					file.getPath());
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

	public void setPlayerKit(OfflinePlayer player, Kit kit) {
		final YamlConfiguration conf = this.getPlayerConfig(player);
		conf.set("kit", kit.getName());
		this.savePlayerConfig(player, conf);
	}

	public Kit getPlayerKit(OfflinePlayer player) {
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
			return configuration.defaultCaseMaterial;
		final XMaterial mat = XMaterial.valueOf(glass);
		if (mat == null)
			return configuration.defaultCaseMaterial;
		return mat;
	}

	// kills

	public int getPlayerTotalKills(OfflinePlayer player) {
		return this.getPlayerConfig(player).getInt("stats.solo.kills");
	}

	public void setPlayerTotalKills(OfflinePlayer player, int kills) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("stats.solo.kills", kills);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerTotalKills(OfflinePlayer player) {
		this.setPlayerTotalKills(player, this.getPlayerTotalKills(player) + 1);
	}

	// souls

	public int getPlayerSouls(OfflinePlayer player) {
		return this.getPlayerConfig(player).getInt("souls");
	}

	public void setPlayerSouls(OfflinePlayer player, int kills) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("souls", kills);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerSouls(OfflinePlayer player) {
		this.setPlayerSouls(player, this.getPlayerSouls(player) + 1);
	}

	// deaths

	public int getPlayerTotalDeaths(OfflinePlayer player) {
		return this.getPlayerConfig(player).getInt("stats.solo.deaths");
	}

	public void setPlayerTotalDeaths(OfflinePlayer player, int deaths) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("stats.solo.deaths", deaths);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerTotalDeaths(OfflinePlayer player) {
		this.setPlayerTotalDeaths(player, this.getPlayerTotalDeaths(player) + 1);
	}

	// wins

	public int getPlayerTotalWins(OfflinePlayer player) {
		return this.getPlayerConfig(player).getInt("stats.solo.wins");
	}

	public void setPlayerTotalWins(OfflinePlayer player, int wins) {
		final YamlConfiguration config = this.getPlayerConfig(player);
		config.set("stats.solo.wins", wins);
		this.savePlayerConfig(player, config);
	}

	public void incrementPlayerTotalWins(OfflinePlayer player) {
		this.setPlayerTotalWins(player, this.getPlayerTotalWins(player) + 1);
	}

	//

	public boolean loadConfig() {

		this.sendDebugMessage("Loading configuration...");

		config = ConfigurationUtils.loadConfiguration("config.yml", "config.yml");

		configuration = new SkywarsConfiguration();
		configuration.load(config);

		final String lang = Skywars.get().getConfig().getString("locale");
		langConfig = ConfigurationUtils.loadConfiguration("lang/" + lang + ".yml", "lang/" + lang + ".yml",
				"lang/en.yml");

		this.sendDebugMessage("Loaded locale: " + lang + " - " + langConfig.getString("language_name"));

		lobbyConfig = ConfigurationUtils.loadConfiguration("lobby.yml", "lobby.yml");

		if (config == null || langConfig == null || lobbyConfig == null)
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
		this.sendMessageWithPrefix(Messager.color(this.prefix), Messager.color(text, format));
	}

	public void sendMessageWithPrefix(String prefix, String text, Object... format) {
		Bukkit.getConsoleSender().sendMessage(Messager.color(prefix) + " " + Messager.color(text, format));
	}

	public HologramController getHologramController() {
		return this.hologramController;
	}
}
