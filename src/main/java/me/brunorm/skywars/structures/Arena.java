package me.brunorm.skywars.structures;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import eu.decentsoftware.holograms.api.DHAPI;
import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.ChestManager;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.commands.CommandsUtils;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import mrblobman.sounds.Sounds;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class Arena {

	private Location location;
	private ArenaStatus status;
	private int countdown;
	private boolean joinable = true;
	private boolean invencibility = false;
	private boolean forcedStart;
	private Player forcedStartPlayer;
	private SkywarsUser winner;

	private final ArenaGameSettings gameSettings = new ArenaGameSettings(this);

	private final SkywarsMap map;

	public Arena get() {
		return this;
	}

	public SkywarsMap getMap() {
		return this.map;
	}

	private final ArrayList<SkywarsUser> users = new ArrayList<SkywarsUser>();
	private final ArrayList<SkywarsEvent> events = new ArrayList<SkywarsEvent>();
	private final ArrayList<Chest> chests = new ArrayList<Chest>();
	private final HashMap<Chest, String> chestHolograms = new HashMap<Chest, String>();

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
		if (event != null) {
			final int time = event.getTime();
			final int minutes = time / 60;
			final int seconds = time % 60;
			final String timeString = String.format("%d:%02d", minutes, seconds);
			return Messager.color(Skywars.langConfig.getString("events.format")
					.replaceAll("%name%",
							Skywars.langConfig.getString("events." + event.getType().name().toLowerCase()))
					.replaceAll("%time%", timeString));
		} else
			return Messager.getMessage("events.noevent");
	}

	public boolean joinPlayer(Player player) {
		if (!SkywarsUtils.joinableCheck(this, player))
			return false;
		if (!this.checkProblems()) {
			for (final String problem : this.getProblems()) {
				player.sendMessage(problem);
			}
			return false;
		}
		if (this.hasPlayer(player))
			return false;
		this.joinable = this.getAlivePlayerCount() < this.map.maxPlayers;
		final int index = this.getNextAvailablePlayerSlot();
		final Location spawn = this.getVectorInArena(this.getSpawn(index));
		if (spawn == null) {
			player.sendMessage(String.format("spawn %s of arena %s not set", index, this.map.getName()));
			return false;
		}
		final SkywarsUser swPlayer = new SkywarsUser(player, this, index);
		this.users.add(swPlayer);
		for (final SkywarsUser players : this.getUsers()) {
			players.getPlayer().sendMessage(Messager.getMessage("JOIN", player.getName(), this.getAlivePlayerCount(),
					this.map.getMaxPlayers()));
			SkywarsUtils.playSoundsFromConfig(player.getPlayer(), "sounds.join");
		}

		if (this.getTask() != null && this.getStatus() == ArenaStatus.STARTING)
			player.sendMessage(Messager.getMessage("GAME_STARTING", this.getCountdown()));

		Skywars.createCase(spawn, XMaterial.LIME_STAINED_GLASS);

		Skywars.get().playerLocations.put(player, player.getLocation());
		player.teleport(SkywarsUtils.getCenteredLocation(spawn));
		swPlayer.setSavedPlayer(new SavedPlayer(player));
		SkywarsUtils.clearPlayer(player, true);

		SkywarsUtils.setPlayerInventory(player, "waiting");

		Skywars.get().NMS().sendTitle(player, Skywars.langConfig.getString("arena_join.title"),
				Skywars.langConfig.getString("arena_join.subtitle"));

		if (this.getStatus() != ArenaStatus.STARTING && this.getUsers().size() >= this.map.getMinPlayers()) {
			this.startTimer(ArenaStatus.STARTING);
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
		player.getInventory().setArmorContents(null);

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

		if (this.getStatus() == ArenaStatus.PLAYING)
			// send death message to players
			for (final SkywarsUser players : this.getUsers()) {
				// TODO: add more death messages
				players.getPlayer().sendMessage(this.getDeathMessage(p, killer, cause));
			}

		this.removePlayer(p);

		p.getPlayer().teleport(SkywarsUtils.getCenteredLocation(this.getVectorInArena(this.getSpawn(p.teamNumber))));
		p.getPlayer().setVelocity(new Vector(0, 1f, 0));

		if (this.getWinner() != p)
			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					Skywars.get().NMS().sendTitle(player, Messager.getMessage("died.title"),
							Messager.getMessage("died.subtitle"), 0, 80, 0);
				}
			}, 20);
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

		if (this.getStatus() == ArenaStatus.STARTING && !this.forcedStart
				&& (this.map.getMinPlayers() <= 0 || this.getAlivePlayerCount() < this.map.getMinPlayers())) {
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
		SkywarsUtils.clearPlayer(player.getPlayer());
		player.getSavedPlayer().Restore();
		if (this.isInBoundaries(player.getPlayer()))
			SkywarsUtils.teleportPlayerBack(player.getPlayer());
	}

	void removePlayer(SkywarsUser player) {
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
		return this.startTimer(ArenaStatus.RESTARTING);
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

	public boolean startTimer(ArenaStatus status) {
		if (this.getStatus() == status)
			return false;
		if (this.started())
			if (status == ArenaStatus.STARTING || status == ArenaStatus.PLAYING)
				return false;
		if (!this.started() && status == ArenaStatus.RESTARTING)
			return false;
		this.cancelTimer();
		this.setStatus(status);
		Skywars.get().sendDebugMessage(String.format("starting %s timer", status));
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
						Arena.this.cancelTimer();
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
		this.setStatus(ArenaStatus.PLAYING);
		this.startTimer(this.getStatus());
		this.calculateChests();
		this.fillChests();
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
			case DAY:
				this.gameSettings.time = 0;
				break;
			case NIGHT:
				this.gameSettings.time = 14000;
				break;
			default:
				this.gameSettings.time = 0;
			}

		final WeatherType weather = SkywarsUtils.mostFrequentElement(this.weatherVotes.values());
		if (weather != null)
			switch (weather) {
			case CLEAR:
				this.gameSettings.weather = org.bukkit.WeatherType.CLEAR;
				break;
			case RAIN:
				this.gameSettings.weather = org.bukkit.WeatherType.DOWNFALL;
				break;
			default:
				this.gameSettings.weather = org.bukkit.WeatherType.CLEAR;
			}

		for (final SkywarsUser user : this.getUsers()) {
			user.getPlayer().setPlayerTime(this.gameSettings.time, false);
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
		this.forcedStart = false;
		this.forcedStartPlayer = null;
		this.setStatus(ArenaStatus.WAITING);
		this.countdown = -1;
		this.users.clear();
		if (this.getWorld() != null) {
			for (final Entity i : this.getWorld().getEntities()) {
				if (i instanceof Item && this.isInBoundaries(i.getLocation())) {
					i.remove();
				}
			}
		}
		if (remove)
			Skywars.get().clearArena(this);
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
		if (this.getWorld() == null)
			problems.add("World not set");
		if (this.getSpawn(this.getAlivePlayerCount()) == null)
			problems.add(String.format("Spawn %s not set", this.getAlivePlayerCount()));
		if (this.map.maxPlayers <= 0)
			problems.add("Max players not set");
		if (this.map.schematicFilename == null)
			problems.add("Schematic not set");
		if (this.location == null)
			problems.add("No location set");
		if (!this.isJoinable())
			problems.add("Arena is not joinable");
		return problems;
	}

	boolean checkProblems() {
		return this.getProblems().size() <= 0;
	}

	public Location getVectorInArena(Vector vector) {
		return new Location(this.getWorld(), vector.getBlockX() + this.location.getBlockX(),
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
		if (this.getLocation() == null || schematic == null)
			return true;
		if (loc.getWorld() != this.getWorld())
			return false;
		return loc.getX() > this.getLocation().getX() - schematic.getWidth() / 2
				&& loc.getX() < this.getLocation().getX() + schematic.getWidth() / 2
				&& loc.getY() > this.getLocation().getY() - schematic.getHeight() / 2
				&& loc.getY() < this.getLocation().getY() + schematic.getHeight() / 2
				&& loc.getZ() > this.getLocation().getZ() - schematic.getLength() / 2
				&& loc.getZ() < this.getLocation().getZ() + schematic.getLength() / 2;
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
		SchematicHandler.pasteSchematic(this.location, this.map.getSchematic());
	}

	public void clearBlocks() {
		SchematicHandler.clear(this.location, this.map.getSchematic());
	}

	public void calculateChests() {
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

		this.chests.clear();

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
				final Location loc = new Location(world, v.getX(), v.getY(), v.getZ()).add(offset).add(this.location);
				final Chest chest = (Chest) loc.getBlock().getState();
				if (this.chests.contains(chest))
					continue;
				this.chests.add(chest);
			}
		}
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
		/*
		 * final Schematic schematic = this.map.getSchematic(); final World world =
		 * this.location.getWorld(); final Vector offset = schematic.getOffset(); final
		 * ListTag tileEntities = schematic.getTileEntities(); //
		 * Skywars.get().sendDebugMessage("tile entities: " + //
		 * tileEntities.getValue().size());
		 *
		 * final byte[] blocks = schematic.getBlocks(); final short length =
		 * schematic.getLength(); final short width = schematic.getWidth(); final short
		 * height = schematic.getHeight();
		 *
		 * int filled = 0;
		 *
		 * for (int x = 0; x < width; ++x) { for (int y = 0; y < height; ++y) { for (int
		 * z = 0; z < length; ++z) { final int index = y * width * length + z * width +
		 * x; final Location loc = new Location(world, x, y,
		 * z).add(offset).add(this.location); final Block block = loc.getBlock(); if
		 * (block.getType() == XMaterial.CHEST.parseMaterial() || blocks[index] == 54 /*
		 * chest id * /) { this.chests.add((Chest) block.getState());
		 * ChestManager.fillChest(this.loc,
		 * SkywarsUtils.distance(this.getLocation().toVector(), this.loc.toVector()) <
		 * this.map.getCenterRadius()); //
		 * Skywars.get().sendDebugMessage("filling chest at loc " + this.loc); filled++;
		 * } }*}}**for(
		 *
		 * final Tag tag:tileEntities.getValue()){**
		 *
		 * @SuppressWarnings("unchecked") final Map<String, Tag> values = (Map<String,
		 * Tag>) tag.getValue(); if (values.get("id").getValue().equals("Chest")) {
		 *
		 * final Vector v = SchematicHandler.getVector(this.values); final Location loc
		 * = new Location(world, v.getX(), v.getY(),
		 * v.getZ()).add(offset).add(this.location);
		 *
		 * final Chest chest = (Chest) this.loc.getBlock()
		 * .getState();if*(this.chests.contains(chest))continue;ChestManager.fillChest(
		 * loc,*SkywarsUtils.distance(this.getLocation().toVector(),loc.toVector())<*
		 * this.map.getCenterRadius()); //
		 * Skywars.get().sendDebugMessage("filling chest entity at loc "+loc);*filled++;
		 * }}Skywars.get().sendDebugMessage("filled %s chests",filled);
		 */}

	public boolean softStart(Player player) {
		if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.start"))
			return false;
		if (!CommandsUtils.arenaCheckWithMessage(player))
			return false;
		if (this.getStatus() == ArenaStatus.WAITING && this.getTask() == null) {
			this.forcedStart = true;
			this.forcedStartPlayer = player;
			this.startTimer(ArenaStatus.STARTING);
			player.sendMessage(Messager.get("started_countdown"));
			return true;
		}
		final boolean started = this.startGame();
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

	public World getWorld() {
		final Location loc = this.getLocation();
		if (this.location == null)
			return null;
		return loc.getWorld();
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
			DHAPI.removeHologram(this.chestHolograms.remove(chest));
		return this.chests.remove(chest);
	}

	public boolean addChestHologram(Chest chest) {
		if (!Skywars.holograms)
			return true;
		if (this.chestHolograms.containsKey(chest))
			return false;
		final Block block = chest.getBlock();
		final String name = Skywars.get().getHologramController().createHologram(
				"Skywars-chest-" + block.getLocation().getX() + "-" + block.getLocation().getY() + "-"
						+ block.getLocation().getZ() + "-" + Instant.now().toString(),
				block.getLocation().add(new Vector(0.5, 2, 0.5)), "");
		this.chestHolograms.put(chest, name);
		return true;
	}

	public void displayChestHolograms(String text) {
		if (!Skywars.holograms)
			return;
		for (final String holoName : this.chestHolograms.values()) {
			Skywars.get().getHologramController().changeHologram(holoName, text);
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
