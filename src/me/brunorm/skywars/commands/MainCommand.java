package me.brunorm.skywars.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.ArenaMenu;
import me.brunorm.skywars.ArenaSetupMenu;
import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.ChestManager;
import me.brunorm.skywars.GamesMenu;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsScoreboard;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.API.NMSHandler;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsPlayer;
import mrblobman.sounds.Sounds;

public class MainCommand implements CommandExecutor {

	Skywars plugin;

	public MainCommand(Skywars plugin) {
		this.plugin = plugin;
	}

	/*
	String[] helpLines = { "&a&lSkywars commands", "&b/skywars create <arena> &e- creates an arena",
			"&b/skywars minplayers <arena> <number> &e- set a number of min players",
			"&b/skywars maxplayers <arena> <number> &e- set a number of min players",
			"&b/skywars world <arena> <world> &e- set the world of an arena to your current world",
			"&b/skywars position <arena> <x> <y> <z> &e- set the position of an arena to your current position",
			"&b/skywars schematic <arena> <schematic> &e- set the schematic file of an arena",
			"&b/skywars spawn <set/remove> <arena> <number> &e- set or remove the spawn of an arena",
			"&b/skywars enable <arena> &e- enables an arena", "&b/skywars disable <arena> &e- disables an arena",
			"&b/skywars setmainlobby &e- sets the main lobby to your current position",
			"&b/skywars menu &e- opens the games menu",
			"&b* You can use the shorthand version of the command: &e&l/sw" };
	*/
	
	String[] helpLines = {
			"&a&lSkywars commands",
			"&b/skywars setmainlobby &e- sets the main lobby",
			"&b/skywars lobby &e- teleports you to the main lobby",
			"&b/skywars create <arena> &e- creates an arena",
			"&b/skywars delete <arena> &e- deletes an arena",
			"&b/skywars config <arena> &e- opens the configuration menu",
			"&b/skywars play &e- open the arenas menu",
			"&b/skywars start &e- starts a game",
			"&b/skywars forcestart &e- starts a game immediately"
	};

	boolean permissionCheckWithMessage(Player player, String permission) {
		if(!player.hasPermission(permission)) {
			player.sendMessage(Messager.color(Skywars.get().langConfig.getString("NO_PERMISSION")));
			return false;
		}
		return true;
	}
	
	boolean arenaCheckWithMessage(Player player) {
		if(plugin.getPlayerArena(player) == null) {
			player.sendMessage(Messager.color(Skywars.get().langConfig.getString("NOT_JOINED")));
			return false;
		}
		return true;
	}
	
