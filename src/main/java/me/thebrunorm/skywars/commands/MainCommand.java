package me.thebrunorm.skywars.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.SkywarsUtils;
import me.thebrunorm.skywars.commands.CommandsUtils;
import me.thebrunorm.skywars.handlers.SkywarsScoreboard;
import me.thebrunorm.skywars.managers.ArenaManager;
import me.thebrunorm.skywars.managers.ChestManager;
import me.thebrunorm.skywars.managers.MapManager;
import me.thebrunorm.skywars.menus.ConfigMenu;
import me.thebrunorm.skywars.menus.GamesMenu;
import me.thebrunorm.skywars.menus.MapMenu;
import me.thebrunorm.skywars.schematics.Schematic;
import me.thebrunorm.skywars.schematics.SchematicHandler;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;
import me.thebrunorm.skywars.structures.SkywarsUser;

/**
 * Main command class that handles all Skywars commands
 * 主命令类，处理所有Skywars命令
 */
public class MainCommand implements CommandExecutor {

	/*
	 * String[] helpLines = { "&a&lSkywars commands",
	 * "&b/skywars create <arena> &e- creates an arena",
	 * "&b/skywars minplayers <arena> <number> &e- set a number of min players",
	 * "&b/skywars maxplayers <arena> <number> &e- set a number of min players",
	 * "&b/skywars world <arena> <world> &e- set the world of an arena to your current world"
	 * ,
	 * "&b/skywars position <arena> <x> <y> <z> &e- set the position of an arena to your current position"
	 * ,
	 * "&b/skywars schematic <arena> <schematic> &e- set the schematic file of an arena"
	 * ,
	 * "&b/skywars spawn <set/remove> <arena> <number> &e- set or remove the spawn of an arena"
	 * , "&b/skywars enable <arena> &e- enables an arena",
	 * "&b/skywars disable <arena> &e- disables an arena",
	 * "&b/skywars setmainlobby &e- sets the main lobby to your current position",
	 * "&b/skywars menu &e- opens the games menu",
	 * "&b* You can use the shorthand version of the command: &e&l/sw" };
	 */

	// Schematic object for handling schematics
	// 用于处理图纸的图纸对象
	Schematic schematic;

	// Bukkit tasks for scheduling operations
	// 用于安排操作的Bukkit任务
	BukkitTask task;
	BukkitTask anotherTask;

	/**
	 * Cancel the scheduled timer task
	 * 取消计划的计时器任务
	 */
	void cancelTimer() {
		if (this.task != null)
			this.task.cancel();
	}

