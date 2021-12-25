package me.brunorm.skywars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.API.NMSHandler;
import me.brunorm.skywars.commands.MainCommand;
import me.brunorm.skywars.commands.WhereCommand;
import me.brunorm.skywars.events.DisableWeather;
import me.brunorm.skywars.events.Events;
import me.brunorm.skywars.events.MessageSound;
import me.brunorm.skywars.events.SignEvents;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.Kit;
import me.brunorm.skywars.structures.SkywarsPlayer;

@SuppressWarnings("deprecation")
public class Skywars extends JavaPlugin {

	// get plugin.yml
	PluginDescriptionFile descriptionFile = getDescription();
	// get plugin data
	public String version = descriptionFile.getVersion();
	public String name = descriptionFile.getName();
	public List<String> authors = descriptionFile.getAuthors();
	public String prefix = ChatColor.translateAlternateColorCodes('&', String.format("&6[&e%s&6]", name));
	public static String arenasPath;
	public static String kitsPath;
	public static String schematicsPath;
	// config.yml
	public static FileConfiguration config;
	public YamlConfiguration scoreboardConfig;
	public YamlConfiguration langConfig;
	
    private String packageName;
    private String serverPackageVersion;
    private NMSHandler nmsHandler;

	public static Skywars plugin;
	public static Skywars get() {
		return plugin;
	}

	private Location lobby;

	public void setLobby(Location lobby) {
		config.set("lobby.x", lobby.getX());
		config.set("lobby.y", lobby.getY());
		config.set("lobby.z", lobby.getZ());
		config.set("lobby.world", lobby.getWorld().getName());
		this.lobby = lobby;
		System.out.println("lobby set");
	}

	public Location getLobby() {
		return this.lobby;
	}

