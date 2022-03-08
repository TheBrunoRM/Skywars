package me.brunorm.skywars.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jnbt.ListTag;
import org.jnbt.Tag;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.ChestManager;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import mrblobman.sounds.Sounds;

public class Arena {

	World world;
	Location location;
	ArenaStatus status;
	int countdown;
	boolean joinable = true;
	boolean invencibility = false;
	public boolean forcedStart;
	public Player forcedStartPlayer;
	SkywarsPlayer winner;

	SkywarsMap map;

	public Arena get() {
		return this;
	}

	public SkywarsMap getMap() {
		return this.map;
	}

	private final ArrayList<SkywarsPlayer> users = new ArrayList<SkywarsPlayer>();
	ArrayList<SkywarsEvent> events = new ArrayList<SkywarsEvent>();

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
		final SkywarsEvent event = this.getNextEvent();
		if (this.status == ArenaStatus.ENDING)
			return Skywars.langConfig.getString("status.ended");
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
			return "No event";
	}

	public boolean joinPlayer(Player player) {
		if (!SkywarsUtils.JoinableCheck(this, player))
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
		final SkywarsPlayer swPlayer = new SkywarsPlayer(player, this, index);
		this.users.add(swPlayer);
		for (final SkywarsPlayer players : this.getUsers()) {
			players.getPlayer().sendMessage(Messager.colorFormat("&7%s &ehas joined (&b%s&e/&b%s&e)!", player.getName(),
					this.getAlivePlayerCount(), this.map.getMaxPlayers()));
			final String sound = Skywars.config.getString("sounds.join");
			final String[] splitted = sound.split(";");
			player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.valueOf(splitted[0]).bukkitSound(),
					splitted.length > 1 ? Float.parseFloat(splitted[1]) : 1f,
					splitted.length > 2 ? Float.parseFloat(splitted[2]) : 1f);
		}

		if (this.getTask() != null && this.getStatus() == ArenaStatus.STARTING)
			player.sendMessage(Messager.colorFormat("&eThe game is starting in &6%s &eseconds!", this.getCountdown()));

		Skywars.createCase(spawn, XMaterial.LIME_STAINED_GLASS);

		Skywars.get().playerLocations.put(player, player.getLocation());
		player.teleport(SkywarsUtils.getCenteredLocation(spawn));
		swPlayer.setSavedPlayer(new SavedPlayer(player));
		SkywarsUtils.ClearPlayer(player);

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

	public void makeSpectator(SkywarsPlayer p, Player killer) {
		if (p.isSpectator())
			return;

		p.setSpectator(true);
		final Player player = p.getPlayer();

		if (killer != null) {
			final SkywarsPlayer killerPlayer = this.getPlayer(killer);
			if (killerPlayer != null) {
				Skywars.get().incrementPlayerTotalKills(killer);
				final double killMoney = Skywars.get().getConfig().getDouble("economy.kill");
				if (Skywars.get().getEconomy() != null && killMoney > 0) {
					Skywars.get().getEconomy().depositPlayer(killer, killMoney);
					killer.sendMessage(Messager.colorFormat("&6+$%s", SkywarsUtils.formatDouble(killMoney)));
				}
			}
			Skywars.get().incrementPlayerTotalDeaths(player);
		}

		// only drop items if the player is inside the arena
		// if the player dies for void, it will not drop items
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

		SkywarsUtils.ClearPlayer(player);
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
			for (final SkywarsPlayer players : this.getUsers()) {
				// TODO: add more death messages
				if (killer != null)
					players.getPlayer().sendMessage(
							Messager.colorFormat("&c%s &ekilled &c%s", killer.getName(), player.getName()));
				else if (p.getLastHit() != null)
					players.getPlayer().sendMessage(Messager.colorFormat("&c%s &edied while trying to escape &c%s",
							player.getName(), p.getLastHit().getName()));
				else
					players.getPlayer().sendMessage(Messager.colorFormat("&c%s &edied.", player.getName()));
			}

		this.removePlayer(p);

		p.getPlayer().teleport(SkywarsUtils.getCenteredLocation(this.getVectorInArena(this.getSpawn(p.teamNumber))));
		p.getPlayer().setVelocity(new Vector(0, 1f, 0));

		if (this.getWinner() != p)
			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					Skywars.get().NMS().sendTitle(player, "&c&lYOU DIED!", "&7You are now a spectator!", 0, 80, 0);
				}
			}, 20);
	}

	public void leavePlayer(Player player) {
		this.leavePlayer(this.getPlayer(player));
	}

	public void leavePlayer(SkywarsPlayer player) {
		if (player == null)
			return;
		player.getPlayer().sendMessage(
				Messager.getFormattedMessage("LEAVE_SELF", player.getPlayer(), this, player, this.map.getName()));
		if (!this.started())
			this.joinable = true;
		this.users.remove(player);
		this.removePlayer(player);
		if (this.getStatus() != ArenaStatus.ENDING && !player.isSpectator()) {
			for (final SkywarsPlayer players : this.getUsers()) {
				players.getPlayer().sendMessage(Messager.getFormattedMessage("LEAVE", player.getPlayer(), this, player,
						player.getPlayer().getName(), this.getUsers().size(), this.map.getMaxPlayers()));
				final String sound = Skywars.config.getString("sounds.leave");
				final String[] splitted = sound.split(";");
				player.getPlayer().playSound(player.getPlayer().getLocation(),
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
			for (final SkywarsPlayer players : this.getUsers()) {
				players.getPlayer().sendMessage(Messager.getMessage("COUNTDOWN_STOPPED", this.getAlivePlayerCount()));
			}
			this.cancelTimer();
		}
		if (this.status != ArenaStatus.WAITING && this.getAlivePlayerCount() <= 0)
			this.clear();
	}

	public void exitPlayer(Player player) {
		this.exitPlayer(this.getPlayer(player));
	}

	public void exitPlayer(SkywarsPlayer player) {
		if (player == null)
			return;
		SkywarsUtils.ClearPlayer(player.getPlayer());
		player.getSavedPlayer().Restore();
		if (this.isInBoundaries(player.getPlayer()))
			SkywarsUtils.TeleportPlayerBack(player.getPlayer());
	}

	void removePlayer(SkywarsPlayer player) {
		if (this.getStatus() == ArenaStatus.PLAYING) {
			if (this.getAlivePlayers().size() < 2) {
				final List<SkywarsPlayer> winners = new ArrayList<>(this.getAlivePlayers());
				if (winners.size() > 0) {
					this.setWinner(winners.get(0));
				}

				final double winMoney = Skywars.get().getConfig().getDouble("economy.win");

				for (final SkywarsPlayer p : this.getUsers()) {
					if (p == this.getWinner()) {
						if (Skywars.get().getEconomy() != null && winMoney > 0)
							Skywars.get().getEconomy().depositPlayer(p.getPlayer(), winMoney);
						Skywars.get().NMS().sendTitle(p.getPlayer(), "&6&lYOU WON", "&7Congratulations!", 0, 80, 0);
					} else {
						Skywars.get().NMS().sendTitle(p.getPlayer(), "&c&lGAME ENDED", "&7You didn't win this time.", 0,
								80, 0);
					}
					if (this.winner == null) {
						p.getPlayer().sendMessage(Messager.color("&cnobody &ewon"));
					} else {
						p.getPlayer()
								.sendMessage(Messager.colorFormat("&c%s &ewon!", this.winner.getPlayer().getName()));
					}
				}
				this.startTimer(ArenaStatus.ENDING);
			} else {
				for (final SkywarsPlayer p : this.getUsers()) {
					Skywars.get().NMS().sendActionbar(p.getPlayer(),
							String.format("&c%s &eplayers remaining", this.getAlivePlayerCount()));
				}
			}
		}
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

	public void startTimer(ArenaStatus status) {
		this.cancelTimer();
		this.setStatus(status);
		Skywars.get().sendDebugMessage(String.format("starting %s timer", status));
		if (status == ArenaStatus.STARTING) {
			if (this.forcedStart && this.forcedStartPlayer != null) {
				for (final SkywarsPlayer player : this.getUsers()) {
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

					for (final SkywarsPlayer player : Arena.this.getUsers()) {

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
		} else if (status == ArenaStatus.ENDING) {
			this.task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				int time = Skywars.get().getConfig().getInt("time.ending") + 1;
				int fireworks = 0;

				@Override
				public void run() {
					this.time--;
					Arena.this.countdown = this.time;

					final SkywarsPlayer swp = Arena.this.getWinner();
					if (this.fireworks <= Skywars.get().getConfig().getInt("endFireworks") && swp != null
							&& Arena.this.getPlayer(swp.getPlayer()) != null && !swp.isSpectator()) {
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
	}

	public void startGame() {
		if (this.getStatus() == ArenaStatus.PLAYING)
			return;
		this.cancelTimer();
		this.setStatus(ArenaStatus.PLAYING);
		this.startTimer(this.getStatus());
		this.calculateAndFillChests();
		for (final Vector spawn : this.map.getSpawns().values()) {
			Skywars.createCase(this.getVectorInArena(spawn), XMaterial.AIR);
		}
		for (final SkywarsPlayer player : this.getUsers()) {
			if (player.isSpectator())
				continue;
			SkywarsUtils.ClearPlayer(player.getPlayer());
			player.getPlayer().setGameMode(GameMode.SURVIVAL);
			final Kit kit = Skywars.get().getPlayerKit(player.getPlayer());
			if (kit != null) {
				for (final ItemStack item : kit.getItems()) {
					player.getPlayer().getInventory().addItem(item);
				}
			}
			player.getPlayer().sendMessage(Messager.color("&eCages opened! &cFIGHT!"));
			Skywars.get().NMS().sendTitle(player.getPlayer(), "&c&lINSANE MODE");
			player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.NOTE_PLING.bukkitSound(), 0.5f, 1f);
			player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.PORTAL_TRIGGER.bukkitSound(), 0.5f,
					5f);

			this.setInvencibility(true);

			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					for (final String l : Skywars.get().getConfig().getStringList("startLines")) {
						player.getPlayer().sendMessage(Messager.color(l));
					}
				}
			}, 20);

			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					final double playMoney = Skywars.get().getConfig().getDouble("economy.play");
					if (Skywars.get().getEconomy() != null && playMoney > 0)
						Skywars.get().getEconomy().depositPlayer(player.getPlayer(), playMoney);
				}
			}, 20 * 10);
		}

		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				Arena.this.setInvencibility(false);
			}
		}, 40);
	}

	public void clear() {
		this.clear(true);
	}

	public void clear(boolean remove) {
		this.cancelTimer();
		Skywars.get().sendMessage("Clearing arena for map " + this.map.getName());
		for (final SkywarsPlayer player : this.getUsers()) {
			this.exitPlayer(player);
		}
		this.forcedStart = false;
		this.forcedStartPlayer = null;
		this.setStatus(ArenaStatus.WAITING);
		this.countdown = -1;
		this.users.clear();
		if (this.world != null) {
			for (final Entity i : this.world.getEntities()) {
				if (i instanceof Item && this.isInBoundaries(i.getLocation())) {
					i.remove();
				}
			}
		}
		if (remove)
			Skywars.get().clearArena(this);
	}

	public void resetCases() {
		for (final Vector spawn : this.map.getSpawns().values()) {
			Skywars.createCase(this.getVectorInArena(spawn), XMaterial.RED_STAINED_GLASS);
		}
	}

	public ArrayList<String> getProblems() {
		final ArrayList<String> problems = new ArrayList<String>();
		if (this.world == null)
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
			if (this.getPlayer(i) == null)
				return i;
		}
		return -1;
	}

	public int getAlivePlayerCount() {
		return this.users.stream().filter(player -> !player.isSpectator()).collect(Collectors.toList()).size();
	}

	public ArrayList<SkywarsPlayer> getUsers() {
		return this.users;
	}

	public ArrayList<SkywarsPlayer> getAlivePlayers() {
		return this.users.stream().filter(player -> !player.isSpectator())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	ArrayList<SkywarsPlayer> getSpectators() {
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

	SkywarsPlayer getPlayer(int index) {
		for (final SkywarsPlayer swp : this.getUsers()) {
			if (swp.getNumber() == index)
				return swp;
		}
		return null;
	}

	public SkywarsPlayer getPlayer(String name) {
		for (final SkywarsPlayer swp : this.getUsers()) {
			if (swp.getPlayer().getName().equals(name))
				return swp;
		}
		return null;
	}

	public SkywarsPlayer getPlayer(Player player) {
		for (final SkywarsPlayer swp : this.getUsers()) {
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

	public void calculateAndFillChests() {
		final Schematic schematic = this.map.getSchematic();
		final World world = this.location.getWorld();
		final Vector offset = schematic.getOffset();
		final ListTag tileEntities = schematic.getTileEntities();
		// Skywars.get().sendDebugMessage("tile entities: " + tileEntities.getValue().size());

		final byte[] blocks = schematic.getBlocks();
		final short length = schematic.getLength();
		final short width = schematic.getWidth();
		final short height = schematic.getHeight();

		int filled = 0;

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {
					final int index = y * width * length + z * width + x;
					final Location loc = new Location(world, x, y, z).add(offset).add(this.location);
					final Block block = loc.getBlock();
					if (block.getType() == XMaterial.CHEST.parseMaterial() || blocks[index] == 54 /* chest id */) {
						ChestManager.fillChest(loc, SkywarsUtils.distance(this.getLocation().toVector(),
								loc.toVector()) < this.map.getCenterRadius());
						// Skywars.get().sendDebugMessage("filling chest at loc " + loc);
						filled++;
					}
				}
			}
		}

		for (final Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			final Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Chest")) {
				final Vector v = SchematicHandler.getVector(values);
				final Location loc = new Location(world, v.getX(), v.getY(), v.getZ()).add(offset).add(this.location);
				ChestManager.fillChest(loc, SkywarsUtils.distance(this.getLocation().toVector(),
						loc.toVector()) < this.map.getCenterRadius());
				// Skywars.get().sendDebugMessage("filling chest entity at loc " + loc);
				filled++;
			}
		}
		Skywars.get().sendDebugMessage("filled %s chests", filled);
	}

	public void softStart(Player player) {
		if (this.getStatus() == ArenaStatus.WAITING && this.getTask() == null) {
			this.forcedStart = true;
			this.forcedStartPlayer = player;
			this.startTimer(ArenaStatus.STARTING);
		} else {
			this.startGame();
		}
	}

	public void broadcastRefillMessage() {
		for (final SkywarsPlayer player : this.getUsers()) {
			player.getPlayer().sendMessage(Messager.color(Skywars.langConfig.getString("REFILL")));
			Skywars.get().NMS().sendTitle(player.getPlayer(), "",
					Messager.color(Skywars.langConfig.getString("REFILL")));
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

	public void setWorld(String worldName) {
		this.world = Bukkit.getWorld(worldName);
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public World getWorld() {
		return this.world;
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

	public SkywarsPlayer getWinner() {
		return this.winner;
	}

	public void setWinner(SkywarsPlayer winner) {
		this.winner = winner;
	}

}
