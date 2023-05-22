package me.brunorm.skywars.structures;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.ChestManager;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.commands.CommandsUtils;
import me.brunorm.skywars.holograms.HologramController;
import me.brunorm.skywars.managers.ArenaManager;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import mrblobman.sounds.Sounds;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class Arena {

	private World world;
	private Location location;
	private ArenaStatus status;
	private int countdown;
	private boolean joinable = true;
	private boolean invencibility = false;
	private boolean forcedStart;
	private Player forcedStartPlayer;
	private SkywarsUser winner;
	private int minimumY = -1;

	private final ArenaGameSettings gameSettings = new ArenaGameSettings(this);

	private final SkywarsMap map;

	public Arena get() {
		return this;
	}

	public SkywarsMap getMap() {
		return this.map;
	}

	private final ArrayList<SkywarsTeam> teams = new ArrayList<SkywarsTeam>();
	private final ArrayList<SkywarsUser> users = new ArrayList<SkywarsUser>();
	private final ArrayList<SkywarsEvent> events = new ArrayList<SkywarsEvent>();

	// TODO move the chests field into SkywarsMap
	// and store chest locations and types (normal or center chests)
	private final ArrayList<Chest> chests = new ArrayList<Chest>();
	private final HashMap<Chest, String> chestHolograms = new HashMap<Chest, String>();

	public ArrayList<SkywarsTeam> getTeams() {
		return this.teams;
	}

	public Arena(SkywarsMap map) {
		this.map = map;
		this.status = ArenaStatus.WAITING;
		this.events.add(new SkywarsEvent(this, SkywarsEventType.REFILL, 60));
		this.events.add(new SkywarsEvent(this, SkywarsEventType.REFILL, 60));
		this.joinable = true;
	}

	public SkywarsEvent getNextEvent() {
		if (this.events.size() > 0)
			return this.events.get(0);
		return null;
	}

	public String getNextEventText() {
		if (this.status == ArenaStatus.RESTARTING)
			return Skywars.langConfig.getString("status.ended");
		final SkywarsEvent event = this.getNextEvent();
		if (event == null)
			return Messager.getMessage("events.noevent");
		final int time = event.getTime();
		final int minutes = time / 60;
		final int seconds = time % 60;
		final String timeString = String.format("%d:%02d", minutes, seconds);
		return Messager.color(Skywars.langConfig.getString("events.format")
				.replaceAll("%name%", Skywars.langConfig.getString("events." + event.getType().name().toLowerCase()))
				.replaceAll("%time%", timeString));

	}

	SkywarsTeam getNextFreeTeamOrCreateIfItDoesntExist() {
		for (final SkywarsTeam team : this.teams)
			if (team.getUsers().size() < this.getMap().teamSize)
				return team;
		final SkywarsTeam team = new SkywarsTeam(this);
		this.teams.add(team);
		return team;
	}

	public boolean joinPlayer(Player player) {
		if (SkywarsUtils.getJoinProblems(this, player) != null)
			return false;
		if (!this.checkProblems()) {
			for (final String problem : this.getProblems()) {
				player.sendMessage("problem when joining map: " + problem);
			}
			return false;
		}
		if (this.hasPlayer(player))
			return false;
		this.joinable = this.getAlivePlayerCount() < this.map.getMaxPlayers();
		final int index = this.getNextAvailablePlayerSlot();
		final Location spawn = this.getVectorInArena(this.getSpawn(index));
		if (spawn == null) {
			player.sendMessage(String.format("spawn %s of arena %s not set", index, this.map.getName()));
			return false;
		}
		final SkywarsTeam team = this.getNextFreeTeamOrCreateIfItDoesntExist();
		final SkywarsUser swPlayer = new SkywarsUser(player, team, index);
		this.users.add(swPlayer);
		player.sendMessage("[DEBUG] You joined team " + team.getNumber());
		for (final SkywarsUser players : this.getUsers()) {
			players.getPlayer().sendMessage(Messager.getMessage("JOIN", player.getName(), this.getAlivePlayerCount(),
					this.map.getMaxPlayers()));
			SkywarsUtils.playSoundsFromConfig(player.getPlayer(), "sounds.join");
		}

		if (this.getTask() != null && this.getStatus() == ArenaStatus.STARTING)
			player.sendMessage(Messager.getMessage("GAME_STARTING", this.getCountdown()));

		Skywars.createCase(spawn, Skywars.get().getPlayerCaseXMaterial(player));

		Skywars.get().playerLocations.put(player, player.getLocation());
		swPlayer.setSavedPlayer(new SavedPlayer(player));
		player.teleport(SkywarsUtils.getCenteredLocation(spawn));
		SkywarsUtils.clearPlayer(player, true);

		SkywarsUtils.setPlayerInventory(player, "waiting");

		Skywars.get().NMS().sendTitle(player, Skywars.langConfig.getString("arena_join.title"),
				Skywars.langConfig.getString("arena_join.subtitle"));

		if (this.getStatus() != ArenaStatus.STARTING && this.getUsers().size() >= Skywars.config.getInt("minPlayers")) {
			this.startTimerAndSetStatus(ArenaStatus.STARTING);
		}

		return true;
	}

	private Vector getSpawn(Object key) {
		return this.map.spawns.get(key);
	}

	public void makeSpectator(SkywarsUser p) {
		this.makeSpectator(p, null, null);
	}

	public void makeSpectator(SkywarsUser p, Player killer) {
		this.makeSpectator(p, killer, null);
	}

	public void makeSpectator(SkywarsUser p, DamageCause cause) {
		this.makeSpectator(p, null, cause);
	}

	public void makeSpectator(SkywarsUser p, Player killer, DamageCause cause) {
		if (!this.started())
			return;

		if (p.isSpectator())
			return;

		p.setSpectator(true);
		final Player player = p.getPlayer();

		if (killer != null) {
			final SkywarsUser killerPlayer = this.getUser(killer);
			if (killerPlayer != null) {
				Skywars.get().incrementPlayerTotalKills(killer);
				final double killMoney = Skywars.get().getConfig().getDouble("economy.kill");
				if (Skywars.get().getEconomy() != null && killMoney > 0) {
					Skywars.get().getEconomy().depositPlayer(killer, killMoney);
					killer.sendMessage(Messager.colorFormat("&6+$%s", SkywarsUtils.formatDouble(killMoney)));
				}
				Skywars.get().incrementPlayerSouls(killer);
				killer.sendMessage(Messager.colorFormat("&b+%s Soul", 1));
			}
		}
		Skywars.get().incrementPlayerTotalDeaths(player);

		// only drop items if the player is inside the arena
		// if the player dies in the void, it will not drop items
		if (this.isInBoundaries(player)) {
			// drop player inventory
			for (final ItemStack i : player.getInventory().getContents()) {
				if (i != null) {
					player.getWorld().dropItemNaturally(player.getLocation(), i);
					player.getInventory().remove(i);
				}
			}
			// drop player armor
			for (final ItemStack i : player.getInventory().getArmorContents()) {
				if (i != null && i.getType() != Material.AIR) {
					player.getWorld().dropItemNaturally(player.getLocation(), i);
				}
			}
		}

		if (this.getStatus() == ArenaStatus.PLAYING)
			// send death message to players
			for (final SkywarsUser players : this.getUsers()) {
				// TODO: add more death messages
				players.getPlayer().sendMessage(this.getDeathMessage(p, killer, cause));
			}

		this.spectator(player, this.getVectorInArena(this.getSpawn(p.teamNumber)));

		this.removePlayer(p);

		if (this.getWinner() != p)
			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					Skywars.get().NMS().sendTitle(player, Messager.getMessage("died.title"),
							Messager.getMessage("died.subtitle"), 0, 80, 0);
				}
			}, 20);
	}

	private void spectator(Player player, Location location) {
		// TODO: make customizable spectator mode

		SkywarsUtils.clearPlayer(player);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setGameMode(GameMode.ADVENTURE);

		for (final Player players : Bukkit.getOnlinePlayers()) {
			players.hidePlayer(player);
		}

		// give spectator items (player tracker, spectator settings, leave item)
		SkywarsUtils.setPlayerInventory(player, "spectator");

		player.teleport(SkywarsUtils.getCenteredLocation(location));
		player.setVelocity(new Vector(0, 1f, 0));
	}

	private String getDeathMessage(SkywarsUser p, Player killer, DamageCause cause) {
		final Player player = p.getPlayer();
		if (killer != null)
			return Messager.colorFormat("&c%s &ekilled &c%s", killer.getName(), player.getName());
		else if (p.getLastHit() != null)
			return Messager.colorFormat("&c%s &edied while trying to escape &c%s", player.getName(),
					p.getLastHit().getName());
		else if (cause == DamageCause.VOID)
			return Messager.colorFormat("&c%s &efell in the void.", player.getName());
		else
			return Messager.colorFormat("&c%s &edied.", player.getName());
	}

	public void leavePlayer(Player player) {
		this.leavePlayer(this.getUser(player));
	}

	public void leavePlayer(SkywarsUser player) {
		if (player == null)
			return;
		player.getPlayer().sendMessage(
				Messager.getFormattedMessage("LEAVE_SELF", player.getPlayer(), this, player, this.map.getName()));
		if (!this.started())
			this.joinable = true;
		if (player.getTeam().getUsers().size() <= 1)
			player.getTeam().disband();
		this.users.remove(player);
		this.removePlayer(player);
		if (this.getStatus() != ArenaStatus.RESTARTING && !player.isSpectator()) {
			for (final SkywarsUser players : this.getUsers()) {
				players.getPlayer().sendMessage(Messager.getFormattedMessage("LEAVE", player.getPlayer(), this, player,
						player.getPlayer().getName(), this.getUsers().size(), this.map.getMaxPlayers()));
				final String sound = Skywars.config.getString("sounds.leave");
				final String[] splitted = sound.split(";");
				players.getPlayer().playSound(players.getPlayer().getLocation(),
						Sounds.valueOf(splitted[0]).bukkitSound(),
						splitted.length > 1 ? Float.parseFloat(splitted[1]) : 1,
						splitted.length > 2 ? Float.parseFloat(splitted[2]) : 1);
			}
		}
		// restore player
		this.exitPlayer(player);

		final int minPlayers = Skywars.config.getInt("minPlayers");
		if (this.getStatus() == ArenaStatus.STARTING && !this.forcedStart
				&& (minPlayers <= 0 || this.getAlivePlayerCount() < minPlayers)) {
			// Skywars.get().sendDebugMessage("stopping start cooldown");
			this.setStatus(ArenaStatus.WAITING);
			for (final SkywarsUser players : this.getUsers()) {
				players.getPlayer().sendMessage(Messager.getMessage("COUNTDOWN_STOPPED", this.getAlivePlayerCount()));
			}
			this.cancelTimer();
		}
		if (this.status != ArenaStatus.WAITING && this.getAlivePlayerCount() <= 0)
			this.clear();

	}

	public void exitPlayer(Player player) {
		this.exitPlayer(this.getUser(player));
	}

	public void exitPlayer(SkywarsUser player) {
		if (player == null)
			return;

		if (this.isInBoundaries(player.getPlayer()))
			SkywarsUtils.teleportPlayerBackToTheLobbyOrToTheirLastLocationIfTheLobbyIsNotSet(player.getPlayer());

		SkywarsUtils.clearPlayer(player.getPlayer());
		player.getSavedPlayer().Restore();
	}

	public void removePlayer(SkywarsUser player) {
		if (this.getStatus() != ArenaStatus.PLAYING)
			return;

		if (this.getAlivePlayers().size() <= 1) {
			this.endGame();
			return;
		}

		for (final SkywarsUser p : this.getUsers()) {
			Skywars.get().NMS().sendActionbar(p.getPlayer(),
					Messager.getMessage("PLAYERS_REMAINING", this.getAlivePlayerCount()));
		}
	}

	public boolean endGame() {
		if (this.getStatus() == ArenaStatus.RESTARTING)
			return false;

		// TODO make a proper winner list
		final List<SkywarsUser> winners = new ArrayList<>(this.getAlivePlayers());
		if (winners.size() > 0) {
			this.setWinner(winners.get(0));
		}

		final double winMoney = Skywars.get().getConfig().getDouble("economy.win");

		if (this.getWinner() != null) {
			Skywars.get().incrementPlayerTotalWins(this.getWinner().getPlayer());
			if (winMoney > 0 && Skywars.get().getEconomy() != null) {
				Skywars.get().getEconomy().depositPlayer(this.getWinner().getPlayer(), winMoney);
				this.getWinner().getPlayer()
						.sendMessage(Messager.colorFormat("&6+$%s", SkywarsUtils.formatDouble(winMoney)));
			}
		}
		for (final SkywarsUser p : this.getUsers()) {
			if (p == this.getWinner()) {
				Skywars.get().NMS().sendTitle(p.getPlayer(), Messager.getMessage("won.title"),
						Messager.getMessage("won.subtitle"), 0, 80, 0);
			} else {
				Skywars.get().NMS().sendTitle(p.getPlayer(), "&c&lGAME ENDED", "&7You didn't win this time.", 0, 80, 0);
			}
			if (this.winner == null) {
				p.getPlayer().sendMessage(Messager.color("&cnobody &ewon"));
			} else {
				p.getPlayer().sendMessage(Messager.colorFormat("&c%s &ewon!", this.winner.getPlayer().getName()));
			}
		}
		this.removeHolograms();
		return this.startTimerAndSetStatus(ArenaStatus.RESTARTING);
	}

	BukkitTask task;

	public BukkitTask getTask() {
		return this.task;
	}

	void cancelTimer() {
		if (this.getTask() == null)
			return;
		this.getTask().cancel();
		this.task = null;
	}

	public boolean startTimerAndSetStatus(ArenaStatus status) {
		Skywars.get().sendDebugMessage("startTimer for %s: %s", this.getMap().getName(), status);
		if (this.getStatus() == status)
			return false;
		if (this.started() && status == ArenaStatus.STARTING)
			return false;
		if (!this.started() && status == ArenaStatus.RESTARTING)
			return false;
		this.cancelTimer();
		this.setStatus(status);
		Skywars.get().sendDebugMessage("starting %s timer", status);
		if (status == ArenaStatus.STARTING) {
			if (this.forcedStart && this.forcedStartPlayer != null) {
				for (final SkywarsUser player : this.getUsers()) {
					player.getPlayer()
							.sendMessage(Messager.getMessage("FORCED_START", this.forcedStartPlayer.getName()));
				}
			}
			this.task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				int time = Skywars.get().getConfig().getInt("time.starting") + 1;

				@Override
				public void run() {
					this.time--;
					Arena.this.countdown = this.time;

					if (this.time == 0) {
						Arena.this.startGame();
						return;
					}

					for (final SkywarsUser player : Arena.this.getUsers()) {

						final String sound = Skywars.config.getString("sounds.countdown");
						final String[] splitted = sound.split(";");
						player.getPlayer().playSound(player.getPlayer().getLocation(),
								Sounds.valueOf(splitted[0]).bukkitSound(),
								splitted.length > 1 ? Float.parseFloat(splitted[1]) : 1,
								splitted.length > 2 ? Float.parseFloat(splitted[2]) : 1);

						for (final Object object : Skywars.langConfig.getList("countdown")) {
							@SuppressWarnings("unchecked")
							final HashMap<Object, Object> hash = (HashMap<Object, Object>) object;
							final Object range = hash.get("range");
							boolean yes = false;
							if (range.getClass().equals(Integer.class) && this.time == (Integer) range) {
								yes = true;
							} else if (range.getClass().equals(String.class)) {
								final String[] nums = ((String) range).split("-");
								int first = Integer.parseInt(nums[0]);
								int last = Integer.parseInt(nums[1]);
								// put them from lower to higher
								if (first > last) {
									final int temp = first;
									first = last;
									last = temp;
								}
								// check time in range
								if (this.time >= first && this.time <= last) {
									yes = true;
								}
							}
							if (yes) {
								final String title = (String) hash.get("title");
								final String subtitle = (String) hash.get("subtitle");
								final String color = String.valueOf(hash.get("color"));
								Skywars.get().NMS().sendTitle(player.getPlayer(),
										SkywarsUtils.format(title, player.getPlayer(), Arena.this.get(), player),
										SkywarsUtils.format(subtitle, player.getPlayer(), Arena.this.get(), player), 0,
										50, 0);
								final String msg = Skywars.langConfig
										.getString(this.time == 1 ? "GAME_STARTING_SECOND" : "GAME_STARTING_SECONDS");
								player.getPlayer()
										.sendMessage(Messager.color(SkywarsUtils.format(
												msg.replaceAll("%count%", "&" + color + "%count%")
														.replaceAll("%seconds%", "&" + color + "%seconds%"),
												player.getPlayer(), Arena.this.get(), player)));
							}
						}
					}
				}
			}, 0L, 20L);
		} else if (status == ArenaStatus.PLAYING) {
			this.task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				@Override
				public void run() {

					final SkywarsEvent event = Arena.this.getNextEvent();

					if (event != null) {
						event.decreaseTime();
						if (event.getTime() <= 0) {
							Arena.this.events.remove(event);
							event.run();
						}
					}
				}
			}, 0L, 20L);
		} else if (this.status == ArenaStatus.RESTARTING) {
			this.task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				int time = Skywars.get().getConfig().getInt("time.ending") + 1;
				int fireworks = 0;

				@Override
				public void run() {
					this.time--;
					Arena.this.countdown = this.time;

					final SkywarsUser swp = Arena.this.getWinner();
					if (this.fireworks <= Skywars.get().getConfig().getInt("endFireworks") && swp != null
							&& Arena.this.getUser(swp.getPlayer()) != null && !swp.isSpectator()) {
						final Player p = swp.getPlayer();
						SkywarsUtils.spawnRandomFirework(p.getLocation());
						this.fireworks++;
					}

					if (this.time == 0) {
						Arena.this.clear();
					}
				}
			}, 0L, 20L);
		}
		return true;

	}

	public boolean startGame() {
		return this.startGame(null);
	}

	public boolean startGame(Player playerStarted) {
		if (playerStarted != null) {
			if (!CommandsUtils.permissionCheckWithMessage(playerStarted, "skywars.forcestart"))
				return false;
			if (!CommandsUtils.arenaCheckWithMessage(playerStarted))
				return false;
		}
		if (this.started())
			return false;
		this.cancelTimer();
		this.startTimerAndSetStatus(ArenaStatus.PLAYING);
		this.calculateAndFillChests();
		this.applyGameSettings();
		for (final Vector spawn : this.map.getSpawns().values()) {
			Skywars.createCase(this.getVectorInArena(spawn), XMaterial.AIR);
		}
		for (final SkywarsUser player : this.getAlivePlayers()) {
			SkywarsUtils.clearPlayer(player.getPlayer());
			player.getPlayer().setGameMode(GameMode.SURVIVAL);
			final Kit kit = Skywars.get().getPlayerKit(player.getPlayer());
			if (kit != null) {
				for (final ItemStack item : kit.getItems()) {
					player.getPlayer().getInventory().addItem(item);
				}
			}
			player.getPlayer().sendMessage(Messager.getMessage("arena_start.message"));
			Skywars.get().NMS().sendTitle(player.getPlayer(), Messager.getMessage("arena_start.title"),
					Messager.getMessage("arena_start.subtitle"));
			SkywarsUtils.playSoundsFromConfig(player.getPlayer(), "sounds.start");

			this.setInvencibility(true);

			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					if (!Arena.this.started() || Arena.this.getUser(player.getPlayer()) == null || player.isSpectator())
						return;
					for (final String l : Skywars.langConfig.getStringList("startLines")) {
						player.getPlayer().sendMessage(Messager.color(l));
					}
				}
			}, 20);

			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					if (!Arena.this.started() || Arena.this.getUser(player.getPlayer()) == null || player.isSpectator())
						return;
					final double playMoney = Skywars.get().getConfig().getDouble("economy.play");
					if (Skywars.get().getEconomy() != null && playMoney > 0) {
						Skywars.get().getEconomy().depositPlayer(player.getPlayer(), playMoney);
						player.getPlayer()
								.sendMessage(Messager.colorFormat("&6+$%s", SkywarsUtils.formatDouble(playMoney)));
					}
				}
			}, 20 * 10);
		}

		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {

			@Override
			public void run() {
				Arena.this.setInvencibility(false);
			}

		}, 40);
		return true;
	}

	private void applyGameSettings() {

		final TimeType time = SkywarsUtils.mostFrequentElement(this.timeVotes.values());
		if (time != null)
			switch (time) {
			case NIGHT:
				this.gameSettings.time = 14000;
				break;
			default:
				this.gameSettings.time = 0;
			}

		final WeatherType weather = SkywarsUtils.mostFrequentElement(this.weatherVotes.values());
		if (weather != null)
			switch (weather) {
			case RAIN:
				this.gameSettings.weather = org.bukkit.WeatherType.DOWNFALL;
				break;
			default:
				this.gameSettings.weather = org.bukkit.WeatherType.CLEAR;
			}

		for (final SkywarsUser user : this.getUsers()) {
			user.getPlayer().setPlayerTime(this.gameSettings.time, true);
			user.getPlayer().setPlayerWeather(this.gameSettings.weather);
		}

		// TODO apply chest type
	}

	public void clear() {
		this.clear(true);
	}

	public void clear(boolean remove) {
		this.cancelTimer();
		Skywars.get().sendDebugMessage("Clearing arena for map " + this.map.getName());
		for (final SkywarsUser player : this.getUsers()) {
			this.exitPlayer(player);
		}
		this.users.clear();

		final World world = this.getWorldAndLoadIfItIsNotLoaded();
		if (this.getMap().getSchematic() != null || this.getMap().schematicFilename != null) {
			// remove entities only (and paste schematic later)
			for (final Entity i : world.getEntities()) {
				if (i instanceof Item && this.isInBoundaries(i.getLocation())) {
					i.remove();
				}
			}
		} else {
			// restart the whole world
			this.unloadWorld(world);
		}

		this.forcedStart = false;
		this.forcedStartPlayer = null;
		this.setStatus(ArenaStatus.WAITING);
		this.countdown = -1;

		if (remove)
			ArenaManager.removeArenaFromListAndDeleteArena(this);
	}

	private boolean unloadWorld(World world) {
		Skywars.get().sendDebugMessage("Restarting world '%s' for map '%s'", world.getName(), this.getMap().getName());
		boolean unloaded = false;
		int tries = 0;
		while (!unloaded) {
			if (tries >= 5) {
				break;
			}
			Skywars.get().sendDebugMessage("Trying to restart world: %s (tries: %s)", world.getName(), tries);
			for (final Player p : world.getPlayers()) {
				Skywars.get().sendDebugMessage("Teleporting player %s to another world", p.getName());
				p.teleport(Bukkit.getWorlds().stream().filter(w -> w.getName() != world.getName()).findFirst().get()
						.getSpawnLocation());
			}
			unloaded = Bukkit.unloadWorld(world, false);
			tries++;
		}
		if (!unloaded) {
			Skywars.get().sendMessage("Could not unload world '%s' for map '%s'", world.getName(),
					this.getMap().getName());
		} else {
			Skywars.get().sendDebugMessage("Successfully unloaded world '%s' for map '%s'", world.getName(),
					this.getMap().getName());
			try {
				FileUtils.deleteDirectory(world.getWorldFolder());
				Skywars.get().sendDebugMessage("Sucessfully deleted world '%s' for map '%s'", world.getName(),
						this.getMap().getName());
			} catch (final Exception e) {
				e.printStackTrace();
				Skywars.get().sendMessage("Could not delete world '%s' for map '%s'", world.getName(),
						this.getMap().getName());
			}
		}
		return unloaded;
	}

	void removeHolograms() {
		for (final String holoName : this.chestHolograms.values()) {
			Skywars.get().getHologramController().removeHologram(holoName);
		}
		this.chestHolograms.clear();
	}

	public void resetCases() {
		for (final Vector spawn : this.map.getSpawns().values()) {
			Skywars.createCase(this.getVectorInArena(spawn), XMaterial.RED_STAINED_GLASS);
		}
	}

	public ArrayList<String> getProblems() {
		final ArrayList<String> problems = new ArrayList<String>();
		if (this.getWorldAndLoadIfItIsNotLoaded() == null)
			problems.add("World not found");
		if (this.getSpawn(this.getAlivePlayerCount()) == null)
			problems.add(String.format("Spawn %s not set", this.getAlivePlayerCount()));
		if (this.map.getMaxPlayers() <= 0)
			problems.add("Max players not set");
		if (this.map.getWorldName() == null) {
			if (this.map.schematicFilename == null)
				problems.add("Schematic not set");
			if (this.location == null)
				problems.add("No location set");
		}
		if (!this.isJoinable())
			problems.add("Arena is not joinable");
		return problems;
	}

	boolean checkProblems() {
		return this.getProblems().size() <= 0;
	}

	public Location getVectorInArena(Vector vector) {
		final World world = this.getWorldAndLoadIfItIsNotLoaded();
		if (this.location == null)
			this.location = new Location(world, 0, 0, 0);
		return new Location(world, vector.getBlockX() + this.location.getBlockX(),
				vector.getBlockY() + this.location.getBlockY(), vector.getBlockZ() + this.location.getBlockZ());
	}

	public Location getLocationInArena(Location loc) {
		return new Location(loc.getWorld(), loc.getBlockX() + this.location.getBlockX(),
				loc.getBlockY() + this.location.getBlockY(), loc.getBlockZ() + this.location.getBlockZ());
	}

	public void goBackToCenter(Player player) {
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setVelocity(new Vector(0, 0, 0));
		player.teleport(this.getLocation());
		player.setVelocity(new Vector(0, 5f, 0));
	}

	public boolean isInBoundaries(Player player) {
		return this.isInBoundaries(player.getLocation());
	}

	public boolean isInBoundaries(Location loc) {
		final Schematic schematic = this.map.getSchematic();
		if (loc.getWorld() != this.getWorldAndLoadIfItIsNotLoaded())
			return false;
		if (schematic == null || this.getLocation() == null)
			return loc.getY() > this.getMinimumY();
		return loc.getX() > this.getLocation().getX() - schematic.getWidth() / 2
				&& loc.getX() < this.getLocation().getX() + schematic.getWidth() / 2
				&& loc.getY() > this.getLocation().getY() - schematic.getHeight() / 2
				&& loc.getY() < this.getLocation().getY() + schematic.getHeight() / 2
				&& loc.getZ() > this.getLocation().getZ() - schematic.getLength() / 2
				&& loc.getZ() < this.getLocation().getZ() + schematic.getLength() / 2;
	}

	private int getMinimumY() {
		if (this.minimumY >= 0)
			return this.minimumY;

		final long start = Instant.now().toEpochMilli();

		int min = 0;
		for (final Chunk chunk : this.getAllChunksInMap()) {
			for (int y = min; y < 256; y++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						if (chunk.getBlock(x, y, z).getType() != Material.AIR) {
							min = y - 1;
							break;
						}
					}
				}
			}
		}

		Skywars.get().sendDebugMessage("Calculated minimum Y for " + this.map.getName() + " in %sms",
				Instant.now().toEpochMilli() - start);
		this.minimumY = min;
		return this.minimumY;
	}

	private int getNextAvailablePlayerSlot() {
		for (int i = 0; i < this.map.getMaxPlayers(); i++) {
			if (this.getUser(i) == null)
				return i;
		}
		return -1;
	}

	public int getAlivePlayerCount() {
		return this.users.stream().filter(player -> !player.isSpectator()).collect(Collectors.toList()).size();
	}

	public ArrayList<SkywarsUser> getUsers() {
		return this.users;
	}

	public ArrayList<SkywarsUser> getAlivePlayers() {
		return this.users.stream().filter(player -> !player.isSpectator())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	ArrayList<SkywarsUser> getSpectators() {
		return this.users.stream().filter(player -> player.isSpectator())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public boolean hasPlayer(Player player) {
		for (int i = 0; i < this.users.size(); i++) {
			if (this.users.get(i).getPlayer().getName().equals(player.getName())) {
				return true;
			}
		}
		return false;
	}

	SkywarsUser getUser(int index) {
		for (final SkywarsUser swp : this.getUsers()) {
			if (swp.getNumber() == index)
				return swp;
		}
		return null;
	}

	public SkywarsUser getUser(String name) {
		for (final SkywarsUser swp : this.getUsers()) {
			if (swp.getPlayer().getName().equals(name))
				return swp;
		}
		return null;
	}

	public SkywarsUser getUser(Player player) {
		for (final SkywarsUser swp : this.getUsers()) {
			if (swp.getPlayer().getName().equals(player.getName()))
				return swp;
		}
		return null;
	}

	public void pasteSchematic() {
		if (this.location == null || this.map.getSchematic() == null)
			return;
		Skywars.get().sendDebugMessage("pasting schematic at " + this.location.toString());
		try {
			SchematicHandler.pasteSchematic(this.location, this.map.getSchematic());
		} catch (final Exception e) {
			Skywars.get().sendDebugMessage("could not paste schematic");
			e.printStackTrace();
		}
	}

	public void clearBlocks() {
		SchematicHandler.clear(this.location, this.map.getSchematic());
	}

	public void calculateChests() {
		Skywars.get().sendDebugMessage("calculating chests for arena: " + this.getMap().getName());
		this.chests.clear();

		if (this.getMap().getSchematicFilename() != null) {
			// calculate chests from schematic

			final Schematic schematic = this.map.getSchematic();
			if (schematic == null)
				return;
			final World world = this.location.getWorld();
			final Vector offset = schematic.getOffset();
			final ListTag<CompoundTag> tileEntities = schematic.getBlockEntities();
			// Skywars.get().sendDebugMessage("tile entities: " +
			// tileEntities.getValue().size());

			final byte[] blocks = schematic.getBlocks();
			final int length = schematic.getLength();
			final int width = schematic.getWidth();
			final int height = schematic.getHeight();

			for (int x = 0; x < width; ++x) {
				for (int y = 0; y < height; ++y) {
					for (int z = 0; z < length; ++z) {
						final int index = y * width * length + z * width + x;
						final Location loc = new Location(world, x, y, z).add(offset).add(this.location);
						final Block block = loc.getBlock();
						if (block.getState() instanceof Chest && block.getType() == XMaterial.CHEST.parseMaterial()
								&& blocks[index] == 54 /* chest id */) {
							final Chest chest = (Chest) block.getState();
							if (this.chests.contains(chest))
								continue;
							this.chests.add(chest);
						}
					}
				}
			}

			for (final CompoundTag tag : tileEntities) {
				String id = tag.getString("id");
				if (id == null)
					id = tag.getString("Id");
				if (id.equalsIgnoreCase("chest")) {
					final Vector v = SchematicHandler.getVector(tag);
					final Location loc = new Location(world, v.getX(), v.getY(), v.getZ()).add(offset)
							.add(this.location);
					if (!(loc.getBlock() instanceof Chest))
						continue;
					final Chest chest = (Chest) loc.getBlock().getState();
					if (this.chests.contains(chest))
						continue;
					this.chests.add(chest);
				}
			}
		} else {
			// calculate chests from world

			for (final BlockState state : this.getAllBlockStatesInMap(Material.CHEST)) {
				this.chests.add((Chest) state);
				Skywars.get().sendDebugMessage("Added chest for map %s at location: %s", this.getMap().getName(),
						state.getLocation());
			}
		}

		Skywars.get().sendDebugMessage("Calculated %s chests!", this.chests.size());
	}

	public ArrayList<Chunk> getAllChunksInMap() {
		final ArrayList<Chunk> list = new ArrayList<Chunk>();

		final World world = this.getWorldAndLoadIfItIsNotLoaded();
		if (world == null) {
			Skywars.get().sendMessage("Could not get world for map: ", this.getMap().getName());
			return list;
		}

		final int maxMapSizeInBlocks = Skywars.config.getInt("maxMapSize");
		final int minBlock = -maxMapSizeInBlocks / 2;
		final int maxBlock = maxMapSizeInBlocks / 2;

		final Chunk minChunk = world.getChunkAt(new Location(world, minBlock, 0, minBlock));
		final Chunk maxChunk = world.getChunkAt(new Location(world, maxBlock, 0, maxBlock));

		for (int x = minChunk.getX(); x < maxChunk.getX(); x++) {
			for (int z = minChunk.getZ(); z < maxChunk.getZ(); z++) {
				final Chunk chunk = world.getChunkAt(x, z);
				list.add(chunk);
			}
		}

		return list;
	}

	public ArrayList<BlockState> getAllBlockStatesInMap(Material mat) {
		final ArrayList<BlockState> list = new ArrayList<BlockState>();

		final World world = this.getWorldAndLoadIfItIsNotLoaded();
		if (world == null) {
			Skywars.get().sendMessage("Could not get world for map: ", this.getMap().getName());
			return list;
		}

		final int maxMapSizeInBlocks = Skywars.config.getInt("maxMapSize");
		final int minBlock = -maxMapSizeInBlocks / 2;
		final int maxBlock = maxMapSizeInBlocks / 2;

		final Chunk minChunk = world.getChunkAt(new Location(world, minBlock, 0, minBlock));
		final Chunk maxChunk = world.getChunkAt(new Location(world, maxBlock, 0, maxBlock));

		for (int x = minChunk.getX(); x < maxChunk.getX(); x++) {
			for (int z = minChunk.getZ(); z < maxChunk.getZ(); z++) {
				final Chunk chunk = world.getChunkAt(x, z);
				for (final BlockState state : chunk.getTileEntities()) {
					if (state.getType() != mat)
						continue;
					list.add(state);
				}
			}
		}

		return list;
	}

	public void fillChests() {
		for (final Chest chest : this.chests) {
			final Block block = chest.getBlock();
			if (!(block.getState() instanceof Chest))
				continue;
			final Location loc = block.getLocation();
			ChestManager.fillChest(loc,
					SkywarsUtils.distance(this.getLocation().toVector(), loc.toVector()) < this.map.getCenterRadius());
		}
	}

	public void calculateAndFillChests() {
		this.calculateChests();
		this.fillChests();
	}

	public boolean softStart(Player player) {
		if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.start"))
			return false;
		if (!CommandsUtils.arenaCheckWithMessage(player))
			return false;
		if (this.getStatus() == ArenaStatus.WAITING && this.getTask() == null) {
			this.forcedStart = true;
			this.forcedStartPlayer = player;
			this.startTimerAndSetStatus(ArenaStatus.STARTING);
			player.sendMessage(Messager.get("started_countdown"));
			return true;
		}
		final boolean started = this.startGame(player);
		if (started)
			player.sendMessage(Messager.get("started_game"));
		else
			player.sendMessage(Messager.get("already_started"));
		return started;
	}

	public void broadcastRefillMessage() {
		for (final SkywarsUser player : this.getUsers()) {
			player.getPlayer().sendMessage(Messager.getMessage("refill.message"));
			Skywars.get().NMS().sendTitle(player.getPlayer(), Messager.getMessage("refill.title"),
					Messager.getMessage("refill.subtitle"));
		}
	}

	public boolean started() {
		return !(this.getStatus() == ArenaStatus.WAITING || this.getStatus() == ArenaStatus.STARTING);
	}

	public int getCountdown() {
		return this.countdown;
	}

	public ArenaStatus getStatus() {
		return this.status;
	}

	public void setStatus(ArenaStatus status) {
		this.status = status;
	}

	public World getWorldAndLoadIfItIsNotLoaded() {
		if (this.world != null)
			return this.world;

		final String worldName = this.getMap().getWorldName();
		if (worldName == null) {
			Skywars.get().sendDebugMessage("worldname is null: " + this.getMap().getName());
			final Location loc = this.getLocation();
			if (this.location == null)
				return null;
			return loc.getWorld();
		}

		final World w = Bukkit.getWorld(worldName);
		if (w != null) {
			this.world = w;
			return w;
		}

		Skywars.get().sendDebugMessage("world is not loaded");
		Skywars.get().sendDebugMessage("loading world by copying it from the worlds folder");

		final File worldFolder = new File(Skywars.worldsPath, worldName);
		if (!worldFolder.isDirectory()) {
			Skywars.get().sendDebugMessage("could not find the world in the worlds folder");
			return null;
		}

		Skywars.get().sendDebugMessage("Loading world for map: " + this.getMap().getName());
		final File bukkitWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
		try {
			if (bukkitWorldFolder.isDirectory()) {
				Skywars.get().sendDebugMessage("deleting old world folder: %s", bukkitWorldFolder.getAbsolutePath());
				bukkitWorldFolder.delete();
			}
			if (!bukkitWorldFolder.canWrite()) {
				Skywars.get().sendDebugMessage("cant write to world folder: %s", bukkitWorldFolder.getAbsolutePath());
			}
			bukkitWorldFolder.mkdirs();
			Skywars.get().sendDebugMessage("copying: %s", worldFolder.getAbsolutePath());
			Skywars.get().sendDebugMessage("to location: %s", bukkitWorldFolder.getAbsolutePath());
			FileUtils.copyDirectory(worldFolder, bukkitWorldFolder);
		} catch (final Exception e) {
			e.printStackTrace();
			Skywars.get().sendMessage("Could not create world for map: ", this.map.getName());
			return null;
		}
		if (!bukkitWorldFolder.isDirectory()) {
			Skywars.get().sendMessage("Could not load world for map: ", this.map.getName());
			return null;
		}
		final World world = Bukkit.createWorld(new WorldCreator(worldName));
		Skywars.get().setupWorld(world);
		return world;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public boolean isJoinable() {
		return this.joinable;
	}

	public boolean isInvencibility() {
		return this.invencibility;
	}

	public void setInvencibility(boolean invencibility) {
		this.invencibility = invencibility;
	}

	public SkywarsUser getWinner() {
		return this.winner;
	}

	public void setWinner(SkywarsUser winner) {
		this.winner = winner;
	}

	public ArrayList<Chest> getChests() {
		return this.chests;
	}

	public boolean removeChest(Chest chest) {
		if (!Skywars.holograms)
			return true;
		if (this.chestHolograms.containsKey(chest))
			Skywars.get().getHologramController().removeHologram(this.chestHolograms.remove(chest));
		return this.chests.remove(chest);
	}

	public boolean addChestHologram(Chest chest) {
		if (!Skywars.holograms)
			return true;
		if (this.chestHolograms.containsKey(chest))
			return false;
		final Block block = chest.getBlock();
		final String name = Skywars.get().getHologramController().createHologram(
				"Skywars_chest_" + block.getLocation().getX() + "_" + block.getLocation().getY() + "_"
						+ block.getLocation().getZ() + "_" + Instant.now().toEpochMilli(),
				block.getLocation().add(new Vector(0.5, 2, 0.5)), "");
		this.chestHolograms.put(chest, name);
		return true;
	}

	public void displayChestHolograms(String text) {
		if (!Skywars.holograms)
			return;

		final HologramController controller = Skywars.get().getHologramController();
		for (final Entry<Chest, String> h : this.chestHolograms.entrySet()) {
			final Chest chest = h.getKey();
			final int contents = Arrays.asList(chest.getInventory().getContents()).stream()
					.filter(i -> i != null && i.getType() != XMaterial.AIR.parseMaterial()).collect(Collectors.toList())
					.size();
			controller.changeHologram(h.getValue(), Messager.color(text), 0);
			controller.changeHologram(h.getValue(), contents <= 0 ? Messager.color("&cEmpty") : null, 1);
		}

	}

	public ArenaGameSettings getGameSettings() {
		return this.gameSettings;
	}

	@Deprecated
	public void changeGameSettings(Object obj) {
		this.gameSettings.change(obj);
		this.broadcastMessage("Changed game settings: " + obj.toString());
	}

	public void broadcastMessage(String string) {
		for (final SkywarsUser user : this.getUsers())
			user.getPlayer().sendMessage(string);
	}

	HashMap<UUID, TimeType> timeVotes = new HashMap<UUID, TimeType>();
	HashMap<UUID, WeatherType> weatherVotes = new HashMap<UUID, WeatherType>();
	HashMap<UUID, ChestType> chestVotes = new HashMap<UUID, ChestType>();

	public void voteTime(Player player, TimeType time) {
		if (this.timeVotes.get(player.getUniqueId()) == time)
			return;
		this.timeVotes.put(player.getUniqueId(), time);
		this.broadcastMessage(Messager.get("vote", player.getName(), Messager.get("time." + time)));
	}

	public void voteWeather(Player player, WeatherType weather) {
		if (this.weatherVotes.get(player.getUniqueId()) == weather)
			return;
		this.weatherVotes.put(player.getUniqueId(), weather);
		this.broadcastMessage(Messager.get("vote", player.getName(), Messager.get("weather." + weather)));
	}

	public void voteChests(Player player, ChestType chests) {
		if (this.chestVotes.get(player.getUniqueId()) == chests)
			return;
		this.chestVotes.put(player.getUniqueId(), chests);
		this.broadcastMessage(Messager.get("vote", player.getName(), Messager.get("chests." + chests)));
	}

}