	public void onEnable() {
		
		// set stuff
		plugin = this;
		config = getConfig();
		arenasPath = getDataFolder() + "/arenas";
		kitsPath = getDataFolder() + "/kits";
		schematicsPath = getDataFolder() + "/schematics";

		// load lobby
		if (config.get("lobby") != null) {
			String worldName = config.getString("lobby.world");
			if(worldName != null) {				
				World world = Bukkit.getWorld(worldName);
				double x = config.getDouble("lobby.x");
				double y = config.getDouble("lobby.y");
				double z = config.getDouble("lobby.z");
				this.lobby = new Location(world, x, y, z);
			}
		}
		
		packageName = this.getServer().getClass().getPackage().getName();
		serverPackageVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		nmsHandler = new NMSHandler();
		
		// load stuff
		// loadPlayers();
		loadConfig();
		loadCommands();
		loadEvents();
		loadArenas();
		loadKits();
		// done
		Bukkit.getConsoleSender().sendMessage(Messager.color(
				String.format("%s &ahas been enabled: &bv%s", prefix, version)));
		//Bukkit.getConsoleSender().sendMessage(Messager.color(
		//		String.format("%s &eis running on &b%s", prefix, serverPackageVersion)));

		Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					SkywarsScoreboard.update(player);
					SkywarsActionbar.update(player);
				}
			}
		}, 0L, 20L);
	}

	public void onDisable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (getPlayerArena(player) != null) {
				SkywarsUtils.ClearPlayer(player);
				SkywarsUtils.TeleportToLobby(player);
			}
		}
		ArenaSetupMenu.currentArenas.forEach((player, arena) -> {
			player.getInventory().removeItem(ArenaSetup.item);
			SkywarsUtils.TeleportToLobby(player);
		});
		System.out.println("Stopping arenas...");
		for (Arena arena : arenas) {
			if(arena.getStatus() == ArenaStatus.PLAYING
					|| arena.getStatus() == ArenaStatus.ENDING) {
				arena.getConfig().set("restartNeeded", true);
				arena.saveConfig();
			}
		}
	}

	public NMSHandler NMS() {
		return nmsHandler;
	}
	
	public String getServerPackageVersion() {
		return serverPackageVersion;
	}
	
	public void Reload() {
		loadConfig();
		loadArenas();
		kits.clear();
		loadKits();
	}

	// player data
	public void loadPlayers() {
		File playersFile = new File(this.getDataFolder(), "players.yml");
		if (!playersFile.exists()) {
			try {
				playersFile.createNewFile();
			} catch (IOException e) {
				System.out.println("couldn't create players.yml");
			}
		}
	}

	// events
	public void loadEvents() {
		getServer().getPluginManager().registerEvents(new Events(), this);
		getServer().getPluginManager().registerEvents(new GamesMenu(), this);
		if(config.getBoolean("signsEnabled") == true) {
			System.out.println("Registering sign events...");
			getServer().getPluginManager().registerEvents(new SignEvents(), this);
		}
		getServer().getPluginManager().registerEvents(new ArenaMenu(), this);
		getServer().getPluginManager().registerEvents(new KitsMenu(), this);
		getServer().getPluginManager().registerEvents(new ArenaSetup(), this);
		getServer().getPluginManager().registerEvents(new ChestManager(), this);
		getServer().getPluginManager().registerEvents(new MessageSound(), this);
		getServer().getPluginManager().registerEvents(new DisableWeather(), this);
		getServer().getPluginManager().registerEvents(new ArenaSetupMenu(), this);
		getServer().getPluginManager().registerEvents(new PlayerInventoryManager(), this);
	}

	// commands
	public void loadCommands() {
		HashMap<String, CommandExecutor> cmds = new HashMap<String, CommandExecutor>();
		cmds.put("skywars", new MainCommand(this));
		cmds.put("where", new WhereCommand(this));
		for(String cmd : cmds.keySet()) {
			if(!getConfig().getStringList("disabledCommands").contains(cmd)) {
				Bukkit.getConsoleSender().sendMessage(
					Messager.colorFormat("%s &eLoading command &a%s&e...", prefix, cmd));
				this.getCommand(cmd).setExecutor(cmds.get(cmd));
			} else Bukkit.getConsoleSender().sendMessage(
				Messager.colorFormat("%s &7Skipping command &c%s&e...", prefix, cmd));
		}
	}

	// config file
	public void loadConfig() {
		
		// config.yml
		File config = new File(this.getDataFolder(), "config.yml");
		if (!config.exists()) {
			saveDefaultConfig();
		}
		//loadConfigFile("config.yml");

		// scoreboard.yml
		scoreboardConfig = YamlConfiguration.loadConfiguration(loadConfigFile("scoreboard.yml"));
		
		// lang.yml
		langConfig = YamlConfiguration.loadConfiguration(loadConfigFile("lang.yml"));
	}
	
	public File loadConfigFile(String name) {
		return loadConfigFile(name, name);
	}
	
	public File loadConfigFile(String name, String defaultFileName) {
		File file = new File(getDataFolder(), name);
		if (!file.exists()) {
			copyDefaultContentsToFile(defaultFileName, file);
		}
		createMissingKeys(defaultFileName, file);
		return file;
	}
	
	public void createMissingKeys(String defaultFileName, File file) {
		try {
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			Reader defaultConfigStream =
					new InputStreamReader(getResource(defaultFileName), "UTF-8");
			YamlConfiguration defaultConfig =
					YamlConfiguration.loadConfiguration(defaultConfigStream);
			ConfigurationSection section = defaultConfig.getConfigurationSection("");
			for (String key : section.getKeys(true)) {
				if (config.get(key) == null) {
					config.set(key, defaultConfig.get(key));
				}
			}
			config.save(file);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void copyDefaultContentsToFile(String defaultFileName, File file) {
		try {
			if(!file.exists()) file.createNewFile();
			try {
				copyInputStreamToFile(getResource(defaultFileName), file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadKits() {
		File folder = new File(kitsPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		if(folder.listFiles().length <= 0) {
			System.out.println("Created default kit");
			copyDefaultContentsToFile("kits/default.yml", new File(kitsPath, "default.yml"));
		}
		for (File file : folder.listFiles()) {
			String name = file.getName().replaceFirst("[.][^.]+$", "");
			createMissingKeys("kits/default.yml", file);
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			
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
			System.out.println("Loaded kit " + kit.getName());
		}
	}
	
    public static final int DEFAULT_BUFFER_SIZE = 8192;
	
    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }

    }
	
	public void loadArenas() {
		arenas.clear();
		File folder = new File(arenasPath);
		if (!folder.exists()) {
			folder.mkdir();
		}
		File defaultArenaFile = new File(arenasPath, "MiniTrees.yml");
		if(folder.listFiles().length <= 0) {
			System.out.println("Created default arena");
			defaultArenaFile = new File(arenasPath, "MiniTrees.yml");
			copyDefaultContentsToFile("arenas/MiniTrees.yml", defaultArenaFile);
		}
		File schematics = new File(schematicsPath);
		if(!schematics.exists()) schematics.mkdir();
		if(schematics.listFiles().length <= 0) {
			System.out.println("copying default schematic");
			copyDefaultContentsToFile("schematics/mini_trees.schematic",
					new File(schematicsPath, "mini_trees.schematic"));
		}
		for (File arenaFile : folder.listFiles()) {
			String name = arenaFile.getName().replaceFirst("[.][^.]+$", "");
			YamlConfiguration config = YamlConfiguration.loadConfiguration(arenaFile);
			// create arena and set values from config
			Arena arena = new Arena(name);
			arena.setFile(arenaFile);
			arena.setConfig(config);
			if(defaultArenaFile == null) {				
				arena.setWorldName(config.getString("worldName"));
			} else {
				Properties props = new Properties();
				try {
					props.load(new FileReader("server.properties"));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String levelName = props.getProperty("level-name");
				if(levelName != null) {
					System.out.println("Got world name from server.properties: " + levelName);
					arena.setWorldName(levelName);
				} else {
					String firstWorld = Bukkit.getServer().getWorlds().get(0).getName();
					System.out.println("Got world name from first world");
					arena.setWorldName(firstWorld);
				}
			}
			arena.setStatus(config.getBoolean("disabled") ? ArenaStatus.DISABLED : ArenaStatus.WAITING);
			if (arena.getWorldName() != null && config.get("location") != null) {
				double x = config.getDouble("location.x");
				double y = config.getDouble("location.y");
				double z = config.getDouble("location.z");
				Location location = new Location(Bukkit.getWorld(arena.getWorldName()), x, y, z);
				arena.setLocation(location);
			}
			arena.setSchematic(config.getString("schematic"));
			arena.setMinPlayers(config.getInt("minPlayers"));
			arena.setMaxPlayers(config.getInt("maxPlayers"));
			// spawns
			if (arena.getWorldName() != null) {
				World world = Bukkit.getServer().getWorld(arena.getWorldName());
				if (world != null) {
					/*
					 * if(config.get("lobby") != null) { double x = config.getDouble("lobby.x");
					 * double y = config.getDouble("lobby.y"); double z =
					 * config.getDouble("lobby.z"); Location lobby = new Location(world,x,y,z);
					 * arena.setLobby(lobby); }
					 */
					System.out.println("Loading spawns from config for arena " + arena.getName());
					for (int i = 0; i < arena.getMaxPlayers(); i++) {
						if (config.get(String.format("spawn.%s", i)) == null)
							continue;
						double x = config.getDouble(String.format("spawn.%s.x", i));
						double y = config.getDouble(String.format("spawn.%s.y", i));
						double z = config.getDouble(String.format("spawn.%s.z", i));
						Location location = new Location(world, x, y, z);
						arena.getSpawns().put(i, location);
					}
				} else System.out.println("Warning: could not get world by "
					+ arena.getWorldName() + " for arena " + arena.getName());
			} else System.out.println("Warning: world not set for arena " + arena.getName());
			// add arena to the arena list
			arenas.add(arena);
			System.out.println("Loaded arena " + arena.getName());
			if(config.getBoolean("restartNeeded")) {
				Bukkit.getConsoleSender().sendMessage(Messager.color("&c&lNeeded to restart arena " + arena.getName()));
				arena.Clear();
				config.set("restartNeeded", null);
				arena.saveConfig();
			}
		}
	}

	public static void createCase(Location location, Material material) {
		createCase(location, material, 0);
	}

	public static void createCase(Location location, Material material, int data) {
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
		for (int i = 0; i < blocks.length; i++) {
			int[] relative = blocks[i];
			Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(material);
			if(!XMaterial.isNewVersion()) {					
				block.setData((byte) data);
			}
		}
	}
	
	// kits
	
	private ArrayList<Kit> kits = new ArrayList<Kit>();
	
	public ArrayList<Kit> getKits() {
		return this.kits;
	}
	
	// arenas

	private ArrayList<Arena> arenas = new ArrayList<Arena>();

	public ArrayList<Arena> getArenas() {
		return this.arenas;
	}

	public ArrayList<Arena> getSortedArenas() {
		ArrayList<Arena> sortedArenas = arenas;
		sortedArenas.sort((a, b) -> a.getProblems().size() - b.getProblems().size());
		sortedArenas.sort((a, b) -> b.getPlayers().size() - a.getPlayers().size());
		return sortedArenas;
	}

	public Arena getRandomJoinableArena() {
		ArrayList<Arena> sortedArenas = getSortedArenas();
		for (Arena arena : sortedArenas) {
			if (!arena.isFull()) {
				if (arena.getStatus() == ArenaStatus.WAITING
						|| arena.getStatus() == ArenaStatus.STARTING) {
					return arena;
				}
			}
		}
		return null;
	}

	public void createArena(String name) {
		if (getArena(name) != null)
			return;
		Arena arena = new Arena(name);
		arena.saveParametersInConfig();
		//loadArenas();
		arenas.add(arena);
	}

	public void deleteArena(String name) {
		Arena arena = getArena(name);
		if (arena == null)
			return;
		arena.getFile().delete();
		arenas.remove(arena);
	}

	public Arena getArena(String name) {
		for (int i = 0; i < arenas.size(); i++) {
			if (arenas.get(i).getName().equalsIgnoreCase(name)) {
				return arenas.get(i);
			}
		}
		return null;
	}
	
	public String[] getArenaNames() {
		String[] arenaNames = new String[arenas.size()];
		for(int i = 0; i < arenas.size(); i++) {
			Arena _arena = arenas.get(i);
			arenaNames[i] = _arena.getName();
		}
		return arenaNames;
	}

	public Arena getPlayerArena(Player player) {
		for (int i = 0; i < arenas.size(); i++) {
			List<SkywarsPlayer> players = arenas.get(i).getPlayers();
			for (int j = 0; j < players.size(); j++) {
				if (players.get(j).getPlayer().equals(player)) {
					return arenas.get(i);
				}
			}
		}
		return null;
	}
	
	/*
	public void PasteSchematicWorldEdit(String schematicName, Location pasteLoc) {
		try {
			File dir = new File(Skywars.get().getDataFolder(), "/schematics/" + schematicName);

			EditSession editSession = new EditSession(new BukkitWorld(pasteLoc.getWorld()), 999999999);
			editSession.enableQueue();

			SchematicFormat schematic = SchematicFormat.getFormat(dir);
			CuboidClipboard clipboard = schematic.load(dir);

			clipboard.paste(editSession, BukkitUtil.toVector(pasteLoc), false);
			editSession.flushQueue();
			System.out.println(String.format("pasted schematic %s", schematicName));
		} catch (DataException | IOException ex) {
			ex.printStackTrace();
		} catch (MaxChangedBlocksException ex) {
			ex.printStackTrace();
		}
	}
	*/

	public File getSchematicFile(String schematicName) {
		File schematic = new File(Skywars.get().getDataFolder(), "/schematics/" + schematicName);
		if(schematic.exists()) return schematic;
		return null;
	}

	public YamlConfiguration getPlayerConfig(Player player) {
		File folder = new File(getDataFolder() + "/players");
		if(!folder.exists()) folder.mkdir();
		File file = new File(getDataFolder() + "/players", player.getUniqueId() + ".yml");
		if(!file.exists())
			copyDefaultContentsToFile("players/default.yml", file);
		createMissingKeys("players/default.yml", file);
		return YamlConfiguration.loadConfiguration(file);
	}
	
	public void savePlayerConfig(Player player) {
		try {
			File file = new File(getDataFolder() + "/players", player.getUniqueId() + ".yml");
			getPlayerConfig(player).save(file);
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
	
	/*
	 * void PasteSchematic(Location location) { File file = new
	 * File(Skywars.get().getDataFolder(), "schematic.schematic"); ClipboardFormat
	 * format = ClipboardFormats.findByFile(file); try { ClipboardReader reader =
	 * format.getReader(new FileInputStream(file)); Clipboard clipboard =
	 * reader.read(); try {
	 * 
	 * @SuppressWarnings("deprecation") EditSession editSession =
	 * WorldEdit.getInstance().getEditSessionFactory()
	 * .getEditSession((com.sk89q.worldedit.world.World)
	 * Bukkit.getWorld(location.getWorld().getName()), -1); Operation operation =
	 * new ClipboardHolder(clipboard).createPaste(editSession)
	 * .to(BlockVector3.at(location.getX(), location.getY(),
	 * location.getZ())).ignoreAirBlocks(false) .build();
	 * Operations.complete(operation); } catch (Exception e) {
	 * 
	 * } } catch (Exception e) {
	 * 
	 * } }
	 */
}
