package me.brunorm.skywars.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.ChestManager;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsScoreboard;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.NMS.NMSHandler;
import me.brunorm.skywars.menus.GamesMenu;
import me.brunorm.skywars.menus.MapMenu;
import me.brunorm.skywars.menus.SetupMenu;
import me.brunorm.skywars.schematics.Schem;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;
import me.brunorm.skywars.structures.SkywarsPlayer;
import mrblobman.sounds.Sounds;

public class MainCommand implements CommandExecutor {

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
	
	Schematic schematic;
	Schem schem;
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		try {
			Player player = null;
			if (sender instanceof Player) {
				player = (Player) sender;
			}
			boolean joined = Skywars.get().getPlayerArena(player) != null;
			if (args.length > 0) {
				String name = null;
				if(args.length > 1) {
					name = args[1];
				}
				Arena playerArena = Skywars.get().getPlayerArena(player);
				SkywarsMap map = Skywars.get().getMap(name);
				Arena arena = Skywars.get().getJoinableArenaByMap(map);
				if (args[0].equalsIgnoreCase("setmainlobby")) {
					if(CommandsUtils.permissionCheckWithMessage(player, "skywars.setmainlobby")) {						
						Skywars.get().setLobby(player.getLocation());
						player.sendMessage(Messager.getMessage("MAIN_LOBBY_SET"));
					}
				}
				else if (args[0].equalsIgnoreCase("lobby")) {	
					if (joined) {
						Skywars.get().getPlayerArena(player).leavePlayer(player);
					}
					if(Skywars.get().getLobby() != null) {
						player.teleport(Skywars.get().getLobby());
						player.sendMessage(Messager.getMessage("TELEPORTED_TO_MAIN_LOBBY"));
					} else {
						player.sendMessage(Messager.getMessage("MAIN_LOBBY_NOT_SET"));
					}
				}
				else if(args[0].equalsIgnoreCase("play")) {
					MapMenu.open(player);
				}
				else if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("setup")) {
					if(CommandsUtils.permissionCheckWithMessage(player, "skywars.config")) {
						if(map == null) {
							player.sendMessage(Messager.getMessage("NO_MAP"));
							return false;
						}
						SetupMenu.OpenConfigurationMenu(player, map);
					}
				}
				else if (args[0].equalsIgnoreCase("help")) {
					for (String line : helpLines) {
						sender.sendMessage(Messager.color(line));
					}
				}
				else if (args[0].equalsIgnoreCase("leave")) {
					if (CommandsUtils.arenaCheckWithMessage(player))
						playerArena.leavePlayer(player);
				}
				else if (args[0].equalsIgnoreCase("forcestart")) {
					if(CommandsUtils.permissionCheckWithMessage(player, "skywars.forcestart"))
						if (CommandsUtils.arenaCheckWithMessage(player))
							playerArena.startGame();
				}
				else if (args[0].equalsIgnoreCase("start")) {
					if(CommandsUtils.permissionCheckWithMessage(player, "skywars.start")) {
						if (CommandsUtils.arenaCheckWithMessage(player)) {
							playerArena.softStart(player);
						}
					}
				}
				else if (args[0].equalsIgnoreCase("join")) {
					//if(!SkywarsUtils.JoinableCheck(arena, player)) return false;
					Skywars.get().joinMap(map, player);
				}
				else if (args[0].equalsIgnoreCase("version")) {
					sender.sendMessage(String.format("%s version %s made by %s",
							Skywars.get().name, Skywars.get().version,
							String.join(", ", Skywars.get().authors)));
				}
				else if (args[0].equalsIgnoreCase("menu")) {
					GamesMenu.OpenMenu(player);
				} else if (args[0].equalsIgnoreCase("reload")
						|| args[0].equalsIgnoreCase("rl")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Skywars.get().Reload();
					sender.sendMessage(Messager.getMessage("RELOADED"));
				}
				
				//else if(!CommandsUtils.permissionCheckWithMessage(player, "skywars.test")) return false;
				// TEST COMMANDS
				
				else if(args[0].equalsIgnoreCase("bigcase")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Skywars.createBigCase(player.getLocation(), XMaterial.LIME_STAINED_GLASS);
				}
				else if(args[0].equalsIgnoreCase("pasteschematic")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if(schematic == null) {
						sender.sendMessage("schematic not loaded");
						return false;
					}
					SchematicHandler.pasteSchematic(player.getLocation(), schematic);
				}
				else if(args[0].equalsIgnoreCase("loadschematic")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					File file = new File(Skywars.get().getDataFolder() + "/schematics/" + args[1]);
					if(!file.exists()) {
						sender.sendMessage("file not found: " + args[1]);
						return false;
					}
					Object loaded = SchematicHandler.loadSchematic(file);
					if(loaded instanceof Schematic) {
						System.out.println("Loading schematic!");
						schematic = (Schematic) loaded;
					} else if (loaded instanceof Schem) {
						System.out.println("Loading schem!");
						schem = (Schem) loaded;
					} else
						System.out.println("Unknown schematic type: " + loaded);
				}
				else if(args[0].equalsIgnoreCase("testnms")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					NMSHandler handler = new NMSHandler();
					handler.sendTitle(player, "hola", "esto es una prueba");
				}
				else if(args[0].equalsIgnoreCase("soundnms")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.playSound(player.getLocation(), Sounds.NOTE_STICKS.bukkitSound(), 1, 1);
				}
				else if(args[0].equalsIgnoreCase("nms")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.sendMessage("NMS test sent");
					Skywars.get().NMS().sendTitle(player, "Hello!", "This is a NMS test");
				}
				else if(args[0].equalsIgnoreCase("worldname")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if(map.getWorldName() != null)
						player.sendMessage(map.getWorldName());
					else
						player.sendMessage("not set");
				}
				else if(args[0].equalsIgnoreCase("deleteplayer")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if(player != null && player instanceof Entity) {
						Entity entity = (Entity) player;
						entity.remove();
					}
				}
				else if(args[0].equalsIgnoreCase("worlds")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.sendMessage(String.join(", ",
						Bukkit.getServer().getWorlds().stream().map(world -> world.getName())
							.collect(Collectors.toList())));
				}
				else if(args[0].equalsIgnoreCase("where")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.sendMessage(player.getWorld().getName());
				}
				else if(args[0].equalsIgnoreCase("tp")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.teleport(arena.getLocation());
				}
				else if(args[0].equalsIgnoreCase("getarenaconfig")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Object xd = map.getConfig().get(args[2]);
					if(xd == null) xd = "lmao it doesnt exist";
					sender.sendMessage(xd.toString());
				}
				else if(args[0].equalsIgnoreCase("getconfig")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Object xd = Skywars.get().getConfig().get(args[1]);
					if(xd == null) xd = "lmao it doesnt exist";
					sender.sendMessage(xd.toString());
				}
				else if (args[0].equalsIgnoreCase("scoreboard")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					SkywarsScoreboard.update(player);
				}
				else if (args[0].equalsIgnoreCase("getblock")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Block block = SkywarsUtils.getTargetBlock(player, 5);
					Bukkit.broadcastMessage(String.format("block is at %s", block.getLocation()));
				}
				else if (args[0].equalsIgnoreCase("fillchest")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Block block = SkywarsUtils.getTargetBlock(player, 5);
					ChestManager.fillChest(block.getLocation(), false);
				}
				else if(args[0].equalsIgnoreCase("set")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Block block = SkywarsUtils.getTargetBlock(player, 5);
					block.setData(Byte.parseByte(args[1]));
				}
				else if (args[0].equalsIgnoreCase("testschem")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					arena.pasteSchematic();
					player.sendMessage("pasted");
				}
				else if (args[0].equalsIgnoreCase("resetcases")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					arena.resetCases();
					player.sendMessage("regenerated");
				}
				else if (args[0].equalsIgnoreCase("players")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (arena != null) {
						for (SkywarsPlayer swp : arena.getAllPlayersIncludingAliveAndSpectators()) {
							if (swp == null)
								sender.sendMessage("null");
							else
								sender.sendMessage(String.format("%s", swp.getPlayer().getName()));
						}
					}
				}
				else if(args[0].equalsIgnoreCase("restart")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					arena.clear();
				}
				else if (args[0].equalsIgnoreCase("stop")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (playerArena != null) {
						playerArena.startTimer(ArenaStatus.ENDING);
					}
				}
				else if (args[0].equalsIgnoreCase("forcestop")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (arena != null) {
						arena.clear();
					}
				}
				else if(args[0].equalsIgnoreCase("calculatespawns")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					map.calculateSpawns();
				}
				else if (args[0].equalsIgnoreCase("save")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					// plugin.saveArenas();
					// sender.sendMessage("saved");
				}
				else if (args[0].equalsIgnoreCase("joined")) {
					sender.sendMessage(Skywars.get().getPlayerArena(player) != null ? "joined" : "not joined");
				}
				else if (args[0].equalsIgnoreCase("case")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Skywars.createCase(player.getLocation(), XMaterial.GLASS);
				}
				else if (args[0].equalsIgnoreCase("spawn")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					String nameMap = args[2];
					SkywarsMap mapSpawn = Skywars.get().getMap(nameMap);
					if (mapSpawn == null) {
						player.sendMessage(String.format("No arena found by '%s'", nameMap));
						return false;
					}
					if(args[1].equalsIgnoreCase("list")) {
						int n = mapSpawn.getSpawns().size();
						player.sendMessage(String.format("spawns of arena %s (%s)", mapSpawn.getName(), n));
						for(int i = 0; i < n; i++) {
							if(mapSpawn.getSpawn(i) == null) {
								player.sendMessage(String.format("%s: null", i));
								continue;
							}
							player.sendMessage(String.format("%s: %s, %s, %s", i,
									mapSpawn.getSpawn(i).getX(),
									mapSpawn.getSpawn(i).getY(),
									mapSpawn.getSpawn(i).getZ()));
						}
						return true;
					}
					int spawn = Integer.parseInt(args[3]);
					if (args[1].equalsIgnoreCase("nullcheck")) {
						Vector arenaSpawnLocation = mapSpawn.getSpawn(spawn);
						sender.sendMessage(arenaSpawnLocation == null ? "spawn is null" : "spawn exists");
					}
					else if (args[1].equalsIgnoreCase("set")) {
						// TODO: calculate location relative to arena location
						//mapSpawn.setSpawn(spawn, player.getLocation().toVector());
						player.sendMessage(String.format("Set spawn %s of arena '%s' to your current location", spawn,
								mapSpawn.getName()));
					}
					else if (args[1].equalsIgnoreCase("tp")) {
						player.teleport(arena.getVectorInArena(mapSpawn.getSpawn(spawn)));
						player.sendMessage(
								String.format("Teleported to spawn %s of arena '%s'", spawn, mapSpawn.getName()));
					}
				}
				else if (args[0].equalsIgnoreCase("arenas")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					ArrayList<Arena> arenas = Skywars.get().getArenas();
					if (arenas != null && arenas.size() > 0) {
						List<String> arenaNames = arenas.stream()
								.map(m -> m.getMap().getName())
								.collect(Collectors.toList());
						sender.sendMessage(String.format("Arenas: %s", String.join(", ", arenaNames)));
					} else
						sender.sendMessage("No arenas");
				}
				else if (args[0].equalsIgnoreCase("maps")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					ArrayList<SkywarsMap> maps = Skywars.get().getMaps();
					if (maps != null && maps.size() > 0) {
						List<String> mapNames = maps.stream()
								.map(m -> m.getName())
								.collect(Collectors.toList());
						sender.sendMessage(String.format("Maps: %s", String.join(", ", mapNames)));
					} else
						sender.sendMessage("No maps");
				}
				else if (args[0].equalsIgnoreCase("create")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (name != null) {
						if (Skywars.get().getMap(name) == null) {
							if(Skywars.get().createMap(name))
								sender.sendMessage("Map successfully created");
							else
								sender.sendMessage("Could not create map");
						} else {
							sender.sendMessage("Map already exists");
						}
					} else {
						sender.sendMessage("No name");
					}
				}
				else if (args[0].equalsIgnoreCase("delete")) {
					if(!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (name != null) {
						if (Skywars.get().getMap(name) != null) {
							if(Skywars.get().deleteMap(name))
								sender.sendMessage("Map successfully deleted");
							else
								sender.sendMessage("Could not delete map");
						} else {
							sender.sendMessage("Map doesn't exist");
						}
					} else {
						sender.sendMessage("No name");
					}
				} else {
					sender.sendMessage(Messager.color("&cInvalid arguments! &eUse &b/sw help"));
				}
				return true;
			} else {
				sender.sendMessage(Messager.color("&a&lSkyWars &e- /sw help"));
			}

		} catch (Exception e) {
			sender.sendMessage("there was an error executing the command");
			String m = e.getLocalizedMessage();
			if(m != null)
				sender.sendMessage(m);
			else
				sender.sendMessage(e.getClass().getName());
			e.printStackTrace();
		}
		return false;
	}

}