	Schematic schematic;
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		try {
			Player player = null;
			if (sender instanceof Player) {
				player = (Player) sender;
			}
			boolean joined = plugin.getPlayerArena(player) != null;
			if (args.length > 0) {
				String name = null;
				Arena arena = null;
				if(args.length > 1) {
					name = args[1];
				}
				Arena playerArena = plugin.getPlayerArena(player);
				if(name != null) arena = Skywars.get().getArena(name);
				if (args[0].equalsIgnoreCase("setmainlobby")) {
					if(permissionCheckWithMessage(player, "skywars.setmainlobby")) {						
						plugin.setLobby(player.getLocation());
						plugin.saveConfig();
						player.sendMessage(Messager.color("&eThe main lobby has been set to your current position."));
					}
				}
				if (args[0].equalsIgnoreCase("lobby")) {
					if(permissionCheckWithMessage(player, "skywars.setmainlobby")) {	
						if (joined) {
							Skywars.get().getPlayerArena(player).leavePlayer(player);
						}
						SkywarsUtils.TeleportToLobby(player);
						player.sendMessage(Messager.color("&eTeleported to the lobby!"));
					}
				}
				if(args[0].equalsIgnoreCase("play")) {
					ArenaMenu.open(player);
				}
				if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("setup")) {
					if(permissionCheckWithMessage(player, "skywars.config")) {
						if(arena == null) {
							player.sendMessage(Messager.color("&cNo arena."));
							return false;
						}
						ArenaSetupMenu.OpenConfigurationMenu(player, arena);
					}
				}
				if (args[0].equalsIgnoreCase("help")) {
					for (String line : helpLines) {
						sender.sendMessage(Messager.color(line));
					}
				}
				if (args[0].equalsIgnoreCase("leave")) {
					if (arenaCheckWithMessage(player))
						playerArena.leavePlayer(player);
				}
				if (args[0].equalsIgnoreCase("forcestart")) {
					if(permissionCheckWithMessage(player, "skywars.forcestart"))
						if (arenaCheckWithMessage(player))
							playerArena.startGame();
				}
				if (args[0].equalsIgnoreCase("start")) {
					if(permissionCheckWithMessage(player, "skywars.start")) {
						if (arenaCheckWithMessage(player)) {
							if(playerArena.getStatus() == ArenaStatus.WAITING &&
									playerArena.getTask() == null) {
								playerArena.forcedStart = true;
								playerArena.forcedStartPlayer = player;
								playerArena.startTimer(ArenaStatus.STARTING);
							} else {
								playerArena.startGame();
							}
						}
					}
				}
				if (args[0].equalsIgnoreCase("join")) {
					if(!SkywarsUtils.JoinableCheck(arena, player)) return false;
					arena.joinPlayer(player);
				}
				if (args[0].equalsIgnoreCase("version")) {
					sender.sendMessage(String.format("%s version %s made by %s", plugin.name, plugin.version,
							String.join(", ", plugin.authors)));
				}
				if (args[0].equalsIgnoreCase("menu")) {
					GamesMenu.OpenMenu(player);
				}
				if(!permissionCheckWithMessage(player, "skywars.test")) return false;
				if(args[0].equalsIgnoreCase("pasteschematic")) {
					if(schematic == null) {
						sender.sendMessage("schematic not loaded");
						return false;
					}
					SchematicHandler.pasteSchematic(player.getLocation(), schematic);
				}
				if(args[0].equalsIgnoreCase("loadschematic")) {
					File file = new File(Skywars.get().getDataFolder() + "/schematics/" + args[1]);
					if(!file.exists()) {
						sender.sendMessage("file not found: " + args[1]);
						return false;
					}
					schematic = SchematicHandler.loadSchematic(file);
				}
				if(args[0].equalsIgnoreCase("testnms")) {
					NMSHandler handler = new NMSHandler();
					handler.sendTitle(player, "hola", "esto es una prueba");
				}
				if(args[0].equalsIgnoreCase("soundnms")) {
					player.playSound(player.getLocation(), Sounds.NOTE_STICKS.bukkitSound(), 1, 1);
				}
				if(args[0].equalsIgnoreCase("nms")) {
					player.sendMessage("NMS test sent");
					Skywars.get().NMS().sendTitle(player, "Hello!", "This is a NMS test");
				}
				if(args[0].equalsIgnoreCase("worldname")) {
					if(arena.getWorldName() != null)
						player.sendMessage(arena.getWorldName());
					else
						player.sendMessage("not set");
				}
				if(args[0].equalsIgnoreCase("deleteplayer")) {
					if(player != null && player instanceof Entity) {
						Entity entity = (Entity) player;
						entity.remove();
					}
				}
				if(args[0].equalsIgnoreCase("worlds")) {
					player.sendMessage(String.join(", ",
						Bukkit.getServer().getWorlds().stream().map(world -> world.getName())
							.collect(Collectors.toList())));
				}
				if(args[0].equalsIgnoreCase("where")) {
					player.sendMessage(player.getWorld().getName());
				}
				if(args[0].equalsIgnoreCase("tp")) {
					player.teleport(arena.getLocation());
				}
				if(args[0].equalsIgnoreCase("getarenaconfig")) {
					Object xd = arena.getConfig().get(args[2]);
					if(xd == null) xd = "lmao it doesnt exist";
					sender.sendMessage(xd.toString());
				}
				if(args[0].equalsIgnoreCase("getconfig")) {
					Object xd = Skywars.get().getConfig().get(args[1]);
					if(xd == null) xd = "lmao it doesnt exist";
					sender.sendMessage(xd.toString());
				}
				if(args[0].equalsIgnoreCase("setconfig")) {
					plugin.getConfig().set(args[1], args[2]);
					plugin.saveConfig();
					sender.sendMessage("set");
				}
				if(args[0].equalsIgnoreCase("savearenaconfig")) {
					arena.saveParametersInConfig();
					sender.sendMessage("saved");
				}
				if(args[0].equalsIgnoreCase("reloadarenas")) {
					Skywars.get().loadArenas();
					sender.sendMessage("reloaded arenas");
				}
				if(args[0].equalsIgnoreCase("reload")) {
					Skywars.get().Reload();
				}
				if (args[0].equalsIgnoreCase("scoreboard")) {
					SkywarsScoreboard.update(player);
				}
				if (args[0].equalsIgnoreCase("reload")) {
					sender.sendMessage("reloaded");
				}
				if (args[0].equalsIgnoreCase("getblock")) {
					Block block = SkywarsUtils.getTargetBlock(player, 5);
					Bukkit.broadcastMessage(String.format("block is at %s", block.getLocation()));
				}
				if (args[0].equalsIgnoreCase("fillchest")) {
					Block block = SkywarsUtils.getTargetBlock(player, 5);
					ChestManager.fillChest(block.getLocation());
				}
				if (args[0].equalsIgnoreCase("testschem")) {
					arena.pasteSchematic();
					player.sendMessage("pasted");
				}
				if (args[0].equalsIgnoreCase("resetcases")) {
					arena.resetCases();
					player.sendMessage("regenerated");
				}
				if (args[0].equalsIgnoreCase("players")) {
					if (arena != null) {
						for (SkywarsPlayer swp : arena.getPlayers()) {
							if (swp == null)
								sender.sendMessage("null");
							else
								sender.sendMessage(String.format("%s", swp.getPlayer().getName()));
						}
					}
				}
				if(args[0].equalsIgnoreCase("restart")) {
					arena.clear();
				}
				if (args[0].equalsIgnoreCase("stop")) {
					if (playerArena != null) {
						playerArena.startTimer(ArenaStatus.ENDING);
					}
				}
				if (args[0].equalsIgnoreCase("forcestop")) {
					if (arena != null) {
						arena.stopGame();
					}
				}
				if(args[0].equalsIgnoreCase("calculatespawns")) {
					arena.calculateSpawns();
				}
				if (args[0].equalsIgnoreCase("save")) {
					// plugin.saveArenas();
					// sender.sendMessage("saved");
				}
				if (args[0].equalsIgnoreCase("joined")) {
					sender.sendMessage(plugin.getPlayerArena(player) != null ? "joined" : "not joined");
				}
				if (args[0].equalsIgnoreCase("case")) {
					Skywars.createCase(player.getLocation(), XMaterial.GLASS.parseMaterial(), Integer.parseInt(args[1]));
				}
				if (args[0].equalsIgnoreCase("spawn")) {
					String nameArena = args[2];
					Arena arenaSpawn = plugin.getArena(nameArena);
					if (arenaSpawn == null) {
						player.sendMessage(String.format("No arena found by '%s'", nameArena));
						return false;
					}
					if(args[1].equalsIgnoreCase("list")) {
						int n = arenaSpawn.getSpawns().size();
						player.sendMessage(String.format("spawns of arena %s (%s)", arenaSpawn.getName(), n));
						for(int i = 0; i < n; i++) {
							if(arenaSpawn.getSpawn(i) == null) {
								player.sendMessage(String.format("%s: null", i));
								continue;
							}
							player.sendMessage(String.format("%s: %s, %s, %s", i,
									arenaSpawn.getSpawn(i).getX(),
									arenaSpawn.getSpawn(i).getY(),
									arenaSpawn.getSpawn(i).getZ()));
						}
						return true;
					}
					int spawn = Integer.parseInt(args[3]);
					if (args[1].equalsIgnoreCase("nullcheck")) {
						Location arenaSpawnLocation = arenaSpawn.getSpawn(spawn);
						sender.sendMessage(arenaSpawnLocation == null ? "spawn is null" : "spawn exists");
					}
					if (args[1].equalsIgnoreCase("set")) {
						arenaSpawn.setSpawn(spawn, player.getLocation());
						player.sendMessage(String.format("Set spawn %s of arena '%s' to your current location", spawn,
								arenaSpawn.getName()));
					}
					if (args[1].equalsIgnoreCase("remove")) {
						if(!arenaSpawn.removeSpawn(spawn)) {
							sender.sendMessage("couldnt remove spawn");
							return false;
						}
						sender.sendMessage(String.format("Removed spawn %s of arena '%s'", spawn, arenaSpawn.getName()));
					}
					if (args[1].equalsIgnoreCase("tp")) {
						player.teleport(arenaSpawn.getSpawn(spawn));
						player.sendMessage(
								String.format("Teleported to spawn %s of arena '%s'", spawn, arenaSpawn.getName()));
					}
				}
				if (args[0].equalsIgnoreCase("arenas")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					ArrayList<Arena> arenas = plugin.getArenas();
					if (arenas != null && arenas.size() > 0) {
						String[] arenaNames = plugin.getArenaNames();
						/*
						List<String> arenaNames = arenas.stream().map(arena -> arena.getName())
								.collect(Collectors.toList());
						*/
						sender.sendMessage(String.format("Arenas: %s", String.join(", ", arenaNames)));
					} else
						sender.sendMessage("No arenas");
				}
				if (args[0].equalsIgnoreCase("create")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					if (name != null) {
						if (plugin.getArena(name) == null) {
							plugin.createArena(name);
							sender.sendMessage("Arena successfully created");
						} else {
							sender.sendMessage("Arena already exists");
						}
					} else {
						sender.sendMessage("No name");
					}
				}
				if (args[0].equalsIgnoreCase("delete")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					if (name != null) {
						if (plugin.getArena(name) != null) {
							plugin.deleteArena(name);
							sender.sendMessage("Arena successfully deleted");
						} else {
							sender.sendMessage("Arena doesn't exist");
						}
					} else {
						sender.sendMessage("No name");
					}
				}
				if (args[0].equalsIgnoreCase("status")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					sender.sendMessage(String.format("Arena status is %s", arena.getStatus()));
				}
				if (args[0].equalsIgnoreCase("enable")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					arena.setStatus(ArenaStatus.WAITING);
					arena.saveParametersInConfig();
					sender.sendMessage(String.format("Enabled arena '%s'", arena.getName()));
				}
				if (args[0].equalsIgnoreCase("disable")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					arena.setStatus(ArenaStatus.DISABLED);
					arena.saveParametersInConfig();
					sender.sendMessage(String.format("Disabled arena '%s'", arena.getName()));
				}
				if (args[0].equalsIgnoreCase("minplayers")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					int n = Integer.parseInt(args[2]);
					arena.setMinPlayers(n);
					arena.saveParametersInConfig();
					sender.sendMessage(String.format("min players set to %s", n));
				}
				if (args[0].equalsIgnoreCase("maxplayers")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					int n = Integer.parseInt(args[2]);
					arena.setMaxPlayers(n);
					arena.saveParametersInConfig();
					sender.sendMessage(String.format("max players set to %s", n));
				}
				if (args[0].equalsIgnoreCase("world")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					World world = player.getWorld();
					arena.setWorldName(world.getName());
					arena.saveParametersInConfig();
					sender.sendMessage(String.format("arena world set to %s", world.getName()));
				}
				if (args[0].equalsIgnoreCase("position")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					if (arena == null) {
						sender.sendMessage("invalid arena");
						return false;
					}
					double x;
					double y;
					double z;
					if (args.length < 5) {
						x = player.getLocation().getX();
						y = player.getLocation().getY();
						z = player.getLocation().getZ();
					} else {
						x = Integer.parseInt(args[2]);
						y = Integer.parseInt(args[3]);
						z = Integer.parseInt(args[4]);
					}
					World world = player.getWorld();
					if (world == null) {
						sender.sendMessage("invalid world");
						return false;
					}
					Location location = new Location(world, x, y, z);
					arena.setLocation(location);
					arena.saveParametersInConfig();
					player.sendMessage(String.format("set location of %s to %s %s %s in world %s", arena.getName(),
							location.getX(), location.getY(), location.getZ(), location.getWorld().getName()));
				}
				if (args[0].equalsIgnoreCase("schematic")) {
					if(!permissionCheckWithMessage(player, "skywars.admin")) return false;
					if (args.length >= 3) {
						String schematic = args[2];
						File file = new File(Skywars.get().getDataFolder() + "/schematics", schematic);
						if (!file.exists()) {
							sender.sendMessage(String.format(
									"File \"%s\" not found. make sure the file exists at the \"schematics\" folder",
									schematic));
							return false;
						}
						arena.setSchematic(schematic);
						arena.saveParametersInConfig();
						sender.sendMessage(String.format("arena schematic set to %s", arena.getSchematic()));
					} else {
						if (arena.getSchematic() == null) {
							sender.sendMessage("no schematic set");
							return false;
						}
						sender.sendMessage(String.format("arena schematic is %s", arena.getSchematic()));
					}
				}
				return true;
			} else {
				sender.sendMessage(Messager.color("&a&lSkyWars &e- /sw help"));
			}

		} catch (Exception e) {
			sender.sendMessage("there was an error executing the command:");
			sender.sendMessage(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return false;
	}

}