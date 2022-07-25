package me.brunorm.skywars.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.ChestManager;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsScoreboard;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.menus.GamesMenu;
import me.brunorm.skywars.menus.MapMenu;
import me.brunorm.skywars.menus.SetupMenu;
import me.brunorm.skywars.schematics.Schem;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;
import me.brunorm.skywars.structures.SkywarsUser;

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

	String[] helpLines = { "&a&lSkywars commands", "&b/skywars setmainlobby &e- sets the main lobby",
			"&b/skywars lobby &e- teleports you to the main lobby", "&b/skywars create <arena> &e- creates an arena",
			"&b/skywars delete <arena> &e- deletes an arena",
			"&b/skywars config <arena> &e- opens the configuration menu", "&b/skywars play &e- open the arenas menu",
			"&b/skywars start &e- starts a game", "&b/skywars forcestart &e- starts a game immediately" };

	Schematic schematic;
	Schem schem;

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
			Player player = null;
			if (sender instanceof Player) {
				player = (Player) sender;
			}
			final boolean joined = Skywars.get().getPlayerArena(player) != null;
			if (args.length > 0) {
				String name = null;
				if (args.length > 1) {
					name = args[1];
				}
				final Arena playerArena = Skywars.get().getPlayerArena(player);
				final SkywarsMap map = Skywars.get().getMap(name);
				final Arena arena = Skywars.get().getArenaAndCreateIfNotFound(map);
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
					if (map == null) {
						player.sendMessage(Messager.getMessage("NO_MAP"));
						return true;
					}
					SetupMenu.OpenConfigurationMenu(player, map);
				} else if (args[0].equalsIgnoreCase("help")) {
					for (final String line : this.helpLines) {
						sender.sendMessage(Messager.color(line));
					}
				} else if (args[0].equalsIgnoreCase("leave")) {
					if (!CommandsUtils.consoleCheckWithMessage(sender))
						return true;
					if (CommandsUtils.arenaCheckWithMessage(player))
						playerArena.leavePlayer(player);

					// TODO add the option to pass an arena name as an argument
				} else if (args[0].equalsIgnoreCase("forcestart")) {
					if (!CommandsUtils.consoleCheckWithMessage(sender))
						return true;
					if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.forcestart"))
						return true;
					if (!CommandsUtils.arenaCheckWithMessage(player))
						return true;
					playerArena.startGame();
				} else if (args[0].equalsIgnoreCase("start")) {
					if (!CommandsUtils.consoleCheckWithMessage(sender))
						return true;
					if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.start"))
						return true;
					if (!CommandsUtils.arenaCheckWithMessage(player))
						return true;
					playerArena.softStart(player);
				} else if (args[0].equalsIgnoreCase("join")) {
					if (!CommandsUtils.consoleCheckWithMessage(sender))
						return true;
					// if(!SkywarsUtils.JoinableCheck(arena, player)) return true;
					Skywars.get().joinMap(map, player);
				} else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("about")
						|| args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("version")) {
					sender.sendMessage(Messager.colorFormat("&b%s &eversion &a%s &emade by &b%s", Skywars.get().name,
							Skywars.get().version, String.join(", ", Skywars.get().authors)));
				} else if (args[0].equalsIgnoreCase("server")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					sender.sendMessage("Server version:");
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

				else if (args[0].equalsIgnoreCase("xmat")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.getInventory().addItem(XMaterial.matchXMaterial(args[1]).get().parseItem());
				} else if (args[0].equalsIgnoreCase("hd")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					// final Hologram holo = HologramsAPI.createHologram(Skywars.get(),
					// player.getLocation());
					// holo.appendTextLine("hola que tal bro");
					// holo.appendTextLine("&6aver ese colorsito");
					// holo.appendTextLine(Messager.color("&6la prueba con el messager"));
					// holo.appendItemLine(XMaterial.IRON_SWORD.parseItem());
					final Hologram holo = DHAPI.createHologram("test", player.getLocation().add(new Vector(0, 2, 0)));
					DHAPI.addHologramLine(holo, "hola bro");
					DHAPI.addHologramLine(holo, "&6aver ese colorsito");
					DHAPI.addHologramLine(holo, Messager.color("&e&lla prueba del color"));
					DHAPI.addHologramLine(holo, XMaterial.IRON_SWORD.parseItem());
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
						sender.sendMessage("schematic not loaded");
						return true;
					}
					SchematicHandler.pasteSchematic(player.getLocation(), this.schematic);
				} else if (args[0].equalsIgnoreCase("loadschematic")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					final File file = new File(Skywars.get().getDataFolder() + "/schematics/" + args[1]);
					if (!file.exists()) {
						sender.sendMessage("file not found: " + args[1]);
						return true;
					}
					final Object loaded = SchematicHandler.loadSchematic(file);
					if (loaded instanceof Schematic) {
						System.out.println("Loading schematic!");
						this.schematic = (Schematic) loaded;
					} else if (loaded instanceof Schem) {
						System.out.println("Loading schem!");
						this.schem = (Schem) loaded;
					} else
						System.out.println("Unknown schematic type: " + loaded);
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
						player.sendMessage("not set");
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
					player.sendMessage(String.join(", ", Bukkit.getServer().getWorlds().stream()
							.map(world -> world.getName()).collect(Collectors.toList())));
				} else if (args[0].equalsIgnoreCase("where")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.sendMessage(player.getWorld().getName());
				} else if (args[0].equalsIgnoreCase("tp")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					player.teleport(arena.getLocation());
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
					System.out.println("mat data: " + data);
					final BlockState state = block.getState();
					System.out.println(" block state: " + state.getData().getData());
					state.setData(data);
					state.update();
				} else if (args[0].equalsIgnoreCase("testschem")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					arena.pasteSchematic();
					player.sendMessage("pasted");
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
				} else if (args[0].equalsIgnoreCase("restart")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					arena.clear();
				} else if (args[0].equalsIgnoreCase("stop")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (playerArena != null) {
						playerArena.startTimer(ArenaStatus.ENDING);
					}
				} else if (args[0].equalsIgnoreCase("forcestop")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (arena != null) {
						arena.clear();
					}
				} else if (args[0].equalsIgnoreCase("calculatespawns")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					map.calculateSpawns();
				} else if (args[0].equalsIgnoreCase("save")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin")) {
					}
				} else if (args[0].equalsIgnoreCase("joined")) {
					sender.sendMessage(Skywars.get().getPlayerArena(player) != null ? "joined" : "not joined");
				} else if (args[0].equalsIgnoreCase("case")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					Skywars.createCase(player.getLocation(), XMaterial.GLASS);
				} else if (args[0].equalsIgnoreCase("spawn")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					final String nameMap = args[2];
					final SkywarsMap mapSpawn = Skywars.get().getMap(nameMap);
					if (mapSpawn == null) {
						player.sendMessage(String.format("No arena found by '%s'", nameMap));
						return true;
					}
					if (args[1].equalsIgnoreCase("list")) {
						final int n = mapSpawn.getSpawns().size();
						player.sendMessage(String.format("spawns of arena %s (%s)", mapSpawn.getName(), n));
						for (int i = 0; i < n; i++) {
							if (mapSpawn.getSpawn(i) == null) {
								player.sendMessage(String.format("%s: null", i));
								continue;
							}
							player.sendMessage(String.format("%s: %s, %s, %s", i, mapSpawn.getSpawn(i).getX(),
									mapSpawn.getSpawn(i).getY(), mapSpawn.getSpawn(i).getZ()));
						}
						return true;
					}
					final int spawn = Integer.parseInt(args[3]);
					if (args[1].equalsIgnoreCase("nullcheck")) {
						final Vector arenaSpawnLocation = mapSpawn.getSpawn(spawn);
						sender.sendMessage(arenaSpawnLocation == null ? "spawn is null" : "spawn exists");
					} else if (args[1].equalsIgnoreCase("set")) {
						// TODO calculate location relative to arena location
						// mapSpawn.setSpawn(spawn, player.getLocation().toVector());
						player.sendMessage(String.format("Set spawn %s of arena '%s' to your current location", spawn,
								mapSpawn.getName()));
					} else if (args[1].equalsIgnoreCase("tp")) {
						player.teleport(arena.getVectorInArena(mapSpawn.getSpawn(spawn)));
						player.sendMessage(
								String.format("Teleported to spawn %s of arena '%s'", spawn, mapSpawn.getName()));
					}
				} else if (args[0].equalsIgnoreCase("arenas")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					final List<Arena> arenas = Skywars.get().getArenas();
					if (arenas != null && arenas.size() > 0) {
						final List<String> arenaNames = arenas.stream().map(m -> m.getMap().getName())
								.collect(Collectors.toList());
						sender.sendMessage(String.format("Arenas: %s", String.join(", ", arenaNames)));
					} else
						sender.sendMessage("No arenas");
				} else if (args[0].equalsIgnoreCase("maps")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					final ArrayList<SkywarsMap> maps = Skywars.get().getMaps();
					if (maps != null && maps.size() > 0) {
						final List<String> mapNames = maps.stream().map(m -> m.getName()).collect(Collectors.toList());
						sender.sendMessage(String.format("Maps: %s", String.join(", ", mapNames)));
					} else
						sender.sendMessage("No maps");
				} else if (args[0].equalsIgnoreCase("create")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (name != null) {
						if (Skywars.get().getMap(name) == null) {
							if (Skywars.get().createMap(name))
								sender.sendMessage("Map successfully created");
							else
								sender.sendMessage("Could not create map");
						} else {
							sender.sendMessage("Map already exists");
						}
					} else {
						sender.sendMessage("No name");
					}
				} else if (args[0].equalsIgnoreCase("delete")) {
					if (!CommandsUtils.permissionCheckWithMessage(sender, "skywars.admin"))
						return true;
					if (name != null) {
						if (Skywars.get().getMap(name) != null) {
							if (Skywars.get().deleteMap(name))
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

		} catch (final Exception e) {
			sender.sendMessage("there was an error executing the command");
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