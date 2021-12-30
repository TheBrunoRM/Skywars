package me.brunorm.skywars.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
	boolean joinable;
	boolean invencibility = false;
	public boolean forcedStart;
	public Player forcedStartPlayer;
	SkywarsPlayer winner;
	
	SkywarsMap map;
	
	public SkywarsMap getMap() {
		return map;
	}
	
	private ArrayList<SkywarsPlayer> players = new ArrayList<SkywarsPlayer>();
	ArrayList<SkywarsEvent> events = new ArrayList<SkywarsEvent>();
	
	public Arena(SkywarsMap map) {
		this.map = map;
		this.status = ArenaStatus.WAITING;
		events.add(new SkywarsEvent(this, SkywarsEventType.REFILL, 60));
		joinable = true;
	}
	
	public SkywarsEvent getNextEvent() {
		if(events.size() > 0)
			return events.get(0);
		return null;
	}
	
	public boolean joinPlayer(Player player) {
		if(!SkywarsUtils.JoinableCheck(this, player)) return false;
		if (!checkProblems()) {
			for (String problem : getProblems()) {
				player.sendMessage(problem);
			}
			return false;
		}
		if (hasPlayer(player))
			return false;
		joinable = getPlayerCount() > map.maxPlayers;
		int index = getNextAvailablePlayerSlot();
		Location spawn = getLocationInArena(getSpawn(index));
		if (spawn == null) {
			player.sendMessage(String.format("spawn %s of arena %s not set", index, map.getName()));
			return false;
		}
		SkywarsPlayer swPlayer = new SkywarsPlayer(player, this, index);
		players.add(swPlayer);
		for (SkywarsPlayer players : this.getAllPlayersIncludingAliveAndSpectators()) {
			players.getPlayer().sendMessage(Messager.colorFormat("&7%s &ehas joined (&b%s&e/&b%s&e)!", player.getName(),
					getPlayerCount(), map.getMaxPlayers()));
		}

		if(getTask() != null && getStatus() == ArenaStatus.STARTING)
			player.sendMessage(Messager.colorFormat("&eThe game is starting in &6%s &eseconds!", getCountdown()));
		
		Skywars.createCase(spawn, XMaterial.LIME_STAINED_GLASS);
		
		Skywars.get().playerLocations.put(player, player.getLocation());
		player.teleport(SkywarsUtils.getCenteredLocation(spawn));
		swPlayer.setSavedPlayer(new SavedPlayer(player));
		SkywarsUtils.ClearPlayer(player);
		
		SkywarsUtils.setPlayerInventory(player, "waiting");
		
		Skywars.get().NMS().sendTitle(player,
				Skywars.get().langConfig.getString("arena_join.title"),
				Skywars.get().langConfig.getString("arena_join.subtitle"));

		if (getStatus() != ArenaStatus.STARTING
				&& this.getAllPlayersIncludingAliveAndSpectators().size() >= map.getMinPlayers()) {
			startTimer(ArenaStatus.STARTING);
		}

		return true;
	}
	
	private Location getSpawn(Object key) {
		return map.spawns.get(key);
	}

	public void makeSpectator(SkywarsPlayer p, Player killer) {
		if (p.isSpectator())
			return;
		
		p.setSpectator(true);
		Player player = p.getPlayer();
		
		if(killer != null) {
			SkywarsPlayer killerPlayer = getPlayer(killer);
			if(killerPlayer != null) {
				Skywars.get().incrementPlayerTotalKills(killer);
				double killMoney = Skywars.get().getConfig().getDouble("economy.kill");
				if(Skywars.get().getEconomy() != null && killMoney > 0) {
					Skywars.get().getEconomy().depositPlayer(killer, killMoney);
					killer.sendMessage(Messager.colorFormat("&6+$%s",
							SkywarsUtils.formatDouble(killMoney)));
				}
			}
			Skywars.get().incrementPlayerTotalDeaths(player);
		}
		
		// only drop items if the player is inside the arena
		// if the player dies for void, it will not drop items
		if(isInBoundaries(player)) {
			// drop player inventory
			for (ItemStack i : player.getInventory().getContents()) {
				if (i != null) {
					player.getWorld().dropItemNaturally(player.getLocation(), i);
					player.getInventory().remove(i);
				}
			}
			// drop player armor
			for (ItemStack i : player.getInventory().getArmorContents()) {
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
		
		for (Player players : Bukkit.getOnlinePlayers()) {
			players.hidePlayer(player);
		}
		
		SkywarsUtils.setPlayerInventory(player, "spectator");

		if(getStatus() == ArenaStatus.PLAYING)
			for (SkywarsPlayer players : getAllPlayersIncludingAliveAndSpectators()) {
				// TODO: add more death messages
				if(killer != null)
					players.getPlayer().sendMessage(
							Messager.colorFormat("&c%s &ekilled &c%s",
							killer.getName(), player.getName()));
				else if (p.getLastHit() != null)
					players.getPlayer().sendMessage(
							Messager.colorFormat("&c%s &edied while trying to escape &c%s",
									player.getName(), p.getLastHit().getName()));
				else players.getPlayer().sendMessage(
						Messager.colorFormat("&c%s &edied.",
								player.getName()));
			}

		removePlayer(p);

		p.getPlayer().teleport(SkywarsUtils.getCenteredLocation(getLocationInArena(getSpawn(p.teamNumber))));
		p.getPlayer().setVelocity(new Vector(0, 1f, 0));

		if(getWinner() != p)
			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					Skywars.get().NMS().sendTitle(player, "&c&lYOU DIED!", "&7You are now a spectator!", 0, 80, 0);
				}
			}, 20);
	}

	public void leavePlayer(Player player) {
		leavePlayer(getPlayer(player));
	}

	public void leavePlayer(SkywarsPlayer player) {
		if(player == null) return;
		player.getPlayer().sendMessage(
				Messager.getFormattedMessage("LEAVE_SELF",
						player.getPlayer(), this, player, map.getName()));
		if(getStatus() == ArenaStatus.WAITING
				|| getStatus() == ArenaStatus.STARTING)
			joinable = true;
		players.remove(player);
		removePlayer(player);
		if (this.getStatus() != ArenaStatus.ENDING && !player.isSpectator()) {
			for (SkywarsPlayer players : getAllPlayersIncludingAliveAndSpectators()) {
				players.getPlayer().sendMessage(Messager.getFormattedMessage("LEAVE",
						player.getPlayer(), this, player,
						player.getPlayer().getName(),
						getAllPlayersIncludingAliveAndSpectators().size(), map.getMaxPlayers()));
			}
		}
		SkywarsUtils.ClearPlayer(player.getPlayer());
		player.getSavedPlayer().Restore();
		if(this.isInBoundaries(player.getPlayer()))
			SkywarsUtils.TeleportPlayerBack(player.getPlayer());
		
		if (this.getStatus() == ArenaStatus.STARTING
				&& !forcedStart
				&& (map.getMinPlayers() <= 0 || getPlayerCount() < map.getMinPlayers())) {
			//System.out.println("stopping start cooldown");
			setStatus(ArenaStatus.WAITING);
			for (SkywarsPlayer players : getAllPlayersIncludingAliveAndSpectators()) {
				players.getPlayer().sendMessage(Messager.getMessage("COUNTDOWN_STOPPED", getPlayerCount()));
			}
			cancelTimer();
		}
		if(status != ArenaStatus.WAITING && getPlayerCount() <= 0) restart();
	}
	
	public void kick(Player player) {
		kick(getPlayer(player));
	}
	
	public void kick(SkywarsPlayer player) {
		if(player == null) return;
		SkywarsUtils.ClearPlayer(player.getPlayer());
		player.getSavedPlayer().Restore();
		SkywarsUtils.TeleportPlayerBack(player.getPlayer());
	}

	void removePlayer(SkywarsPlayer player) {
		if (this.getStatus() == ArenaStatus.PLAYING) {
			if (getAlivePlayers().size() < 2) {
				List<SkywarsPlayer> winners = new ArrayList<>(this.getAlivePlayers());
				if (winners.size() > 0) {
					setWinner(winners.get(0));
				}
				
				double winMoney = Skywars.get().getConfig().getDouble("economy.win");
				
				for (SkywarsPlayer p : getAllPlayersIncludingAliveAndSpectators()) {
					if (p == getWinner()) {
						if(Skywars.get().getEconomy() != null && winMoney > 0)
							Skywars.get().getEconomy().depositPlayer(p.getPlayer(), winMoney);
						Skywars.get().NMS().sendTitle(p.getPlayer(),
								"&6&lYOU WON", "&7Congratulations!", 0, 80, 0);
					} else {
						Skywars.get().NMS().sendTitle(p.getPlayer(),
								"&c&lGAME ENDED", "&7You didn't win this time.", 0, 80, 0);
					}
					if (winner == null) {
						p.getPlayer().sendMessage(Messager.color("&cnobody &ewon"));
					} else {
						p.getPlayer()
								.sendMessage(Messager.colorFormat("&c%s &ewon!", winner.getPlayer().getName()));
					}
				}
				startTimer(ArenaStatus.ENDING);
			} else {
				for (SkywarsPlayer p : getAllPlayersIncludingAliveAndSpectators()) {
					Skywars.get().NMS().sendActionbar(p.getPlayer(),
							String.format("&c%s &eplayers remaining", getPlayerCount()));
				}
			}
		}
	}

	BukkitTask task;
	
	public BukkitTask getTask() {
		return task;
	}
	
	void cancelTimer() {
		if (getTask() == null)
			return;
		getTask().cancel();
		task = null;
	}

	public void startTimer(ArenaStatus status) {
		cancelTimer();
		setStatus(status);
		//System.out.println(String.format("starting %s timer", status));
		if (status == ArenaStatus.STARTING) {
			if (forcedStart && forcedStartPlayer != null) {
				for (SkywarsPlayer player : getAllPlayersIncludingAliveAndSpectators()) {
					player.getPlayer().sendMessage(
							Messager.getMessage("FORCED_START", forcedStartPlayer.getName()));
				}
			}
			task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				int time = Skywars.get().getConfig().getInt("time.starting") + 1;

				@Override
				public void run() {
					this.time--;
					countdown = time;
					
					if (this.time == 0) {
						startGame();
						cancelTimer();
						return;
					}

					for (SkywarsPlayer player : getAllPlayersIncludingAliveAndSpectators()) {

						player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.NOTE_STICKS.bukkitSound(),
								5, 1f);
						if (time == 10) {
							if (this.time == 10) {
								Skywars.get().NMS().sendTitle(player.getPlayer(), Messager.getMessage("10_SECONDS_TITLE"),
										Messager.getMessage("10_SECONDS_SUBTITLE"), 0, 50, 0);
							}
						} else if (time <= 5 || time % 5 == 0) {
							// TODO: add this into the config
							player.getPlayer().sendMessage(
									Messager.colorFormat("&eThe game starts in &c%s &esecond(s)!", this.time));
							Skywars.get().NMS().sendTitle(
									player.getPlayer(),
									Messager.color("&c" + this.time),
									Messager.color("&ePrepare to fight!"),
									0, 50, 0);
						}
					}
				}
			}, 0L, 20L);
		} else if (status == ArenaStatus.PLAYING) {
			task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					
					SkywarsEvent event = getNextEvent();
					
					if(event != null) {
						System.out.println("decreasing event time");
						event.decreaseTime();
						if(event.getTime() <= 0) {
							events.remove(event);
							event.run();							
						}
					} else {
						System.out.println("event is null");
					}
				}
			}, 0L, 20L);
		} else if (status == ArenaStatus.ENDING) {
			task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				int time = Skywars.get().getConfig().getInt("time.ending") + 1;
				int fireworks = 0;
				
				@Override
				public void run() {
					this.time--;
					countdown = time;
					
					SkywarsPlayer swp = getWinner();
					if(fireworks <= Skywars.get().getConfig().getInt("endFireworks")
							&& swp != null
							&& getPlayer(swp.getPlayer()) != null
							&& !swp.isSpectator()) {
						Player p = swp.getPlayer();
						SkywarsUtils.spawnRandomFirework(p.getLocation());
						fireworks++;
					}
					
					if (this.time == 0) {
						restart();
						cancelTimer();
						return;
					}
				}
			}, 0L, 20L);
		}
	}
	
	public void startGame() {
		if(getStatus() == ArenaStatus.PLAYING) return;
		cancelTimer();
		setStatus(ArenaStatus.PLAYING);
		startTimer(getStatus());
		calculateAndFillChests();
		for (Location spawn : map.getSpawns().values()) {
			Skywars.createCase(getLocationInArena(spawn), XMaterial.AIR);
		}
		for (SkywarsPlayer player : getAllPlayersIncludingAliveAndSpectators()) {
			if(player.isSpectator()) continue;
			SkywarsUtils.ClearPlayer(player.getPlayer());
			player.getPlayer().setGameMode(GameMode.SURVIVAL);
			Kit kit = Skywars.get().getPlayerKit(player.getPlayer());
			if(kit != null) {
				for(ItemStack item : kit.getItems()) {
					player.getPlayer().getInventory().addItem(item);
				}
			}
			player.getPlayer().sendMessage(Messager.color("&eCages opened! &cFIGHT!"));
			Skywars.get().NMS().sendTitle(player.getPlayer(), "&c&lINSANE MODE");
			player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.NOTE_PLING.bukkitSound(), 0.5f, 1f);
			player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.PORTAL_TRIGGER.bukkitSound(), 0.5f,
					5f);
			
			setInvencibility(true);
			
			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					for(String l : Skywars.get().getConfig().getStringList("startLines")) {
						player.getPlayer().sendMessage(Messager.color(l));
					}
				}
			}, 20);
			
			Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
				@Override
				public void run() {
					double playMoney = Skywars.get().getConfig().getDouble("economy.play");
					if(Skywars.get().getEconomy() != null && playMoney > 0)
						Skywars.get().getEconomy().depositPlayer(player.getPlayer(), playMoney);
				}
			}, 20*10);
		}
		
		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				setInvencibility(false);
			}
		}, 40);
	}
	
	public boolean restart() {
		cancelTimer();
		Skywars.get().sendMessage("Restarting arena for map " + map.getName());
		for(SkywarsPlayer player : getAllPlayersIncludingAliveAndSpectators()) {
			kick(player);
		}
		if(world != null) {			
			for(Entity i : world.getEntities()) {
				if(i instanceof Item && isInBoundaries(i.getLocation())) {
					i.remove();
				}
			}
		}
		Skywars.get().clearArena(this);
		return true;
	}
	
	public void resetCases() {
		for (Location spawn : map.getSpawns().values()) {
			Skywars.createCase(getLocationInArena(spawn), XMaterial.RED_STAINED_GLASS);
		}
	}
	
	public ArrayList<String> getProblems() {
		ArrayList<String> problems = new ArrayList<String>();
		if(world == null)
			problems.add("World not set");
		if (getSpawn(getPlayerCount()) == null)
			problems.add(String.format("Spawn %s not set", getPlayerCount()));
		if (map.maxPlayers <= 0)
			problems.add("Max players not set");
		if (map.schematicFilename == null)
			problems.add("Schematic not set");
		if (location == null)
			problems.add("No location set");
		if (!isJoinable())
			problems.add("Arena is not joinable");
		return problems;
	}
	
	boolean checkProblems() {
		return getProblems().size() <= 0;
	}
	
	Location getLocationInArena(Location loc) {
		return new Location(
			loc.getWorld(),
			loc.getBlockX() + this.location.getBlockX(),
			loc.getBlockY() + this.location.getBlockY(),
			loc.getBlockZ() + this.location.getBlockZ()
		);
	}
	
	public void goBackToCenter(Player player) {
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setVelocity(new Vector(0,0,0));
		player.teleport(this.getLocation());
		player.setVelocity(new Vector(0, 5f, 0));
	}
	
	public boolean isInBoundaries(Player player) {
		return isInBoundaries(player.getLocation());
	}
	
	public boolean isInBoundaries(Location loc) {
		Schematic schematic = map.getSchematic();
		return loc.getX()>getLocation().getX()-schematic.getWidth()/2
		&& loc.getX()<getLocation().getX()+schematic.getWidth()/2
		&& loc.getY()>getLocation().getY()-schematic.getHeight()/2
		&& loc.getY()<getLocation().getY()+schematic.getHeight()/2
		&& loc.getZ()>getLocation().getZ()-schematic.getLength()/2
		&& loc.getZ()<getLocation().getZ()+schematic.getLength()/2;
	}
	
	private int getNextAvailablePlayerSlot() {
		for(int i = 0; i < map.getMaxPlayers(); i++) {
			if(getPlayer(i) == null) return i;
		}
		return -1;
	}
	
	public int getPlayerCount() {
		return this.players.stream()
				.filter(player -> !player.isSpectator())
				.collect(Collectors.toList()).size();
	}

	public ArrayList<SkywarsPlayer> getAllPlayersIncludingAliveAndSpectators() {
		return this.players;
	}

	public ArrayList<SkywarsPlayer> getAlivePlayers() {
		return this.players.stream()
				.filter(player -> !player.isSpectator())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	ArrayList<SkywarsPlayer> getSpectators() {
		return this.players.stream()
				.filter(player -> player.isSpectator())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public boolean hasPlayer(Player player) {
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).getPlayer().getName().equals(player.getName())) {
				return true;
			}
		}
		return false;
	}

	SkywarsPlayer getPlayer(int index) {
		for (SkywarsPlayer swp : getAllPlayersIncludingAliveAndSpectators()) {
			if(swp.getNumber() == index) return swp;
		}
		return null;
	}

	public SkywarsPlayer getPlayer(String name) {
		for (SkywarsPlayer swp : getAllPlayersIncludingAliveAndSpectators()) {
			if (swp.getPlayer().getName().equals(name))
				return swp;
		}
		return null;
	}

	public SkywarsPlayer getPlayer(Player player) {
		for (SkywarsPlayer swp : getAllPlayersIncludingAliveAndSpectators()) {
			if (swp.getPlayer().getName().equals(player.getName()))
				return swp;
		}
		return null;
	}
	
	public void pasteSchematic() {
		System.out.println("pasting schematic at " + location.toString());
		SchematicHandler.pasteSchematic(location, map.getSchematic());
	}
	
	void calculateAndFillChests() {
		Schematic schematic = map.getSchematic();
		World world = location.getWorld();
		Vector offset = schematic.getOffset();
		ListTag tileEntities = schematic.getTileEntities();
		
		for (Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Chest")) {
				Location loc = SchematicHandler.calculatePositionWithOffset(values, world, offset);
				ChestManager.fillChest(getLocationInArena(loc),
						SkywarsUtils.distance(this.getLocation(),
								getLocationInArena(loc)) < map.getCenterRadius());
			}
		}
	}
	
	public int getCountdown() {
		return countdown;
	}
	
	public ArenaStatus getStatus() {
		return status;
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
		return world;
	}
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public boolean isJoinable() {
		return joinable;
	}
	
	public boolean isInvencibility() {
		return invencibility;
	}

	public void setInvencibility(boolean invencibility) {
		this.invencibility = invencibility;
	}
	
	public SkywarsPlayer getWinner() {
		return winner;
	}

	public void setWinner(SkywarsPlayer winner) {
		this.winner = winner;
	}

	public void softStart(Player player) {
		if(this.getStatus() == ArenaStatus.WAITING &&
				this.getTask() == null) {
			forcedStart = true;
			forcedStartPlayer = player;
			startTimer(ArenaStatus.STARTING);
		} else {
			startGame();
		}
	}
	
}
