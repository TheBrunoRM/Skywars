/* (C) 2021 Bruno */
package me.thebrunorm.skywars.structures;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.*;
import me.thebrunorm.skywars.commands.CommandsUtils;
import me.thebrunorm.skywars.holograms.HologramController;
import me.thebrunorm.skywars.managers.ArenaManager;
import me.thebrunorm.skywars.managers.ChestManager;
import me.thebrunorm.skywars.managers.MapManager;
import mrblobman.sounds.Sounds;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Arena {

	final HashMap<UUID, TimeType> timeVotes = new HashMap<>();
	final HashMap<UUID, WeatherType> weatherVotes = new HashMap<>();
	final HashMap<UUID, ChestType> chestVotes = new HashMap<>();
	private final ArenaGameSettings gameSettings = new ArenaGameSettings(this);
	private final SkywarsMap map;
	private final ArrayList<SkywarsTeam> teams = new ArrayList<>();
	private final ArrayList<SkywarsUser> users = new ArrayList<>();
	private final ArenaEventManager eventManager = new ArenaEventManager(this);
	// TODO store chest locations and types (normal or center chests)
	private final ArrayList<Chest> activeChests = new ArrayList<>();
	private final HashMap<Chest, String> chestHolograms = new HashMap<>();
	String worldName;
	BukkitTask task;
	private World world;
	private ArenaStatus status;
	private int countdown;
	private boolean joinable = true;
	private boolean invincibility = false;
	private boolean forcedStart;
	private Player forcedStartPlayer;
	private SkywarsTeam winningTeam;
	private int minimumY = -1;

	public Arena(SkywarsMap map) {
		this.map = map;
		this.status = ArenaStatus.WAITING;
	}

	public Arena get() {
		return this;
	}

	public ArrayList<SkywarsTeam> getTeams() {
		return this.teams;
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
		final SkywarsTeam team = this.getNextFreeTeamOrCreateIfItDoesntExist();
		final Location spawn = this.getVectorInArena(this.getSpawn(team.getNumber()));
		if (spawn == null) {
			player.sendMessage(String.format("spawn %s of arena %s not set", team.getNumber(), this.map.getName()));
			return false;
		}
		final SkywarsUser swPlayer = new SkywarsUser(player, team, team.getNumber());
		this.users.add(swPlayer);
		if (Skywars.config.getBoolean("debug.enabled"))
			MessageUtils.send(player, "[DEBUG] You joined team %s as player %s", team.getNumber(), getAlivePlayerCount());
		for (final SkywarsUser players : this.getUsers()) {
			MessageUtils.sendTranslated(players.getPlayer(), "JOIN",
					player.getName(), this.getAlivePlayerCount(), this.map.getMaxPlayers());
			SkywarsUtils.playSoundsFromConfig(player.getPlayer(), "sounds.join");
		}

		if (this.getTask() != null && this.getStatus() == ArenaStatus.STARTING)
			player.sendMessage(MessageUtils.getMessage("GAME_STARTING", this.getCountdown()));

		SkywarsCaseCreator.createCase(spawn, Skywars.get().getPlayerCaseXMaterial(player));

		Skywars.get().playerLocations.put(player, player.getLocation());
		swPlayer.setSavedPlayer(new SavedPlayer(player));
		player.teleport(SkywarsUtils.getCenteredLocation(spawn));
		SkywarsUtils.clearPlayer(player, true);
		SkywarsUtils.setPlayerInventory(player, "waiting");

		Skywars.get().NMS().sendTitle(player, Skywars.langConfig.getString("arena_join.title"),
				Skywars.langConfig.getString("arena_join.subtitle"));

		if (this.getStatus() != ArenaStatus.STARTING && this.getTeams().size() >= Skywars.config.getInt("minTeams", 2)) {
			this.startTimerAndSetStatus(ArenaStatus.STARTING);
		}

		Skywars.get().getSignManager().updateSigns();

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

	boolean checkProblems() {
		return this.getProblems().size() <= 0;
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
		Skywars.get().sendDebugMessage("starting timer: %s", status);
		if (status == ArenaStatus.STARTING) {
			if (this.forcedStart && this.forcedStartPlayer != null) {
				for (final SkywarsUser player : this.getUsers()) {
					player.getPlayer()
							.sendMessage(MessageUtils.getMessage("FORCED_START", this.forcedStartPlayer.getName()));
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
								splitted.length > 1 ? Float.parseFloat(splitted[1]):1,
								splitted.length > 2 ? Float.parseFloat(splitted[2]):1);

						for (final Object object : Skywars.langConfig.getList("countdown")) {
							@SuppressWarnings("unchecked") final HashMap<Object, Object> hash = (HashMap<Object, Object>) object;

							if (!isCurrentCountdown(time, hash.get("range")))
								continue;

							final String title = (String) hash.get("title");
							final String subtitle = (String) hash.get("subtitle");
							final String color = String.valueOf(hash.get("color"));
							Skywars.get().NMS().sendTitle(player.getPlayer(),
									SkywarsUtils.format(title, player.getPlayer(), Arena.this.get(), player),
									SkywarsUtils.format(subtitle, player.getPlayer(), Arena.this.get(), player), 0,
									50, 0);
							final String msg = Skywars.langConfig
									.getString(this.time == 1 ? "GAME_STARTING_SECOND":"GAME_STARTING_SECONDS");
							player.getPlayer()
									.sendMessage(MessageUtils.color(SkywarsUtils.format(
											msg.replaceAll("%count%", "&" + color + "%count%")
													.replaceAll("%seconds%", "&" + color + "%seconds%"),
											player.getPlayer(), Arena.this.get(), player)));
						}
					}
				}
			}, 0L, 20L);
		} else if (status == ArenaStatus.PLAYING) {
			this.task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				@Override
				public void run() {

					final SkywarsEvent event = Arena.this.getEventManager().getNextEvent();

					if (event == null) return;

					event.decreaseTime();
					if (event.getTime() <= 0) {
						Arena.this.getEventManager().executeEvent();
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

					if (winningTeam != null)
						for (SkywarsUser winner : winningTeam.getUsers()) {
							if (this.fireworks <= Skywars.get().getConfig().getInt("endFireworks") && winner != null
									&& Arena.this.getUser(winner.getPlayer()) != null && !winner.isSpectator()) {
								final Player p = winner.getPlayer();
								SkywarsUtils.spawnRandomFirework(p.getLocation());
							}
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

	public void teleportPlayerToOwnSpawnAsSpectator(SkywarsUser user) {
		Player player = user.getPlayer();
		player.setAllowFlight(true);
		player.setFlying(true);
		player.teleport(SkywarsUtils.getCenteredLocation(this.getVectorInArena(this.getSpawn(user.teamNumber))));
		player.setVelocity(new Vector(0, 1f, 0));
	}

	private void spectator(SkywarsUser user) {
		// TODO: make customizable spectator mode

		Player player = user.getPlayer();

		SkywarsUtils.clearPlayer(player, true);
		player.setGameMode(GameMode.ADVENTURE);

		for (final Player players : Bukkit.getOnlinePlayers()) {
			players.hidePlayer(player);
		}

		for (final SkywarsUser users : this.getSpectators()) {
			users.player.showPlayer(player);
			player.showPlayer(users.player);
		}

		// give spectator items (player tracker, spectator settings, leave item)
		SkywarsUtils.setPlayerInventory(player, "spectator");
		teleportPlayerToOwnSpawnAsSpectator(user);
	}

	public void leavePlayer(Player player) {
		this.leavePlayer(this.getUser(player));
	}

	public void exitPlayer(Player player) {
		this.exitPlayer(this.getUser(player));
	}

	public void exitPlayer(SkywarsUser player) {
		if (player == null)
			return;

		if (this.isInBoundaries(player.getPlayer()))
			SkywarsUtils.teleportPlayerLobbyOrLastLocation(player.getPlayer());

		player.getSavedPlayer().Restore();
	}

	public SkywarsUser getUser(Player player) {
		for (final SkywarsUser swp : this.getUsers()) {
			if (!swp.getPlayer().getName().equals(player.getName())) continue;
			return swp;
		}
		return null;
	}

	public boolean isInBoundaries(Player player) {
		return this.isInBoundaries(player.getLocation());
	}

	public ArrayList<SkywarsUser> getUsers() {
		return this.users;
	}

	public boolean isInBoundaries(Location loc) {
		return loc.getY() > this.getMinimumY();
	}

	private int getMinimumY() {
		if (this.minimumY >= 0)
			return this.minimumY;

		if (this.getMap().getConfig().get("minimumY") != null) {
			this.minimumY = this.getMap().getConfig().getInt("minimumY");
			Skywars.get().sendDebugMessage("%s: Got minimum Y from config.", this.getMap().getName());
			return this.minimumY;
		}

		Skywars.get().sendDebugMessage("%s: Calculating minimum Y...", this.getMap().getName());
		final long start = Instant.now().toEpochMilli();

		this.minimumY = this.findLowerBlockY();
		this.getMap().getConfig().set("minimumY", this.minimumY);
		this.getMap().saveConfig();
		Skywars.get().sendDebugMessage("&bCalculated minimum Y for %s in %sms: %s", this.map.getName(),
				Instant.now().toEpochMilli() - start, this.minimumY);
		return this.minimumY;
	}

	public SkywarsMap getMap() {
		return this.map;
	}

	private int findLowerBlockY() {
		final Material air = XMaterial.AIR.parseMaterial();
		for (int y = 0; y < 256; y++) {
			for (int x = 0; x < 256; x++) {
				for (int z = 0; z < 256; z++) {
					if (this.getWorld().getBlockAt(x, y, z).getType() != air) {
						return y;
					}
				}
			}
		}
		return -1;
	}

	public World getWorld() {
		if (this.world != null)
			return this.world;

		if (this.getMap().getWorldName() == null) {
			Skywars.get().sendDebugMessage("map world name is null, could not get world: " + this.getMap().getName());
			return null;
		}

		String worldName = this.getWorldName();
		if (worldName == null)
			worldName = this.getMap().getWorldName() + System.currentTimeMillis();

		final World w = Bukkit.getWorld(worldName);
		if (w != null) {
			this.world = w;
			return w;
		}

		Skywars.get().sendDebugMessage("world is not loaded");
		Skywars.get().sendDebugMessage("loading world by copying it from the worlds folder");

		final File worldFolder = new File(Skywars.worldsPath, this.getMap().getWorldName());
		if (!worldFolder.isDirectory()) {
			Skywars.get().sendDebugMessage("could not find the world in the worlds folder");
			return null;
		}

		Skywars.get().sendDebugMessage("Loading world for map: " + this.getMap().getName());
		final File bukkitWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
		try {
			if (bukkitWorldFolder.isDirectory()) {
				Skywars.get().sendDebugMessage("deleting old world folder: %s", bukkitWorldFolder.getAbsolutePath());
				if (!bukkitWorldFolder.delete())
					Skywars.get().sendDebugMessage("could not delete old world folder: %s", bukkitWorldFolder.getAbsolutePath());
			}
			if (!bukkitWorldFolder.canWrite()) {
				Skywars.get().sendDebugMessage("cant write to world folder: %s", bukkitWorldFolder.getAbsolutePath());
			}
			if (!bukkitWorldFolder.mkdirs())
				Skywars.get().sendDebugMessage("could not make directory for world folder: %s", bukkitWorldFolder.getAbsolutePath());
			Skywars.get().sendDebugMessage("copying: %s", worldFolder.getAbsolutePath());
			Skywars.get().sendDebugMessage("to location: %s", bukkitWorldFolder.getAbsolutePath());
			FileUtils.copyDirectory(worldFolder, bukkitWorldFolder);
			this.setWorldName(worldName);
			final File newParent = new File(Bukkit.getWorldContainer(), worldName);
			if (!bukkitWorldFolder.renameTo(newParent))
				Skywars.get().sendDebugMessage("could not move world folder: %s", bukkitWorldFolder.getAbsolutePath());
			final File uid = new File(newParent, "uid.dat");
			if (!uid.exists())
				Skywars.get().sendDebugMessage("could not find uid.dat: %s", newParent.getAbsolutePath());
			if (uid.delete())
				Skywars.get().sendDebugMessage("Deleted uid.dat from %s", newParent.getAbsolutePath());
			else
				Skywars.get().sendDebugMessage("&cCould not delete uid.dat from %s", newParent.getAbsolutePath());
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
		MapManager.setupWorld(world);
		return world;
	}

	public String getWorldName() {
		return this.worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public void makeSpectator(SkywarsUser user, Player killer, DamageCause cause) {
		if (!this.started())
			return;

		if (user.isSpectator())
			return;

		user.setSpectator(true);
		final Player player = user.getPlayer();

		if (killer != null) {
			final SkywarsUser killerPlayer = this.getUser(killer);
			if (killerPlayer != null) {
				Skywars.get().incrementPlayerTotalKills(killer);
				final double killMoney = Skywars.get().getConfig().getDouble("economy.kill");
				if (SkywarsEconomy.getEconomy() != null && killMoney > 0) {
					SkywarsEconomy.getEconomy().depositPlayer(killer, killMoney);
					killer.sendMessage(MessageUtils.color("&6+$%s", SkywarsUtils.formatDouble(killMoney)));
				}
				Skywars.get().incrementPlayerSouls(killer);
				killer.sendMessage(MessageUtils.color("&b+%s Soul", 1));
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
				players.getPlayer().sendMessage(this.getDeathMessage(user, killer, cause));
			}

		this.spectator(user);
		this.removePlayer(user);

		if (winningTeam == null || !winningTeam.getUsers().contains(user))
			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					Skywars.get().NMS().sendTitle(player, MessageUtils.getMessage("died.title"),
							MessageUtils.getMessage("died.subtitle"), 0, 80, 0);
				}
			}, 20);
	}

	private String getDeathMessage(SkywarsUser p, Player killer, DamageCause cause) {
		final Player player = p.getPlayer();
		if (killer != null)
			return MessageUtils.color("&c%s &ekilled &c%s", killer.getName(), player.getName());
		else if (p.getLastHit() != null)
			return MessageUtils.color("&c%s &edied while trying to escape &c%s", player.getName(),
					p.getLastHit().getName());
		else if (cause == DamageCause.VOID)
			return MessageUtils.color("&c%s &efell in the void.", player.getName());
		else
			return MessageUtils.color("&c%s &edied.", player.getName());
	}

	public void removePlayer(SkywarsUser player) {
		if (this.getStatus() != ArenaStatus.PLAYING)
			return;

		long aliveTeamsCount = this.getTeams().stream()
				.filter(team -> team.getUsers().stream().anyMatch(p -> !p.isSpectator()))
				.count();

		if (aliveTeamsCount <= 1) {
			Optional<SkywarsTeam> winning = this.getTeams().stream()
					.filter(team -> team.getUsers().stream().anyMatch(p -> !p.isSpectator()))
					.findFirst();

			winning.ifPresent(skywarsTeam -> winningTeam = skywarsTeam);

			this.endGame();
			return;
		}

		// Si aún hay más de un equipo vivo, solo actualizamos el número de jugadores restantes.
		for (final SkywarsUser p : this.getUsers()) {
			Skywars.get().NMS().sendActionbar(p.getPlayer(),
					MessageUtils.getMessage("PLAYERS_REMAINING", this.getAlivePlayerCount()));
		}
	}

	public BukkitTask getTask() {
		return this.task;
	}

	void cancelTimer() {
		if (this.getTask() == null)
			return;
		this.getTask().cancel();
		this.task = null;
	}

	boolean isCurrentCountdown(int time, Object range) {
		if (range.getClass().equals(Integer.class) && time == (Integer) range)
			return true;

		if (!range.getClass().equals(String.class))
			return false;

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
		return time >= first && time <= last;
	}

	public ArenaEventManager getEventManager() {
		return eventManager;
	}

	public boolean startGame() {
		return this.startGame(null);
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
		this.forcedStart = false;
		this.forcedStartPlayer = null;
		this.setStatus(ArenaStatus.WAITING);
		this.countdown = -1;

		if (remove)
			ArenaManager.removeArena(this);
	}

	void removeHolograms() {
		for (final String holoName : this.chestHolograms.values()) {
			Skywars.get().getHologramController().removeHologram(holoName);
		}
		this.chestHolograms.clear();
	}

	public boolean endGame() {
		if (this.getStatus() == ArenaStatus.RESTARTING)
			return false;

		final double winMoney = Skywars.get().getConfig().getDouble("economy.win");

		if (winningTeam != null)
			for (SkywarsUser user : winningTeam.getUsers()) {
				Skywars.get().incrementPlayerTotalWins(user.getPlayer());
				if (winMoney > 0 && SkywarsEconomy.getEconomy() != null) {
					SkywarsEconomy.getEconomy().depositPlayer(user.getPlayer(), winMoney);
					user.getPlayer().sendMessage(MessageUtils.color("&6+$%s", SkywarsUtils.formatDouble(winMoney)));
				}
			}

		for (final SkywarsUser user : this.getUsers()) {
			if (winningTeam != null && winningTeam.getUsers().contains(user)) {
				Skywars.get().NMS().sendTitle(user.getPlayer(), MessageUtils.getMessage("won.title"),
						MessageUtils.getMessage("won.subtitle"), 0, 80, 0);
			} else {
				Skywars.get().NMS().sendTitle(user.getPlayer(),
						"&c&lGAME ENDED", "&7You didn't win this time.", 0, 80, 0);
			}
			if (winningTeam == null || winningTeam.getUsers().isEmpty()) {
				user.getPlayer().sendMessage(MessageUtils.color("&cnobody &ewon"));
			} else {
				user.getPlayer().sendMessage(MessageUtils.color("&c%s &ewon!",
						winningTeam.getUsers().stream().map(winner -> winner.getPlayer().getName())
								.collect(Collectors.joining(", "))));
			}
		}

		this.removeHolograms();
		return this.startTimerAndSetStatus(ArenaStatus.RESTARTING);
	}

	public void leavePlayer(SkywarsUser player) {
		if (player == null)
			return;
		player.getPlayer().sendMessage(
				MessageUtils.getFormattedMessage("LEAVE_SELF", player.getPlayer(), this, player, this.map.getName()));
		if (!this.started())
			this.joinable = true;
		player.getTeam().removeUser(player);
		if (player.getTeam().getUsers().size() <= 0)
			player.getTeam().disband();
		this.users.remove(player);
		this.removePlayer(player);
		if (this.getStatus() != ArenaStatus.RESTARTING && !player.isSpectator()) {
			for (final SkywarsUser players : this.getUsers()) {
				players.getPlayer().sendMessage(MessageUtils.getFormattedMessage("LEAVE", player.getPlayer(), this, player,
						player.getPlayer().getName(), this.getUsers().size(), this.map.getMaxPlayers()));
				final String sound = Skywars.config.getString("sounds.leave");
				final String[] splitted = sound.split(";");
				players.getPlayer().playSound(players.getPlayer().getLocation(),
						Sounds.valueOf(splitted[0]).bukkitSound(),
						splitted.length > 1 ? Float.parseFloat(splitted[1]):1,
						splitted.length > 2 ? Float.parseFloat(splitted[2]):1);
			}
		}

		// restore player
		this.exitPlayer(player);

		final int minTeams = Skywars.config.getInt("minTeams", 2);
		if (this.getStatus() == ArenaStatus.STARTING && !this.forcedStart
				&& (minTeams <= 0 || this.getTeams().size() < minTeams)) {
			// Skywars.get().sendDebugMessage("stopping start cooldown");
			this.setStatus(ArenaStatus.WAITING);
			for (final SkywarsUser players : this.getUsers()) {
				players.getPlayer().sendMessage(MessageUtils.getMessage("COUNTDOWN_STOPPED", this.getTeams().size()));
			}
			this.cancelTimer();
		}
		if (this.status != ArenaStatus.WAITING && this.getTeams().size() <= 0)
			this.clear();

		Skywars.get().getSignManager().updateSigns();
	}

	private void applyGameSettings() {

		final TimeType time = SkywarsUtils.mostFrequentElement(this.timeVotes.values());
		if (time != null)
			this.gameSettings.time = time == TimeType.NIGHT ? 14000:0;

		final WeatherType weather = SkywarsUtils.mostFrequentElement(this.weatherVotes.values());
		if (weather != null)
			this.gameSettings.weather = weather == WeatherType.RAIN ? org.bukkit.WeatherType.DOWNFALL:org.bukkit.WeatherType.CLEAR;

		for (final SkywarsUser user : this.getUsers()) {
			user.getPlayer().setPlayerTime(this.gameSettings.time, true);
			user.getPlayer().setPlayerWeather(this.gameSettings.weather);
		}

		// TODO apply chest type
	}

	public void resetCases() {
		for (final Vector spawn : this.map.getSpawns().values()) {
			SkywarsCaseCreator.createCase(this.getVectorInArena(spawn), XMaterial.RED_STAINED_GLASS);
		}
	}

	public Location getVectorInArena(Vector vector) {
		final World world = this.getWorld();
		final Location loc = new Location(world, 0, 0, 0);
		return new Location(world, vector.getBlockX() + loc.getBlockX(), vector.getBlockY() + loc.getBlockY(),
				vector.getBlockZ() + loc.getBlockZ());
	}

	public Location getLocationInArena(Location loc) {
		return new Location(loc.getWorld(), loc.getBlockX() + this.getCenterBlock().getBlockX(),
				loc.getBlockY() + this.getCenterBlock().getBlockY(),
				loc.getBlockZ() + this.getCenterBlock().getBlockZ());
	}

	public Vector getCenterBlock() {
		return new Vector(0, Skywars.get().getConfig().getInt("defaultArenaCenterHeight", 100), 0);
	}

	public void goBackToCenter(Player player) {
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setVelocity(new Vector(0, 0, 0));
		player.teleport(this.getCenterBlock().toLocation(this.getWorld()));
		player.setVelocity(new Vector(0, 5f, 0));
	}

	private int getNextAvailablePlayerSlot() {
		for (int i = 0; i < this.map.getMaxPlayers(); i++) {
			if (this.getUser(i) != null) continue;
			return i;
		}
		return -1;
	}

	SkywarsUser getUser(int index) {
		for (final SkywarsUser swp : this.getUsers()) {
			if (swp.getNumber() != index) continue;
			return swp;
		}
		return null;
	}

	public int getAlivePlayerCount() {
		return this.users.stream().filter(player -> !player.isSpectator()).collect(Collectors.toList()).size();
	}

	public ArrayList<SkywarsUser> getAlivePlayers() {
		return this.users.stream().filter(player -> !player.isSpectator())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	ArrayList<SkywarsUser> getSpectators() {
		return this.users.stream().filter(player -> player.isSpectator())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public boolean startGame(Player playerStarted) {
		if (playerStarted != null) {
			if (!CommandsUtils.hasPermission(playerStarted, "skywars.forcestart"))
				return false;
			if (!CommandsUtils.isInArenaJoined(playerStarted))
				return false;
		}
		if (this.started())
			return false;
		this.cancelTimer();
		this.startTimerAndSetStatus(ArenaStatus.PLAYING);
		this.fillChests();
		this.applyGameSettings();
		for (final Vector spawn : this.map.getSpawns().values()) {
			SkywarsCaseCreator.createCase(this.getVectorInArena(spawn), XMaterial.AIR);
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
			player.getPlayer().sendMessage(MessageUtils.getMessage("arena_start.message"));
			Skywars.get().NMS().sendTitle(player.getPlayer(), MessageUtils.getMessage("arena_start.title"),
					MessageUtils.getMessage("arena_start.subtitle"));
			SkywarsUtils.playSoundsFromConfig(player.getPlayer(), "sounds.start");

			Bukkit.getScheduler().runTaskLater(Skywars.get(), () -> {
				if (!Arena.this.started() || Arena.this.getUser(player.getPlayer()) == null || player.isSpectator())
					return;
				for (final String l : Skywars.langConfig.getStringList("startLines")) {
					player.getPlayer().sendMessage(MessageUtils.color(l));
				}
			}, 20);

			Bukkit.getScheduler().runTaskLater(Skywars.get(), () -> {
				if (!Arena.this.started() || Arena.this.getUser(player.getPlayer()) == null || player.isSpectator())
					return;
				final double playMoney = Skywars.get().getConfig().getDouble("economy.play");
				if (SkywarsEconomy.getEconomy() != null && playMoney > 0) {
					SkywarsEconomy.getEconomy().depositPlayer(player.getPlayer(), playMoney);
					player.getPlayer().sendMessage(MessageUtils.color("&6+$%s", SkywarsUtils.formatDouble(playMoney)));
				}
			}, 20 * 10);
		}

		if (Skywars.configuration.invencibilityEnabled) {
			this.setInvincibility(true);
			Bukkit.getScheduler().runTaskLater(Skywars.get(),
					() -> Arena.this.setInvincibility(false), Skywars.configuration.invencibilityTicks);
		}

		return true;
	}

	public ArrayList<String> getProblems() {
		final ArrayList<String> problems = new ArrayList<>();
		if (this.getWorld() == null)
			problems.add("World not found");
		if (this.getSpawn(this.getAlivePlayerCount()) == null)
			problems.add(String.format("Spawn %s not set", this.getAlivePlayerCount()));
		if (this.map.getMaxPlayers() <= 0)
			problems.add("No players can join: no spawns!");
		if (this.getWorldName() == null) {
			problems.add("No world set");
		}
		if (this.isUnusable())
			problems.add("Arena is not joinable");
		return problems;
	}

	public boolean hasPlayer(Player player) {
		for (SkywarsUser user : this.users) {
			if (!user.getPlayer().getName().equals(player.getName())) continue;
			return true;
		}
		return false;
	}

	public SkywarsUser getUser(String name) {
		for (final SkywarsUser swp : this.getUsers()) {
			if (!swp.getPlayer().getName().equals(name)) continue;
			return swp;
		}
		return null;
	}

	public void fillChests() {
		if (this.getMap().getChests().isEmpty()) {
			Skywars.get().sendDebugMessage("&6Tried to fill chests but list is empty. Calculating chests for map: %s",
					this.getMap().getName());
			this.getMap().calculateChests();
		}

		if (this.activeChests.isEmpty()) {
			for (final Vector chestPos : this.getMap().getChests().values()) {
				final Block block = this.getWorld().getBlockAt(chestPos.toLocation(this.getWorld()));
				if (block.getState().getType() != XMaterial.CHEST.parseMaterial())
					continue;
				this.activeChests.add((Chest) block.getState());
			}
		}

		for (final Chest chest : this.activeChests) {
			final Block block = chest.getBlock();
			if (!(block.getState() instanceof Chest))
				continue;
			final Location loc = block.getLocation();
			ChestManager.fillChest(loc,
					SkywarsUtils.distance(this.getCenterBlock(), loc.toVector()) < this.map.getCenterRadius());
		}
	}

	public void broadcastEventMessage(SkywarsEventType eventType) {
		for (final SkywarsUser player : this.getUsers()) {
			player.getPlayer().sendMessage(MessageUtils.getMessage(String.format("events.%s.message", eventType)));
			Skywars.get().NMS().sendTitle(player.getPlayer(), MessageUtils.getMessage(String.format("events.%s.title", eventType)),
					MessageUtils.getMessage(String.format("events.%s.subtitle", eventType)));
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

	public ArrayList<BlockState> getAllBlockStatesInMap(Material mat) {
		final ArrayList<BlockState> list = new ArrayList<>();

		final World world = this.getWorld();
		if (world == null) {
			Skywars.get().sendMessage("Could not get world for map: ", this.getMap().getName());
			return list;
		}

		for (final Chunk chunk : this.getAllChunksInMap()) {
			for (final BlockState state : chunk.getTileEntities()) {
				if (state.getType() != mat)
					continue;
				list.add(state);
			}
		}

		return list;
	}

	public ArrayList<Chunk> getAllChunksInMap() {
		final ArrayList<Chunk> list = new ArrayList<>();

		final World world = this.getWorld();
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

	public boolean isInvincibility() {
		return this.invincibility;
	}

	public void setInvincibility(boolean invincibility) {
		this.invincibility = invincibility;
	}

	public ArrayList<Chest> getActiveChests() {
		return this.activeChests;
	}

	public ArrayList<Block> getAllBlocksInMap(Material mat) {
		final ArrayList<Block> list = new ArrayList<>();

		final World world = this.getWorld();
		if (world == null) {
			Skywars.get().sendMessage("&cCould not get world for map: ", this.getMap().getName());
			return list;
		}

		Skywars.get().sendDebugMessage("chunks gotten: " + this.getAllChunksInMap().size());

		for (final Chunk chunk : this.getAllChunksInMap()) {
			for (int x = 0; x < 15; x++) {
				for (int z = 0; z < 15; z++) {
					for (int y = 0; y < 256; y++) {
						final Block block = chunk.getBlock(x, y, z);
						if (block.getType() == mat)
							list.add(block);
					}
				}
			}
		}

		return list;
	}

	public boolean softStart(Player player) {
		if (!CommandsUtils.hasPermission(player, "skywars.start"))
			return false;
		if (!CommandsUtils.isInArenaJoined(player))
			return false;
		if (this.getStatus() == ArenaStatus.WAITING && this.getTask() == null) {
			this.forcedStart = true;
			this.forcedStartPlayer = player;
			this.startTimerAndSetStatus(ArenaStatus.STARTING);
			player.sendMessage(MessageUtils.get("started_countdown"));
			return true;
		}
		final boolean started = this.startGame(player);
		if (started)
			player.sendMessage(MessageUtils.get("started_game"));
		else
			player.sendMessage(MessageUtils.get("already_started"));
		return started;
	}

	public boolean isUnusable() {
		return !this.joinable;
	}

	public ArenaGameSettings getGameSettings() {
		return this.gameSettings;
	}

	public void removeChest(Chest chest) {
		if (!Skywars.holograms)
			return;
		if (this.chestHolograms.containsKey(chest))
			Skywars.get().getHologramController().removeHologram(this.chestHolograms.remove(chest));
		this.activeChests.remove(chest);
	}

	public void addChestHologram(Chest chest) {
		if (!Skywars.holograms)
			return;
		if (this.chestHolograms.containsKey(chest))
			return;
		final Block block = chest.getBlock();
		final String name = Skywars.get().getHologramController().createHologram(
				"Skywars_chest_" + block.getLocation().getBlockX() + "_" + block.getLocation().getBlockY() + "_"
						+ block.getLocation().getBlockZ() + "_" + Instant.now().toEpochMilli(),
				block.getLocation().add(new Vector(0.5, 2, 0.5)), "");
		this.chestHolograms.put(chest, name);
	}

	public void displayChestHolograms(String text) {
		if (!Skywars.holograms)
			return;

		final HologramController controller = Skywars.get().getHologramController();
		for (final Entry<Chest, String> entry : this.chestHolograms.entrySet()) {
			final Chest chest = entry.getKey();
			final String hologram = entry.getValue();
			final int contents = Arrays.asList(chest.getInventory().getContents()).stream()
					.filter(i -> i != null && i.getType() != XMaterial.AIR.parseMaterial()).collect(Collectors.toList())
					.size();
			controller.changeHologram(hologram, MessageUtils.color(text), 0);
			controller.changeHologram(hologram, contents <= 0 ? MessageUtils.get("chest_holograms.empty"):"", 1);
		}

	}

	public void voteTime(Player player, TimeType time) {
		if (this.timeVotes.get(player.getUniqueId()) == time)
			return;
		this.timeVotes.put(player.getUniqueId(), time);
		this.broadcastMessage(MessageUtils.get("vote", player.getName(), MessageUtils.get("time." + time)));
	}

	public void broadcastMessage(String string) {
		for (final SkywarsUser user : this.getUsers())
			user.getPlayer().sendMessage(string);
	}

	public void voteWeather(Player player, WeatherType weather) {
		if (this.weatherVotes.get(player.getUniqueId()) == weather)
			return;
		this.weatherVotes.put(player.getUniqueId(), weather);
		this.broadcastMessage(MessageUtils.get("vote", player.getName(), MessageUtils.get("weather." + weather)));
	}

	public void voteChests(Player player, ChestType chests) {
		if (this.chestVotes.get(player.getUniqueId()) == chests)
			return;
		this.chestVotes.put(player.getUniqueId(), chests);
		this.broadcastMessage(MessageUtils.get("vote", player.getName(), MessageUtils.get("chests." + chests)));
	}
}
