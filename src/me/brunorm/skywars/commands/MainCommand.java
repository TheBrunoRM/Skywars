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
import me.brunorm.skywars.menus.MapSetupMenu;
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
					if(CommandsUtils.permissionCheckWithMessage(player, "skywars.setmainlobby")) {	
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
						MapSetupMenu.OpenConfigurationMenu(player, map);
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
					if(CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin")) {						
						Skywars.get().Reload();
						sender.sendMessage(Messager.getMessage("RELOADED"));
					}
				}
				
				else if(!CommandsUtils.permissionCheckWithMessage(player, "skywars.test")) return false;
				// TEST COMMANDS
				
				if(args[0].equalsIgnoreCase("bigcase")) {
					Skywars.createBigCase(player.getLocation(), XMaterial.LIME_STAINED_GLASS);
				}
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
					if(map.getWorldName() != null)
						player.sendMessage(map.getWorldName());
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
					Object xd = map.getConfig().get(args[2]);
					if(xd == null) xd = "lmao it doesnt exist";
					sender.sendMessage(xd.toString());
				}
				if(args[0].equalsIgnoreCase("getconfig")) {
					Object xd = Skywars.get().getConfig().get(args[1]);
					if(xd == null) xd = "lmao it doesnt exist";
					sender.sendMessage(xd.toString());
				}
				if (args[0].equalsIgnoreCase("scoreboard")) {
					SkywarsScoreboard.update(player);
				}
				if (args[0].equalsIgnoreCase("getblock")) {
					Block block = SkywarsUtils.getTargetBlock(player, 5);
					Bukkit.broadcastMessage(String.format("block is at %s", block.getLocation()));
				}
				if (args[0].equalsIgnoreCase("fillchest")) {
					Block block = SkywarsUtils.getTargetBlock(player, 5);
					ChestManager.fillChest(block.getLocation(), false);
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
						for (SkywarsPlayer swp : arena.getAllPlayersIncludingAliveAndSpectators()) {
							if (swp == null)
								sender.sendMessage("null");
							else
								sender.sendMessage(String.format("%s", swp.getPlayer().getName()));
						}
					}
				}
				if(args[0].equalsIgnoreCase("restart")) {
					arena.restart();
				}
				if (args[0].equalsIgnoreCase("stop")) {
					if (playerArena != null) {
						playerArena.startTimer(ArenaStatus.ENDING);
					}
				}
				if (args[0].equalsIgnoreCase("forcestop")) {
					if (arena != null) {
						arena.restart();
					}
				}
				if(args[0].equalsIgnoreCase("calculatespawns")) {
					map.calculateSpawns();
				}
				if (args[0].equalsIgnoreCase("save")) {
					// plugin.saveArenas();
					// sender.sendMessage("saved");
				}
				if (args[0].equalsIgnoreCase("joined")) {
					sender.sendMessage(Skywars.get().getPlayerArena(player) != null ? "joined" : "not joined");
				}
				if (args[0].equalsIgnoreCase("case")) {
					Skywars.createCase(player.getLocation(), XMaterial.GLASS);
				}
				if (args[0].equalsIgnoreCase("spawn")) {
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
					if (args[1].equalsIgnoreCase("set")) {
						// TODO: calculate location relative to arena location
						//mapSpawn.setSpawn(spawn, player.getLocation().toVector());
						player.sendMessage(String.format("Set spawn %s of arena '%s' to your current location", spawn,
								mapSpawn.getName()));
					}
					if (args[1].equalsIgnoreCase("tp")) {
						player.teleport(arena.getVectorInArena(mapSpawn.getSpawn(spawn)));
						player.sendMessage(
								String.format("Teleported to spawn %s of arena '%s'", spawn, mapSpawn.getName()));
					}
				}
				if (args[0].equalsIgnoreCase("arenas")) {
					if(!CommandsUtils.permissionCheckWithMessage(player, "skywars.admin")) return false;
					ArrayList<Arena> arenas = Skywars.get().getArenas();
					if (arenas != null && arenas.size() > 0) {
						List<String> arenaNames = arenas.stream()
								.map(m -> m.getMap().getName())
								.collect(Collectors.toList());
						sender.sendMessage(String.format("Arenas: %s", String.join(", ", arenaNames)));
					} else
						sender.sendMessage("No arenas");
				}
				if (args[0].equalsIgnoreCase("maps")) {
					if(!CommandsUtils.permissionCheckWithMessage(player, "skywars.admin")) return false;
					ArrayList<SkywarsMap> maps = Skywars.get().getMaps();
					if (maps != null && maps.size() > 0) {
						List<String> mapNames = maps.stream()
								.map(m -> m.getName())
								.collect(Collectors.toList());
						sender.sendMessage(String.format("Maps: %s", String.join(", ", mapNames)));
					} else
						sender.sendMessage("No maps");
				}
				return true;
			} else {
				sender.sendMessage(Messager.color("&a&lSkyWars &e- /sw help"));
			}

		} catch (Exception e) {
			sender.sendMessage("there was an error executing the command:");
			//sender.sendMessage(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return false;
	}

}