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

	Schematic schematic;

	BukkitTask task;
	BukkitTask anotherTask;

	void cancelTimer() {
		if (this.task != null)
			this.task.cancel();
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		try {
			if (args.length <= 0) {
				sender.sendMessage(Messager.getMessage("WELCOME_MESSAGE"));
				return true;
			}
			Player player = null;
			if (sender instanceof Player) {
				player = (Player) sender;
			}
			final boolean joined = Skywars.get().getPlayerArena(player) != null;
			String name = null;
			if (args.length > 1) {
				name = args[1];
			}
			final Arena playerArena = Skywars.get().getPlayerArena(player);
			final SkywarsMap map = Skywars.get().getMapManager().getMap(name);
			final Arena arena = ArenaManager.getArenaByMap(map, true);
			if (args[0].equalsIgnoreCase("setmainlobby")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				if (CommandsUtils.permissionCheckWithMessage(player, "skywars.setmainlobby")) {
					Skywars.get().setLobby(player.getLocation());
					player.sendMessage(Messager.getMessage("MAIN_LOBBY_SET"));
				}
			} else if (args[0].equalsIgnoreCase("lobby")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				if (joined) {
					Skywars.get().getPlayerArena(player).leavePlayer(player);
				}
				if (Skywars.get().getLobby() != null) {
					player.teleport(Skywars.get().getLobby());
					player.sendMessage(Messager.getMessage("TELEPORTED_TO_MAIN_LOBBY"));
				} else {
					player.sendMessage(Messager.getMessage("MAIN_LOBBY_NOT_SET"));
				}
			} else if (args[0].equalsIgnoreCase("play")) {
				MapMenu.open(player);
			} else if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("setup")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.config"))
					return true;
				final ArrayList<String> list = new ArrayList<String>(Arrays.asList(args));
				list.remove(0);
				final String _name = String.join(" ", list);
				final SkywarsMap _map = Skywars.get().getMapManager().getMap(_name);
				if (_map == null) {
					player.sendMessage(Messager.getMessage("NO_MAP"));
					return true;
				}
				ConfigMenu.OpenConfigurationMenu(player, _map);
			} else if (args[0].equalsIgnoreCase("help")) {
				sender.sendMessage(Messager.color("&a&lCommand list - &b%s &a%s &eby &b%s", Skywars.get().name, Skywars.get().version,
						String.join(", ", Skywars.get().authors)));
				sender.sendMessage(Messager.getMessage("HELP_SETMAINLOBBY"));
				sender.sendMessage(Messager.getMessage("HELP_LOBBY"));
				sender.sendMessage(Messager.getMessage("HELP_CREATE"));
				sender.sendMessage(Messager.getMessage("HELP_DELETE"));
				sender.sendMessage(Messager.getMessage("HELP_CONFIG"));
				sender.sendMessage(Messager.getMessage("HELP_PLAY"));
				sender.sendMessage(Messager.getMessage("HELP_START"));
				sender.sendMessage(Messager.getMessage("HELP_FORCESTART"));
			} else if (args[0].equalsIgnoreCase("leave")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				if (CommandsUtils.arenaCheckWithMessage(player))
					playerArena.leavePlayer(player);

				// TODO add the option to pass an arena name as an argument
			} else if (args[0].equalsIgnoreCase("forcestart")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				playerArena.startGame(player);
			} else if (args[0].equalsIgnoreCase("start")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				playerArena.softStart(player);
			} else if (args[0].equalsIgnoreCase("join")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				// if(!SkywarsUtils.JoinableCheck(arena, player)) return true;
				ArenaManager.joinMap(map, player);
			} else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("about")
					|| args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("version")) {
				sender.sendMessage(Messager.getFormattedMessage("PLUGIN_INFO", player, null, null, Skywars.get().name,
						Skywars.get().version, String.join(", ", Skywars.get().authors)));
			} else if (args[0].equalsIgnoreCase("server")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				sender.sendMessage(Messager.getMessage("SERVER_VERSION_TITLE"));
				sender.sendMessage(Bukkit.getServer().getVersion());
				sender.sendMessage(Bukkit.getServer().getBukkitVersion());
			} else if (args[0].equalsIgnoreCase("refill")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (!CommandsUtils.arenaCheckWithMessage(player))
					return true;
				playerArena.fillChests();
				playerArena.broadcastRefillMessage();
			} else if (args[0].equalsIgnoreCase("menu")) {
				if (!CommandsUtils.consoleCheckWithMessage(sender))
					return true;
				GamesMenu.open(player);
			} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.get().Reload();
				sender.sendMessage(Messager.getMessage("RELOADED"));
			}

			// else if(!CommandsUtils.permissionCheckWithMessage(player, "skywars.test"))
			// return true;
			// TEST COMMANDS

			else if (args[0].equalsIgnoreCase("testconfig")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
			} else if (args[0].equalsIgnoreCase("importworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final World world = player.getWorld();

				player.sendMessage(Messager.getMessage("TELEPORTING_PLAYERS_OUT_OF_WORLD"));
				for (final Player p : world.getPlayers())
					SkywarsUtils.teleportPlayerBackToTheLobbyOrToTheirLastLocationIfTheLobbyIsNotSet(p, true);

				if (!Bukkit.unloadWorld(world, true)) {
					player.sendMessage(Messager.getMessage("COULD_NOT_SAVE_AND_UNLOAD_WORLD"));
					return true;
				}
				player.sendMessage(Messager.getFormattedMessage("SAVED_AND_UNLOADED_WORLD", null, null, null, world.getName()));

				final File worldFolder = new File(Bukkit.getWorldContainer(), world.getName());
				if (!worldFolder.exists()) {
					player.sendMessage(Messager.getFormattedMessage("COULD_NOT_FIND_WORLD_FOLDER", null, null, null, worldFolder.getAbsolutePath()));
					return true;
				}

				final File newFolder = new File(Skywars.worldsPath, world.getName());
				if (!worldFolder.renameTo(newFolder)) {
					player.sendMessage(Messager.getFormattedMessage("COULD_NOT_MOVE_WORLD_TO", null, null, null, newFolder.getAbsolutePath()));
				}
				player.sendMessage(Messager.getMessage("MOVED_WORLD_TO_PLUGINS_FOLDERS"));
				player.sendMessage(Messager.getMessage("SHOULD_BE_ABLE_TO_SEE_WORLD_WHEN_SELECTING"));
			} else if (args[0].equalsIgnoreCase("saveworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final World world = player.getWorld();
				world.save();
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
			} else if (args[0].equalsIgnoreCase("worldexists")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final String worldName = args[1];
				final boolean bol = Bukkit.getServer().getWorld(worldName) != null;
				sender.sendMessage(Messager.getFormattedMessage("WORLD_EXISTS_STATUS", null, null, null, bol));
			} else if (args[0].equalsIgnoreCase("tpworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final String worldName = args[1];
				if (player == null) {
					sender.sendMessage(Messager.getMessage("YOU_NEED_TO_BE_A_PLAYER"));
					return true;
				}
				final Location loc = new Location(Bukkit.getServer().getWorld(worldName), 0, 100, 0);
				if (player.teleport(loc))
					sender.sendMessage(Messager.getFormattedMessage("TELEPORTED_TO_WORLD", null, null, null, worldName));
				else
					sender.sendMessage(Messager.getFormattedMessage("COULD_NOT_TELEPORT_TO_WORLD", null, null, null, worldName));
			} else if (args[0].equalsIgnoreCase("checksetup")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final World world = player.getWorld();
				player.sendMessage(Messager.getFormattedMessage("CURRENT_WORLD_SETUP", null, null, null, world.getName()));
				player.sendMessage(Messager.getFormattedMessage("AUTO_SAVE_STATUS", null, null, null, world.isAutoSave()));
			} else if (args[0].equalsIgnoreCase("setupworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				MapManager.setupWorld(player.getWorld());
							player.sendMessage(Messager.getFormattedMessage("SET_UP_WORLD", null, null, null, player.getWorld().getName()));			} else if (args[0].equalsIgnoreCase("loadworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final String worldName = args[1];
				if (!(new File(Bukkit.getServer().getWorldContainer(), worldName).exists())) {
					sender.sendMessage(Messager.getMessage("WORLD_FOLDER_DOES_NOT_EXIST"));
					return true;
				}
				final World world = Bukkit.getServer().createWorld(new WorldCreator(worldName));
				if (world != null) {
					world.setGameRuleValue("doMobSpawning", "false");
					world.setAutoSave(false);
					sender.sendMessage(Messager.getFormattedMessage("LOADED_WORLD", null, null, null, world.getName()));
					if (player != null) {
						player.teleport(world.getSpawnLocation());
						sender.sendMessage(Messager.getFormattedMessage("TELEPORTED_TO_WORLD", null, null, null, world.getName()));
					}
				} else
					sender.sendMessage(Messager.getFormattedMessage("COULD_NOT_LOAD_WORLD", null, null, null, worldName));
			} else if (args[0].equalsIgnoreCase("unloadworld")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final String worldName = args[1];
				final List<Player> players = Bukkit.getWorld(worldName).getPlayers();
				if (players.size() > 0) {
					final Location spawnLoc = Bukkit.getServer().getWorlds().stream()
							.filter(world -> world.getName() != worldName).collect(Collectors.toList()).get(0)
							.getSpawnLocation();
					players.forEach(p -> p.teleport(spawnLoc));
				}
				if (Bukkit.getServer().unloadWorld(worldName, false))
					sender.sendMessage(Messager.getFormattedMessage("UNLOADED_WORLD", null, null, null, worldName));
				else
					sender.sendMessage(Messager.getFormattedMessage("COULD_NOT_UNLOAD_WORLD", null, null, null, worldName));
			} else if (args[0].equalsIgnoreCase("configstring")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.get().getPlayerConfig(player);
			} else if (args[0].equalsIgnoreCase("xmat")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				player.getInventory().addItem(XMaterial.matchXMaterial(args[1]).get().parseItem());
			} else if (args[0].equalsIgnoreCase("encoding")) {
				player.sendMessage(Skywars.langConfig.getString("died.title"));
			} else if (args[0].equalsIgnoreCase("class")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Boolean bool = false;
				try {
					Class.forName(args[1]);
					bool = true;
				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
				}
				player.sendMessage(Messager.getFormattedMessage("TESTING_BOOLEAN_RESULT", null, null, null, bool));
			} else if (args[0].equalsIgnoreCase("changehologram")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.get().getHologramController().changeHologram(args[1], args[2], Integer.parseInt(args[3]));
			} else if (args[0].equalsIgnoreCase("createhologram")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.get().getHologramController().createHologram(args[1], player.getLocation(), args[2]);
			} else if (args[0].equalsIgnoreCase("tinylittletest")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final Player p = player;
				int e = 0;
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
			} else if (args[0].equalsIgnoreCase("bigcase")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.createBigCase(player.getLocation(), XMaterial.LIME_STAINED_GLASS);
			} else if (args[0].equalsIgnoreCase("actionbar")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.get().NMS().sendActionbar(player, "hola");
			} else if (args[0].equalsIgnoreCase("pasteschematic")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (this.schematic == null) {
					sender.sendMessage(Messager.getMessage("SCHEMATIC_NOT_LOADED"));
					return true;
				}
				SchematicHandler.pasteSchematic(player.getLocation(), this.schematic);
			} else if (args[0].equalsIgnoreCase("debugdata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block != null)
					block.setData((byte) (block.getData() + 1));
				player.sendMessage(Messager.getFormattedMessage("BLOCK_DATA_INFO", null, null, null, block.getData()));
			} else if (args[0].equalsIgnoreCase("metadata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block == null)
					return true;
				for (final MetadataValue value : block.getMetadata("facing")) {
					player.sendMessage(Messager.getFormattedMessage("BLOCK_DATA_INFO", null, null, null, value.asString()));
				}
			} else if (args[0].equalsIgnoreCase("setdata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block == null)
					return true;
				if (args[1] == null)
					return true;
				block.setData(Byte.parseByte(args[1]));
			} else if (args[0].equalsIgnoreCase("setmetadata")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final String newmeta = args[1];
				if (newmeta == null)
					return true;
				final Block block = SkywarsUtils.getTargetBlock(player, 10);
				if (block == null)
					return true;
				block.setData(SchematicHandler.getHorizontalIndex(newmeta, Byte.parseByte(args[2])));
			} else if (args[0].equalsIgnoreCase("loadschematic")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final File file = new File(Skywars.get().getDataFolder() + "/schematics/" + args[1]);
				if (!file.exists()) {
					sender.sendMessage(Messager.getFormattedMessage("FILE_NOT_FOUND", null, null, null, args[1]));
					return true;
				}
				SchematicHandler.loadSchematic(file);
				sender.sendMessage(Messager.getFormattedMessage("LOADED_SCHEMATIC", null, null, null, file.getName()));
			} else if (args[0].equalsIgnoreCase("nms")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.get().NMS().sendTitle(player, "&6&lHello", "&eThis is a NMS test");
				Skywars.get().NMS().sendActionbar(player, "&cHello! This is a NMS test");
				final Location loc = player.getLocation();
				final Player p = player;
				this.cancelTimer();
				this.task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
					byte note = 0;

					@Override
					public void run() {
						p.playNote(loc, (byte) 0, this.note);
						this.note++;
						if (this.note >= 25)
							MainCommand.this.cancelTimer();
					}
				}, 0L, 1L);
				Skywars.get().NMS().sendParticles(player, "EXPLOSION_HUGE", 0);
			} else if (args[0].equalsIgnoreCase("worldname")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (map.getWorldName() != null)
					player.sendMessage(map.getWorldName());
				else
					player.sendMessage(Messager.getMessage("NOT_SET"));
			} else if (args[0].equalsIgnoreCase("deleteplayer")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (player != null && player instanceof Entity) {
					final Entity entity = player;
					entity.remove();
				}
			} else if (args[0].equalsIgnoreCase("worlds")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				sender.sendMessage(Messager.getFormattedMessage("WORLDS_LIST", null, null, null, String.join(", ", Bukkit.getServer().getWorlds().stream()
						.map(world -> world.getName()).collect(Collectors.toList()))));
			} else if (args[0].equalsIgnoreCase("where")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				player.sendMessage(player.getWorld().getName());  // 这个是显示世界名称，不需要本地化
			} else if (args[0].equalsIgnoreCase("tp")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				player.teleport(arena.getCenterBlock().toLocation(arena.getWorld()));
			} else if (args[0].equalsIgnoreCase("getarenaconfig")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Object xd = map.getConfig().get(args[2]);
				if (xd == null)
					xd = "lmao it doesnt exist";
				sender.sendMessage(xd.toString());
			} else if (args[0].equalsIgnoreCase("getconfig")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Object xd = Skywars.get().getConfig().get(args[1]);
				if (xd == null)
					xd = "lmao it doesnt exist";
				sender.sendMessage(xd.toString());
			} else if (args[0].equalsIgnoreCase("scoreboard")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				SkywarsScoreboard.update(player);
			} else if (args[0].equalsIgnoreCase("getblock")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final Block block = SkywarsUtils.getTargetBlock(player, 5);
				Bukkit.broadcastMessage(String.format("block is at %s", block.getLocation()));
			} else if (args[0].equalsIgnoreCase("fillchest")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final Block block = SkywarsUtils.getTargetBlock(player, 5);
				ChestManager.fillChest(block.getLocation(), false);
			} else if (args[0].equalsIgnoreCase("set")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final Block block = SkywarsUtils.getTargetBlock(player, 5);
				final MaterialData data = new MaterialData(block.getType(), Byte.parseByte(args[1]));
				Skywars.get().sendDebugMessage("mat data: " + data);
				final BlockState state = block.getState();
				Skywars.get().sendDebugMessage(" block state: " + state.getData().getData());
				state.setData(data);
				state.update();
			} else if (args[0].equalsIgnoreCase("resetcases")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				arena.resetCases();
				player.sendMessage("regenerated");
			} else if (args[0].equalsIgnoreCase("players")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (arena != null) {
					for (final SkywarsUser swp : arena.getUsers()) {
						if (swp == null)
							sender.sendMessage("null");
						else
							sender.sendMessage(String.format("%s", swp.getPlayer().getName()));
					}
				}
			} else if (args[0].equalsIgnoreCase("stop")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (playerArena != null) {
					playerArena.clear();
				}
			} else if (args[0].equalsIgnoreCase("end")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (playerArena != null) {
					if (!playerArena.endGame())
						sender.sendMessage("could not end");
				}
			} else if (args[0].equalsIgnoreCase("forcestop")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				if (playerArena != null) {
					playerArena.clear();
				}
			} else if (args[0].equalsIgnoreCase("calculatespawns")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				map.calculateSpawns();
			} else if (args[0].equalsIgnoreCase("joined")) {
				sender.sendMessage(Skywars.get().getPlayerArena(player) != null ? Messager.getMessage("JOINED") : Messager.getMessage("NOT_JOINED"));  // 使用已有的JOINED和NOT_JOINED key
			} else if (args[0].equalsIgnoreCase("case")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				Skywars.createCase(player.getLocation(), XMaterial.GLASS);
			} else if (args[0].equalsIgnoreCase("spawn")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final String nameMap = args[2];
				final SkywarsMap mapSpawn = Skywars.get().getMapManager().getMap(nameMap);
				if (mapSpawn == null) {
					player.sendMessage(Messager.getFormattedMessage("NO_ARENA_FOUND_BY_NAME", null, null, null, nameMap));
					return true;
				}
				if (args[1].equalsIgnoreCase("list")) {
					final int n = mapSpawn.getSpawns().size();
					player.sendMessage(Messager.getFormattedMessage("SPAWNS_OF_ARENA_INFO", null, null, null, mapSpawn.getName(), n));
					for (int i = 0; i < n; i++) {
						if (mapSpawn.getSpawn(i) == null) {
							player.sendMessage(Messager.getFormattedMessage("SPAWN_NULL_INFO", null, null, null, i));
							continue;
						}
										player.sendMessage(Messager.getFormattedMessage("SPAWN_COORDS_INFO", null, null, null, i, mapSpawn.getSpawn(i).getX(),
												mapSpawn.getSpawn(i).getY(), mapSpawn.getSpawn(i).getZ()));					}
					return true;
				}
				final int spawn = Integer.parseInt(args[3]);
				if (args[1].equalsIgnoreCase("nullcheck")) {
					final Vector arenaSpawnLocation = mapSpawn.getSpawn(spawn);
					sender.sendMessage(arenaSpawnLocation == null ? Messager.getMessage("SPAWN_IS_NULL") : Messager.getMessage("SPAWN_EXISTS"));
				} else if (args[1].equalsIgnoreCase("set")) {
					// TODO calculate location relative to arena location
					// mapSpawn.setSpawn(spawn, player.getLocation().toVector());
									player.sendMessage(Messager.getFormattedMessage("SET_SPAWN_OF_ARENA", null, null, null, spawn, mapSpawn.getName()));				} else if (args[1].equalsIgnoreCase("tp")) {
					player.teleport(arena.getVectorInArena(mapSpawn.getSpawn(spawn)));
									player.sendMessage(
											Messager.getFormattedMessage("TELEPORTED_TO_SPAWN_OF_ARENA", null, null, null, spawn, mapSpawn.getName()));				}
			} else if (args[0].equalsIgnoreCase("arenas")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final List<Arena> arenas = Skywars.get().getArenas();
				if (arenas != null && arenas.size() > 0) {
					final List<String> arenaNames = arenas.stream().map(m -> m.getMap().getName())
							.collect(Collectors.toList());
					sender.sendMessage(Messager.getFormattedMessage("ARENAS_LIST", null, null, null, String.join(", ", arenaNames)));
				} else
					sender.sendMessage(Messager.getMessage("NO_ARENAS"));
			} else if (args[0].equalsIgnoreCase("maps")) {
				if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
					return true;
				final ArrayList<SkywarsMap> maps = Skywars.get().getMapManager().getMaps();
				if (maps != null && maps.size() > 0) {
					final List<String> mapNames = maps.stream().map(m -> m.getName()).collect(Collectors.toList());
					sender.sendMessage(Messager.getFormattedMessage("MAPS_LIST", null, null, null, String.join(", ", mapNames)));
				} else
					sender.sendMessage(Messager.getMessage("NO_MAPS"));
			} else if (args[0].equalsIgnoreCase("create")) {
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
				if (Skywars.get().getMapManager().createMap(name)) {
					sender.sendMessage(Messager.getFormattedMessage("MAP_SUCCESSFULLY_CREATED", null, null, null, name));
					sender.sendMessage(Messager.getFormattedMessage("USE_CONFIG_TO_CONFIGURE_MAP", null, null, null, name));
				} else
					sender.sendMessage(Messager.getMessage("COULD_NOT_CREATE_MAP"));
			} else if (args[0].equalsIgnoreCase("delete")) {
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
				if (Skywars.get().getMapManager().deleteMap(name))
					sender.sendMessage(Messager.getMessage("MAP_SUCCESSFULLY_DELETED"));
				else
					sender.sendMessage(Messager.getMessage("COULD_NOT_DELETE_MAP"));
			} else {
				sender.sendMessage(Messager.getMessage("INVALID_ARGUMENTS"));
			}
			return true;
		} catch (final Exception e) {
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