	/**
	 * Execute the main command based on arguments provided
	 * 根据提供的参数执行主命令
	 */
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		try {
			// Check if arguments are provided, if not send welcome message
			// 检查是否提供了参数，如果没有则发送欢迎消息
			if (args.length <= 0) {
				sender.sendMessage(Messager.getMessage("WELCOME_MESSAGE"));
				return true;
			}
			Player player = null;
			if (sender instanceof Player) {
				player = (Player) sender;
			}
			// Check if player has already joined an arena
			// 检查玩家是否已加入竞技场
			final boolean joined = Skywars.get().getPlayerArena(player) != null;
			String name = null;
			if (args.length > 1) {
				name = args[1];
			}
			// Get the arena and map objects for the player and specified name
			// 获取玩家和指定名称的竞技场和地图对象
			final Arena playerArena = Skywars.get().getPlayerArena(player);
			final SkywarsMap map = Skywars.get().getMapManager().getMap(name);
			final Arena arena = ArenaManager.getArenaByMap(map, true);

			// Handle setmainlobby command to set main lobby location
			// 处理setmainlobby命令以设置主大厅位置
			if (args[0].equalsIgnoreCase("setmainlobby")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// Check if player has permission to set main lobby
				// 检查玩家是否有权限设置主大厅
				if (CommandsUtils.permissionCheckWithMessage(player, "skywars.setmainlobby")) {
					// Set the lobby to player's location
					// 将大厅设置为玩家的位置
					Skywars.get().setLobby(player.getLocation());
					// Send confirmation message
					// 发送确认消息
					player.sendMessage(Messager.getMessage("MAIN_LOBBY_SET"));
				}
			}
			// Handle lobby command to teleport player to main lobby
			// 处理lobby命令以传送玩家到主大厅
			else if (args[0].equalsIgnoreCase("lobby")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// If player is in an arena, make them leave
				// 如果玩家在竞技场中，让他们离开
				if (joined) {
					Skywars.get().getPlayerArena(player).leavePlayer(player);
				}
				// Check if lobby is set, then teleport player
				// 检查大厅是否设置，然后传送玩家
				if (Skywars.get().getLobby() != null) {
					player.teleport(Skywars.get().getLobby());
					player.sendMessage(Messager.getMessage("TELEPORTED_TO_MAIN_LOBBY"));
				} else {
					player.sendMessage(Messager.getMessage("MAIN_LOBBY_NOT_SET"));
				}
			}
			// Handle play command to open map menu
			// 处理play命令以打开地图菜单
			else if (args[0].equalsIgnoreCase("play")) {
				// Open map menu for the player
				// 为玩家打开地图菜单
				MapMenu.open(player);
			}
			// Handle config/setup command to open configuration menu
			// 处理config/setup命令以打开配置菜单
			else if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("setup")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// Check if player has configuration permissions
				// 检查玩家是否有配置权限
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.config"))
					return true;
				// Join all arguments to form the complete map name
				// 连接所有参数以形成完整的地图名称
				final ArrayList<String> list = new ArrayList<String>(Arrays.asList(args));
				list.remove(0);
				final String _name = String.join(" ", list);
				final SkywarsMap _map = Skywars.get().getMapManager().getMap(_name);
				// Check if map exists
				// 检查地图是否存在
				if (_map == null) {
					player.sendMessage(Messager.getMessage("NO_MAP"));
					return true;
				}
				// Open configuration menu for the map
				// 为地图打开配置菜单
				ConfigMenu.OpenConfigurationMenu(player, _map);
			}
			// Handle help command to show command list
			// 处理help命令以显示命令列表
			else if (args[0].equalsIgnoreCase("help")) {
				// Send plugin information and version
				// 发送插件信息和版本
				sender.sendMessage(Messager.color("&a&lCommand list - &b%s &a%s &eby &b%s", Skywars.get().name, Skywars.get().version,
						String.join(", ", Skywars.get().authors)));
				// Send individual help messages for each command
				// 为每个命令发送单独的帮助消息
				sender.sendMessage(Messager.getMessage("HELP_SETMAINLOBBY"));
				sender.sendMessage(Messager.getMessage("HELP_LOBBY"));
				sender.sendMessage(Messager.getMessage("HELP_CREATE"));
				sender.sendMessage(Messager.getMessage("HELP_DELETE"));
				sender.sendMessage(Messager.getMessage("HELP_CONFIG"));
				sender.sendMessage(Messager.getMessage("HELP_PLAY"));
				sender.sendMessage(Messager.getMessage("HELP_START"));
				sender.sendMessage(Messager.getMessage("HELP_FORCESTART"));
			}
			// Handle leave command to make player leave current arena
			// 处理leave命令以让玩家离开当前竞技场
			else if (args[0].equalsIgnoreCase("leave")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// Check if player is in an arena and make them leave
				// 检查玩家是否在竞技场中并让他们离开
				if (CommandsUtils.arenaCheckWithMessage(player))
					playerArena.leavePlayer(player);

				// TODO add the option to pass an arena name as an argument
				// TODO 添加将竞技场名称作为参数传递的选项
			}
			// Handle forcestart command to force start the game
			// 处理forcestart命令以强制开始游戏
			else if (args[0].equalsIgnoreCase("forcestart")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// Force start the game in the player's arena
				// 在玩家的竞技场中强制开始游戏
				playerArena.startGame(player);
			}
			// Handle start command to softly start the game
			// 处理start命令以软开始游戏
			else if (args[0].equalsIgnoreCase("start")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// Softly start the game in the player's arena (with countdown)
				// 在玩家的竞技场中软开始游戏（带倒计时）
				playerArena.softStart(player);
			}
			// Handle join command to join a specific map
			// 处理join命令以加入特定地图
			else if (args[0].equalsIgnoreCase("join")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// Join the specified map
				// 加入指定的地图
				ArenaManager.joinMap(map, player);
			}
			// Handle info/about/ver/version commands to show plugin information
			// 处理info/about/ver/version命令以显示插件信息
			else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("about")
					|| args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("version")) {
				// Send formatted plugin information
				// 发送格式化的插件信息
				sender.sendMessage(Messager.getFormattedMessage("PLUGIN_INFO", player, null, null, Skywars.get().name,
						Skywars.get().version, String.join(", ", Skywars.get().authors)));
			}
			// Handle server command to show server information
			// 处理server命令以显示服务器信息
			else if (args[0].equalsIgnoreCase("server")) {
				// Check if player has admin permissions
				// 检查玩家是否有管理员权限
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Send server version information
				// 发送服务器版本信息
				sender.sendMessage(Messager.getMessage("SERVER_VERSION_TITLE"));
				sender.sendMessage(Bukkit.getServer().getVersion());
				sender.sendMessage(Bukkit.getServer().getBukkitVersion());
			}
			// Handle refill command to refill chests in arena
			// 处理refill命令以在竞技场中补充箱子
			else if (args[0].equalsIgnoreCase("refill")) {
				// Check if player has admin permissions
				// 检查玩家是否有管理员权限
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Check if player is in an arena
				// 检查玩家是否在竞技场中
				if (!CommandsUtils.arenaCheckWithMessage(player))
					return true;
				// Refill chests and broadcast message
				// 补充箱子并广播消息
				playerArena.fillChests();
				playerArena.broadcastRefillMessage();
			}
			// Handle menu command to open games menu
			// 处理menu命令以打开游戏菜单
			else if (args[0].equalsIgnoreCase("menu")) {
				// Check if command sender is a player
				// 检查命令发送者是否为玩家
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// Open games menu for the player
				// 为玩家打开游戏菜单
				GamesMenu.open(player);
			}
			// Handle reload/rl command to reload plugin configuration
			// 处理reload/rl命令以重载插件配置
			else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
				// Check if player has admin permissions
				// 检查玩家是否有管理员权限
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Reload plugin configuration
				// 重载插件配置
				Skywars.get().Reload();
				// Send reload confirmation message
				// 发送重载确认消息
				sender.sendMessage(Messager.getMessage("RELOADED"));
			}

			// else if(!CommandsUtils.permissionCheckWithMessage(player, "skywars.test"))
			// return true;
			// TEST COMMANDS
			// 测试命令

			// Handle testconfig command for testing configuration
			// 处理testconfig命令以测试配置
			else if (args[0].equalsIgnoreCase("testconfig")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
			}
			// Handle importworld command to import a world
			// 处理importworld命令以导入世界
			else if (args[0].equalsIgnoreCase("importworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get the current world of the player
				// 获取玩家当前的世界
				final World world = player.getWorld();

				// Send message about teleporting players
				// 发送关于传送玩家的消息
				player.sendMessage(Messager.getMessage("TELEPORTING_PLAYERS_OUT_OF_WORLD"));
				// Teleport all players in the world to lobby or their last location
				// 将世界中的所有玩家传送到大厅或他们最后的位置
				for (final Player p : world.getPlayers())
					SkywarsUtils.teleportPlayerBackToTheLobbyOrToTheirLastLocationIfTheLobbyIsNotSet(p, true);

				// Unload the world
				// 卸载世界
				if (!Bukkit.unloadWorld(world, true)) {
					player.sendMessage(Messager.getMessage("COULD_NOT_SAVE_AND_UNLOAD_WORLD"));
					return true;
				}
				// Send success message
				// 发送成功消息
				player.sendMessage(Messager.getFormattedMessage("SAVED_AND_UNLOADED_WORLD", null, null, null, world.getName()));

				// Get world folder path
				// 获取世界文件夹路径
				final File worldFolder = new File(Bukkit.getWorldContainer(), world.getName());
				if (!worldFolder.exists()) {
					player.sendMessage(Messager.getFormattedMessage("COULD_NOT_FIND_WORLD_FOLDER", null, null, null, worldFolder.getAbsolutePath()));
					return true;
				}

				// Move world folder to plugin's worlds path
				// 将世界文件夹移动到插件的世界路径
				final File newFolder = new File(Skywars.worldsPath, world.getName());
				if (!worldFolder.renameTo(newFolder)) {
					player.sendMessage(Messager.getFormattedMessage("COULD_NOT_MOVE_WORLD_TO", null, null, null, newFolder.getAbsolutePath()));
				}
				// Send completion messages
				// 发送完成消息
				player.sendMessage(Messager.getMessage("MOVED_WORLD_TO_PLUGINS_FOLDERS"));
				player.sendMessage(Messager.getMessage("SHOULD_BE_ABLE_TO_SEE_WORLD_WHEN_SELECTING"));
			}
			// Handle saveworld command to save current world
			// 处理saveworld命令以保存当前世界
			else if (args[0].equalsIgnoreCase("saveworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get the current world of the player
				// 获取玩家当前的世界
				final World world = player.getWorld();
				// Save the world
				// 保存世界
				world.save();
				// Send world information messages
				// 发送世界信息消息
				player.sendMessage(Messager.getMessage("SAVED_CURRENT_WORLD"));
				player.sendMessage(Messager.getFormattedMessage("CURRENT_WORLD_NAME", null, null, null, world.getName()));
				player.sendMessage(Messager.getFormattedMessage("CURRENT_WORLD_AUTOSAVES", null, null, null, world.isAutoSave()));
				/*
				 * SkywarsMap map_ = null; for (final SkywarsMap m : Skywars.get().getMaps()) if
				 * (m.getWorldName().equalsIgnoreCase(player.getWorld().getName())) { map_ = m;
				 * break; } if(map_ == null) {
				 * player.sendMessage("Could not find a map that matches your current world.");
				 * return true; }
				 */
			}
			// Handle worldexists command to check if a world exists
			// 处理worldexists命令以检查世界是否存在
			else if (args[0].equalsIgnoreCase("worldexists")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get world name from arguments
				// 从参数获取世界名称
				final String worldName = args[1];
				// Check if world exists
				// 检查世界是否存在
				final boolean bol = Bukkit.getServer().getWorld(worldName) != null;
				// Send result message
				// 发送结果消息
				sender.sendMessage(Messager.getFormattedMessage("WORLD_EXISTS_STATUS", null, null, null, bol));
			}
			// Handle tpworld command to teleport player to a world
			// 处理tpworld命令以传送玩家到世界
			else if (args[0].equalsIgnoreCase("tpworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get target world name from arguments
				// 从参数获取目标世界名称
				final String worldName = args[1];
				// Check if sender is a player
				// 检查发送者是否为玩家
				if (player == null) {
					sender.sendMessage(Messager.getMessage("YOU_NEED_TO_BE_A_PLAYER"));
					return true;
				}
				// Create teleport location
				// 创建传送位置
				final Location loc = new Location(Bukkit.getServer().getWorld(worldName), 0, 100, 0);
				// Attempt to teleport player and send result message
				// 尝试传送玩家并发送结果消息
				if (player.teleport(loc))
					sender.sendMessage(Messager.getFormattedMessage("TELEPORTED_TO_WORLD", null, null, null, worldName));
				else
					sender.sendMessage(Messager.getFormattedMessage("COULD_NOT_TELEPORT_TO_WORLD", null, null, null, worldName));
			}
			// Handle checksetup command to check world setup
			// 处理checksetup命令以检查世界设置
			else if (args[0].equalsIgnoreCase("checksetup")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get current world
				// 获取当前世界
				final World world = player.getWorld();
				// Send world setup information
				// 发送世界设置信息
				player.sendMessage(Messager.getFormattedMessage("CURRENT_WORLD_SETUP", null, null, null, world.getName()));
				player.sendMessage(Messager.getFormattedMessage("AUTO_SAVE_STATUS", null, null, null, world.isAutoSave()));
			}
			// Handle setupworld command to set up a world for Skywars
			// 处理setupworld命令以设置世界用于Skywars
			else if (args[0].equalsIgnoreCase("setupworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Set up the world for Skywars
				// 为Skywars设置世界
				MapManager.setupWorld(player.getWorld());
				// Send confirmation message
				// 发送确认消息
				player.sendMessage(Messager.getFormattedMessage("SET_UP_WORLD", null, null, null, player.getWorld().getName()));
			}
			// Handle loadworld command to load a world
			// 处理loadworld命令以加载世界
			else if (args[0].equalsIgnoreCase("loadworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get world name from arguments
				// 从参数获取世界名称
				final String worldName = args[1];
				// Check if world folder exists
				// 检查世界文件夹是否存在
				if (!(new File(Bukkit.getServer().getWorldContainer(), worldName).exists())) {
					sender.sendMessage(Messager.getMessage("WORLD_FOLDER_DOES_NOT_EXIST"));
					return true;
				}
				// Create and load the world
				// 创建并加载世界
				final World world = Bukkit.getServer().createWorld(new WorldCreator(worldName));
				if (world != null) {
					// Configure world settings
					// 配置世界设置
					world.setGameRuleValue("doMobSpawning", "false");
					world.setAutoSave(false);
					// Send success message
					// 发送成功消息
					sender.sendMessage(Messager.getFormattedMessage("LOADED_WORLD", null, null, null, world.getName()));
					// If sender is a player, teleport them to the world
					// 如果发送者是玩家，将他们传送到世界
					if (player != null) {
						player.teleport(world.getSpawnLocation());
						sender.sendMessage(Messager.getFormattedMessage("TELEPORTED_TO_WORLD", null, null, null, world.getName()));
					}
				} else
					// Send failure message
					// 发送失败消息
					sender.sendMessage(Messager.getFormattedMessage("COULD_NOT_LOAD_WORLD", null, null, null, worldName));
			}
			// Handle unloadworld command to unload a world
			// 处理unloadworld命令以卸载世界
			else if (args[0].equalsIgnoreCase("unloadworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get world name from arguments
				// 从参数获取世界名称
				final String worldName = args[1];
				// Get players in the world
				// 获取世界中的玩家
				final List<Player> players = Bukkit.getWorld(worldName).getPlayers();
				// If there are players in the world, teleport them to another world
				// 如果世界中有玩家，将他们传送到另一个世界
				if (players.size() > 0) {
					// Find another world to teleport players to
					// 查找另一个世界以传送玩家
					final Location spawnLoc = Bukkit.getServer().getWorlds().stream()
							.filter(world -> world.getName() != worldName).collect(Collectors.toList()).get(0)
							.getSpawnLocation();
					// Teleport each player to the other world
					// 将每个玩家传送到另一个世界
					players.forEach(p -> p.teleport(spawnLoc));
				}
				// Unload the world
				// 卸载世界
				if (Bukkit.getServer().unloadWorld(worldName, false))
					// Send success message
					// 发送成功消息
					sender.sendMessage(Messager.getFormattedMessage("UNLOADED_WORLD", null, null, null, worldName));
				else
					// Send failure message
					// 发送失败消息
					sender.sendMessage(Messager.getFormattedMessage("COULD_NOT_UNLOAD_WORLD", null, null, null, worldName));
			}
			// Handle configstring command to get player configuration
			// 处理configstring命令以获取玩家配置
			else if (args[0].equalsIgnoreCase("configstring")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get player configuration
				// 获取玩家配置
				Skywars.get().getPlayerConfig(player);
			}
			// Handle xmat command to give player an XMaterial item
			// 处理xmat命令以给玩家一个XMaterial物品
			else if (args[0].equalsIgnoreCase("xmat")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Add the specified XMaterial item to player's inventory
				// 将指定的XMaterial物品添加到玩家背包
				player.getInventory().addItem(XMaterial.matchXMaterial(args[1]).get().parseItem());
			}
			// Handle encoding command to test encoding
			// 处理encoding命令以测试编码
			else if (args[0].equalsIgnoreCase("encoding")) {
				// Send a test message to check encoding
				// 发送测试消息以检查编码
				player.sendMessage(Skywars.langConfig.getString("died.title"));
			}
			// Handle class command to test if a class exists
			// 处理class命令以测试类是否存在
			else if (args[0].equalsIgnoreCase("class")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Boolean bool = false;
				// Try to load the class and see if it exists
				// 尝试加载类并查看是否存在
				try {
					Class.forName(args[1]);
					bool = true;
				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
				}
				// Send result of the test
				// 发送测试结果
				player.sendMessage(Messager.getFormattedMessage("TESTING_BOOLEAN_RESULT", null, null, null, bool));
			}
			// Handle changehologram command to change hologram text
			// 处理changehologram命令以更改全息文本
			else if (args[0].equalsIgnoreCase("changehologram")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Change the hologram text at the specified position
				// 更改指定位置的全息文本
				Skywars.get().getHologramController().changeHologram(args[1], args[2], Integer.parseInt(args[3]));
			}
			// Handle createhologram command to create a hologram
			// 处理createhologram命令以创建全息
			else if (args[0].equalsIgnoreCase("createhologram")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Create a hologram at the player's location
				// 在玩家位置创建全息
				Skywars.get().getHologramController().createHologram(args[1], player.getLocation(), args[2]);
			}
			// Handle tinylittletest command for testing purposes
			// 处理tinylittletest命令以进行测试
			else if (args[0].equalsIgnoreCase("tinylittletest")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Create test blocks in the arena
				// 在竞技场中创建测试方块
				final Player p = player;
				int e = 0;
				// Create green glass blocks in a range
				// 在范围内创建绿色玻璃方块
				for (int i = -arena.getMap().getCenterRadius(); i < arena.getMap().getCenterRadius(); i++) {
					e++;
					final int a = e;
					this.anotherTask = Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
						@Override
						public void run() {
							p.sendBlockChange(arena.getVectorInArena(new Vector(a, a, a)),
									XMaterial.GREEN_STAINED_GLASS.parseMaterial(), (byte) 0);
						}
					}, e);
				}
				// Restore original blocks after setting them
				// 设置方块后恢复原始方块
				for (int i = -arena.getMap().getCenterRadius(); i < arena.getMap().getCenterRadius(); i++) {
					e++;
					final int a = e;
					this.anotherTask = Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
						@Override
						public void run() {
							final Location l = arena.getVectorInArena(new Vector(a, a, a));
							final Block b = l.getBlock();
							p.sendBlockChange(l, b.getType(), b.getData());
						}
					}, e);
				}
			}
			// Handle bigcase command to create a large case
			// 处理bigcase命令以创建大型箱子
			else if (args[0].equalsIgnoreCase("bigcase")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Create a big case at player's location
				// 在玩家位置创建大箱子
				Skywars.createBigCase(player.getLocation(), XMaterial.LIME_STAINED_GLASS);
			}
			// Handle actionbar command to send action bar message
			// 处理actionbar命令以发送动作栏消息
			else if (args[0].equalsIgnoreCase("actionbar")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Send action bar message to player
				// 向玩家发送动作栏消息
				Skywars.get().NMS().sendActionbar(player, "hola");
			}
			// Handle pasteschematic command to paste a schematic
			// 处理pasteschematic命令以粘贴图纸
			else if (args[0].equalsIgnoreCase("pasteschematic")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Check if schematic is loaded
				// 检查图纸是否已加载
				if (this.schematic == null) {
					sender.sendMessage(Messager.getMessage("SCHEMATIC_NOT_LOADED"));
					return true;
				}
				// Paste the schematic at player's location
				// 在玩家位置粘贴图纸
				SchematicHandler.pasteSchematic(player.getLocation(), this.schematic);
			}
			// Handle debugdata command to get block data
			// 处理debugdata命令以获取方块数据
			else if (args[0].equalsIgnoreCase("debugdata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get target block and modify its data
				// 获取目标方块并修改其数据
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block != null)
					block.setData((byte) (block.getData() + 1));
				// Send block data information
				// 发送方块数据信息
				player.sendMessage(Messager.getFormattedMessage("BLOCK_DATA_INFO", null, null, null, block.getData()));
			}
			// Handle metadata command to get block metadata
			// 处理metadata命令以获取方块元数据
			else if (args[0].equalsIgnoreCase("metadata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get target block and its metadata
				// 获取目标方块及其元数据
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block == null)
					return true;
				// Send metadata information for each value
				// 为每个值发送元数据信息
				for (final MetadataValue value : block.getMetadata("facing")) {
					player.sendMessage(Messager.getFormattedMessage("BLOCK_DATA_INFO", null, null, null, value.asString()));
				}
			}
			// Handle setdata command to set block data
			// 处理setdata命令以设置方块数据
			else if (args[0].equalsIgnoreCase("setdata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get target block and set its data
				// 获取目标方块并设置其数据
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block == null)
					return true;
				if (args[1] == null)
					return true;
				block.setData(Byte.parseByte(args[1]));
			}
			// Handle setmetadata command to set block metadata
			// 处理setmetadata命令以设置方块元数据
			else if (args[0].equalsIgnoreCase("setmetadata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get metadata value from arguments
				// 从参数获取元数据值
				final String newmeta = args[1];
				if (newmeta == null)
					return true;
				// Get target block and set its metadata
				// 获取目标方块并设置其元数据
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block == null)
					return true;
				// Set block data using horizontal index
				// 使用水平索引设置方块数据
				block.setData(SchematicHandler.getHorizontalIndex(newmeta, Byte.parseByte(args[2])));
			}
			// Handle loadschematic command to load a schematic
			// 处理loadschematic命令以加载图纸
			else if (args[0].equalsIgnoreCase("loadschematic")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Load schematic from file
				// 从文件加载图纸
				final File file = new File(Skywars.get().getDataFolder() + "/schematics/" + args[1]);
				if (!file.exists()) {
					sender.sendMessage(Messager.getFormattedMessage("FILE_NOT_FOUND", null, null, null, args[1]));
					return true;
				}
				// Load the schematic and send confirmation message
				// 加载图纸并发送确认消息
				SchematicHandler.loadSchematic(file);
				sender.sendMessage(Messager.getFormattedMessage("LOADED_SCHEMATIC", null, null, null, file.getName()));
			}
			// Handle nms command to test NMS features
			// 处理nms命令以测试NMS功能
			else if (args[0].equalsIgnoreCase("nms")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Send title and action bar messages using NMS
				// 使用NMS发送标题和动作栏消息
				Skywars.get().NMS().sendTitle(player, "&6&lHello", "&eThis is a NMS test");
				Skywars.get().NMS().sendActionbar(player, "&cHello! This is a NMS test");
				final Location loc = player.getLocation();
				final Player p = player;
				// Cancel any existing timer tasks
				// 取消任何现有的计时器任务
				this.cancelTimer();
				// Start a new task to play notes
				// 开始新任务以播放音符
				this.task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
					byte note = 0;

					@Override
					public void run() {
						p.playNote(loc, (byte) 0, this.note);
						this.note++;
						// Stop when reached 25 notes
						// 达到25个音符时停止
						if (this.note >= 25)
							MainCommand.this.cancelTimer();
					}
				}, 0L, 1L);
				// Send particle effects
				// 发送粒子效果
				Skywars.get().NMS().sendParticles(player, "EXPLOSION_HUGE", 0);
			}
			// Handle worldname command to get map's world name
			// 处理worldname命令以获取地图的世界名称
			else if (args[0].equalsIgnoreCase("worldname")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Check if map has a world name set
				// 检查地图是否设置了世界名称
				if (map.getWorldName() != null)
					player.sendMessage(map.getWorldName());
				else
					player.sendMessage(Messager.getMessage("NOT_SET"));
			}
			// Handle deleteplayer command to remove a player entity (dangerous!)
			// 处理deleteplayer命令以移除玩家实体（危险！）
			else if (args[0].equalsIgnoreCase("deleteplayer")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Check if player is a valid entity and remove it
				// 检查玩家是否为有效实体并移除它
				if (player != null && player instanceof Entity) {
					final Entity entity = player;
					entity.remove();
				}
			}
			// Handle worlds command to list all worlds
			// 处理worlds命令以列出所有世界
			else if (args[0].equalsIgnoreCase("worlds")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Send list of all worlds on the server
				// 发送服务器上所有世界的列表
				sender.sendMessage(Messager.getFormattedMessage("WORLDS_LIST", null, null, null, String.join(", ", Bukkit.getServer().getWorlds().stream()
						.map(world -> world.getName()).collect(Collectors.toList()))));
			}
			// Handle where command to get player's world name
			// 处理where命令以获取玩家的世界名称
			else if (args[0].equalsIgnoreCase("where")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Send player's current world name (no localization for world names)
				// 发送玩家当前的世界名称（世界名称不需要本地化）
				player.sendMessage(player.getWorld().getName());
			}
			// Handle tp command to teleport player to arena center
			// 处理tp命令以传送玩家到竞技场中心
			else if (args[0].equalsIgnoreCase("tp")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Teleport player to center of the arena
				// 传送玩家到竞技场中心
				player.teleport(arena.getCenterBlock().toLocation(arena.getWorld()));
			}
			// Handle getarenaconfig command to get configuration value
			// 处理getarenaconfig命令以获取配置值
			else if (args[0].equalsIgnoreCase("getarenaconfig")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get value from arena map's config
				// 从竞技场地图的配置中获取值
				Object xd = map.getConfig().get(args[2]);
				if (xd == null)
					xd = "lmao it doesnt exist";
				sender.sendMessage(xd.toString());
			}
			// Handle getconfig command to get global config value
			// 处理getconfig命令以获取全局配置值
			else if (args[0].equalsIgnoreCase("getconfig")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get value from plugin's main config
				// 从插件的主配置中获取值
				Object xd = Skywars.get().getConfig().get(args[1]);
				if (xd == null)
					xd = "lmao it doesnt exist";
				sender.sendMessage(xd.toString());
			}
			// Handle scoreboard command to update player's scoreboard
			// 处理scoreboard命令以更新玩家的记分板
			else if (args[0].equalsIgnoreCase("scoreboard")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Update the player's scoreboard
				// 更新玩家的记分板
				SkywarsScoreboard.update(player);
			}
			// Handle getblock command to get target block information
			// 处理getblock命令以获取目标方块信息
			else if (args[0].equalsIgnoreCase("getblock")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get target block and broadcast its location
				// 获取目标方块并广播其位置
				final Block block = SkywarsUtils.getTargetBlock(player, 5);
				Bukkit.broadcastMessage(String.format("block is at %s", block.getLocation()));
			}
			// Handle fillchest command to fill a chest with items
			// 处理fillchest命令以用物品填充箱子
			else if (args[0].equalsIgnoreCase("fillchest")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get target block and fill it as a chest
				// 获取目标方块并将其填充为箱子
				final Block block = SkywarsUtils.getTargetBlock(player, 5);
				ChestManager.fillChest(block.getLocation(), false);
			}
			// Handle set command to set block material data
			// 处理set命令以设置方块材质数据
			else if (args[0].equalsIgnoreCase("set")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get target block and set its material data
				// 获取目标方块并设置其材质数据
				final Block block = SkywarsUtils.getTargetBlock(player, 5);
				final MaterialData data = new MaterialData(block.getType(), Byte.parseByte(args[1]));
				// Send debug messages about the material data
				// 发送关于材质数据的调试消息
				Skywars.get().sendDebugMessage("mat data: " + data);
				final BlockState state = block.getState();
				Skywars.get().sendDebugMessage(" block state: " + state.getData().getData());
				// Update the block state with new data
				// 使用新数据更新方块状态
				state.setData(data);
				state.update();
			}
			// Handle resetcases command to reset all spawn cases
			// 处理resetcases命令以重置所有出生箱子
			else if (args[0].equalsIgnoreCase("resetcases")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Reset all spawn cases in the arena
				// 重置竞技场中的所有出生箱子
				arena.resetCases();
				player.sendMessage("regenerated");
			}
			// Handle players command to list players in an arena
			// 处理players命令以列出竞技场中的玩家
			else if (args[0].equalsIgnoreCase("players")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// List all players in the arena
				// 列出竞技场中的所有玩家
				if (arena != null) {
					for (final SkywarsUser swp : arena.getUsers()) {
						if (swp == null)
							sender.sendMessage("null");
						else
							sender.sendMessage(String.format("%s", swp.getPlayer().getName()));
					}
				}
			}
			// Handle stop command to stop the current game
			// 处理stop命令以停止当前游戏
			else if (args[0].equalsIgnoreCase("stop")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Clear the player's arena to stop the game
				// 清除玩家的竞技场以停止游戏
				if (playerArena != null) {
					playerArena.clear();
				}
			}
			// Handle end command to end the current game
			// 处理end命令以结束当前游戏
			else if (args[0].equalsIgnoreCase("end")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// End the game in the player's arena
				// 结束玩家竞技场中的游戏
				if (playerArena != null) {
					if (!playerArena.endGame())
						sender.sendMessage("could not end");
				}
			}
			// Handle forcestop command to force stop the game
			// 处理forcestop命令以强制停止游戏
			else if (args[0].equalsIgnoreCase("forcestop")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Force clear the player's arena to stop the game
				// 强制清除玩家的竞技场以停止游戏
				if (playerArena != null) {
					playerArena.clear();
				}
			}
			// Handle calculatespawns command to calculate spawn points
			// 处理calculatespawns命令以计算出生点
			else if (args[0].equalsIgnoreCase("calculatespawns")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Calculate spawn points for the map
				// 为地图计算出生点
				map.calculateSpawns();
			}
			// Handle joined command to check if player is in an arena
			// 处理joined命令以检查玩家是否在竞技场中
			else if (args[0].equalsIgnoreCase("joined")) {
				// Send message indicating if player is in an arena
				// 发送消息指示玩家是否在竞技场中
				sender.sendMessage(Skywars.get().getPlayerArena(player) != null ? Messager.getMessage("JOINED") : Messager.getMessage("NOT_JOINED"));
			}
			// Handle case command to create a case at player's location
			// 处理case命令以在玩家位置创建箱子
			else if (args[0].equalsIgnoreCase("case")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Create a case at player's location
				// 在玩家位置创建箱子
				Skywars.createCase(player.getLocation(), XMaterial.GLASS);
			}
			// Handle spawn command to manage spawn points
			// 处理spawn命令以管理出生点
			else if (args[0].equalsIgnoreCase("spawn")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				// Get map name from arguments
				// 从参数获取地图名称
				final String nameMap = args[2];
				final SkywarsMap mapSpawn = Skywars.get().getMapManager().getMap(nameMap);
				// Check if map exists
				// 检查地图是否存在
				if (mapSpawn == null) {
					player.sendMessage(Messager.getFormattedMessage("NO_ARENA_FOUND_BY_NAME", null, null, null, nameMap));
					return true;
				}
				// Handle spawn list command
				// 处理spawn list命令
				if (args[1].equalsIgnoreCase("list")) {
					final int n = mapSpawn.getSpawns().size();
					// Send spawn count information
					// 发送出生点数量信息
					player.sendMessage(Messager.getFormattedMessage("SPAWNS_OF_ARENA_INFO", null, null, null, mapSpawn.getName(), n));
					// List all spawn points with their coordinates
					// 列出所有出生点及其坐标
					for (int i = 0; i < n; i++) {
						if (mapSpawn.getSpawn(i) == null) {
							// Send message if spawn is null
							// 如果出生点为空则发送消息
							player.sendMessage(Messager.getFormattedMessage("SPAWN_NULL_INFO", null, null, null, i));
							continue;
						}
						// Send spawn coordinates
						// 发送出生点坐标
						player.sendMessage(Messager.getFormattedMessage("SPAWN_COORDS_INFO", null, null, null, i, mapSpawn.getSpawn(i).getX(),
								mapSpawn.getSpawn(i).getY(), mapSpawn.getSpawn(i).getZ()));
					}
					return true;
				}
				final int spawn = Integer.parseInt(args[3]);
				// Handle spawn nullcheck command
				// 处理spawn nullcheck命令
				if (args[1].equalsIgnoreCase("nullcheck")) {
					final Vector arenaSpawnLocation = mapSpawn.getSpawn(spawn);
					// Send message indicating if spawn exists
					// 发送消息指示出生点是否存在
					sender.sendMessage(arenaSpawnLocation == null ? Messager.getMessage("SPAWN_IS_NULL") : Messager.getMessage("SPAWN_EXISTS"));
				}
				// Handle spawn set command
				// 处理spawn set命令
				else if (args[1].equalsIgnoreCase("set")) {
					// TODO calculate location relative to arena location
					// TODO 计算相对于竞技场位置的位置
					// mapSpawn.setSpawn(spawn, player.getLocation().toVector());
					// Send confirmation message
					// 发送确认消息
					player.sendMessage(Messager.getFormattedMessage("SET_SPAWN_OF_ARENA", null, null, null, spawn, mapSpawn.getName()));
				}
				// Handle spawn tp (teleport) command
				// 处理spawn tp（传送）命令
				else if (args[1].equalsIgnoreCase("tp")) {
					// Teleport player to the specified spawn point
					// 传送玩家到指定的出生点
					player.teleport(arena.getVectorInArena(mapSpawn.getSpawn(spawn)));
					// Send teleport confirmation message
					// 发送传送确认消息
					player.sendMessage(Messager.getFormattedMessage("TELEPORTED_TO_SPAWN_OF_ARENA", null, null, null, spawn, mapSpawn.getName()));
				}
			}
			// Handle arenas command to list all arenas
			// 处理arenas命令以列出所有竞技场
			else if (args[0].equalsIgnoreCase("arenas")) {
				// Check admin permission to list arenas
				// 检查管理员权限以列出竞技场
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final List<Arena> arenas = Skywars.get().getArenas();
				if (arenas != null && arenas.size() > 0) {
					// Get arena names and send them to the sender
					// 获取竞技场名称并发送给发送者
					final List<String> arenaNames = arenas.stream().map(m -> m.getMap().getName())
							.collect(Collectors.toList());
					sender.sendMessage(Messager.getFormattedMessage("ARENAS_LIST", null, null, null, String.join(", ", arenaNames)));
				} else
					sender.sendMessage(Messager.getMessage("NO_ARENAS"));
			}
			// Handle maps command to list all maps
			// 处理maps命令以列出所有地图
			else if (args[0].equalsIgnoreCase("maps")) {
				// Check admin permission to list maps
				// 检查管理员权限以列出地图
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final ArrayList<SkywarsMap> maps = Skywars.get().getMapManager().getMaps();
				if (maps != null && maps.size() > 0) {
					// Get map names and send them to the sender
					// 获取地图名称并发送给发送者
					final List<String> mapNames = maps.stream().map(m -> m.getName()).collect(Collectors.toList());
					sender.sendMessage(Messager.getFormattedMessage("MAPS_LIST", null, null, null, String.join(", ", mapNames)));
				} else
					sender.sendMessage(Messager.getMessage("NO_MAPS"));
			}
			// Handle create command to create a new map
			// 处理create命令以创建新地图
			else if (args[0].equalsIgnoreCase("create")) {
				// Check admin permission to create a map
				// 检查管理员权限以创建地图
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (name == null) {
					sender.sendMessage(Messager.getMessage("NO_NAME_SPECIFIED"));
					return true;
				}
				if (Skywars.get().getMapManager().getMap(name) != null) {
					sender.sendMessage(Messager.getFormattedMessage("MAP_ALREADY_EXISTS", null, null, null, name));
					return true;
				}
				// Create the new map and send confirmation messages
				// 创建新地图并发送确认消息
				if (Skywars.get().getMapManager().createMap(name)) {
					sender.sendMessage(Messager.getFormattedMessage("MAP_SUCCESSFULLY_CREATED", null, null, null, name));
					sender.sendMessage(Messager.getFormattedMessage("USE_CONFIG_TO_CONFIGURE_MAP", null, null, null, name));
				} else
					sender.sendMessage(Messager.getMessage("COULD_NOT_CREATE_MAP"));
			}
			// Handle delete command to delete a map
			// 处理delete命令以删除地图
			else if (args[0].equalsIgnoreCase("delete")) {
				// Check admin permission to delete a map
				// 检查管理员权限以删除地图
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (name == null) {
					sender.sendMessage(Messager.getMessage("NO_NAME_SPECIFIED"));
					return true;
				}
				if (Skywars.get().getMapManager().getMap(name) == null) {
					sender.sendMessage(Messager.getMessage("MAP_DOES_NOT_EXIST"));
					return true;
				}
				// Delete the map and send confirmation message
				// 删除地图并发送确认消息
				if (Skywars.get().getMapManager().deleteMap(name))
					sender.sendMessage(Messager.getMessage("MAP_SUCCESSFULLY_DELETED"));
				else
					sender.sendMessage(Messager.getMessage("COULD_NOT_DELETE_MAP"));
			}
			// Handle invalid command arguments
			// 处理无效的命令参数
			else {
				// Send message for invalid arguments
				// 发送无效参数的消息
				sender.sendMessage(Messager.getMessage("INVALID_ARGUMENTS"));
			}
			return true;
		} catch (final Exception e) {
			// Handle any exceptions during command execution
			// 处理命令执行期间的任何异常
			sender.sendMessage(Messager.getMessage("THERE_WAS_ERROR_EXECUTING_COMMAND"));
			final String m = e.getLocalizedMessage();
			if (m != null)
				sender.sendMessage(m);
			else
				sender.sendMessage(e.getClass().getName());
			e.printStackTrace();
		}
		return true;
	}

}