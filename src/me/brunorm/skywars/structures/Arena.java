package me.brunorm.skywars.structures;

import java.io.File;
import java.io.IOException;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jnbt.ListTag;
import org.jnbt.Tag;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.ChestManager;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.SavedPlayer;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import mrblobman.sounds.Sounds;

public class Arena {

	String name;
	File file;
	YamlConfiguration config;
	ArenaStatus status;
	String worldName;
	String schematicFilename;
	Schematic loadedSchematic;
	Location location;
	int minPlayers;
	int maxPlayers;
	int countdown;
	boolean full;
	boolean invencibility = false;
	public boolean forcedStart;
	public Player forcedStartPlayer;
	
	HashMap<Integer, Location> spawns = new HashMap<Integer, Location>();
	private ArrayList<SkywarsPlayer> players = new ArrayList<SkywarsPlayer>();
	
	public Arena(String name) {
		this.name = name;
		this.status = ArenaStatus.DISABLED;
		setFile(new File(Skywars.arenasPath, name + ".yml"));
	}

	public void setLocationConfig(String string, Location location) {
		System.out.println("setting location " + string);
		if (location == null) {
			config.set(string, null);
		} else {
			config.set(string + ".x", location.getBlockX());
			config.set(string + ".y", location.getBlockY());
			config.set(string + ".z", location.getBlockZ());
		}
	}
	
	public void saveParametersInConfig() {
		System.out.println("Setting config for arena " + this.name);
		setConfig(YamlConfiguration.loadConfiguration(getFile()));
		config.set("minPlayers", this.getMinPlayers());
		config.set("maxPlayers", this.getMaxPlayers());
		config.set("worldName", this.getWorldName());
		config.set("schematic", this.getSchematic());
		setLocationConfig("location", location);
		config.set("disabled", this.getStatus() == ArenaStatus.DISABLED ? true : false);
		if (this.getSpawns() != null) {
			for (int i = 0; i < this.getSpawns().size(); i++) {
				Location spawn = this.getSpawn(i);
				setLocationConfig("spawn." + i, spawn);
			}
		}
	}
	
	public void saveConfig() {
		if(getFile() == null) {			
			File arenaFile = new File(Skywars.arenasPath, String.format("%s.yml", this.getName()));
			if (!arenaFile.exists()) {
				try {
					arenaFile.createNewFile();
				} catch (IOException e) {
				}
			}
			setFile(arenaFile);
		}
		try {
			getConfig().save(file);
		} catch (IOException e) {
			System.out.println("couldn't save config file for arena " + this.name);
		}
	}
	
	public boolean joinPlayer(Player player) {
		if(!SkywarsUtils.JoinableCheck(this, player)) return false;
		if (status == ArenaStatus.PLAYING)
			return false;
		if (!checkProblems()) {
			for (String problem : getProblems()) {
				player.sendMessage(problem);
			}
			return false;
		}
		if (hasPlayer(player))
			return false;
		int index = getPlayers().size();
		if (index > maxPlayers)
			return false;
		else
			full = false;
		Location spawn = getLocationInArena(getSpawn(index));
		if (spawn == null) {
			player.sendMessage(String.format("spawn %s of arena %s not set", index, this.getName()));
			return false;
		} else {
			System.out.println("Player spawn: " + spawn);
		}
		SkywarsPlayer swPlayer = new SkywarsPlayer(player, this);
		YamlConfiguration playerConfig = Skywars.get().getPlayerConfig(player);
		String kitName = playerConfig.getString("kit");
		Kit kit = Skywars.get().getKit(kitName);
		swPlayer.setKit(kit);
		swPlayer.number = index;
		players.add(swPlayer);
		for (SkywarsPlayer players : this.getPlayers()) {
			players.getPlayer().sendMessage(Messager.colorFormat("&7%s &ehas joined (&b%s&e/&b%s&e)!", player.getName(),
					index + 1, this.getMaxPlayers()));
		}

		Skywars.createCase(spawn, XMaterial.LIME_STAINED_GLASS.parseMaterial(), XMaterial.LIME_STAINED_GLASS.getData());
		
		Location centered = new Location(spawn.getWorld(), spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
		centered.add(0.5,0,0.5);
		
		player.teleport(SkywarsUtils.getCenteredLocation(spawn));
		swPlayer.setSavedPlayer(new SavedPlayer(player));
		SkywarsUtils.ClearPlayer(player);
		SkywarsUtils.GiveBedItem(player);
		
		ItemStack bow = XMaterial.BOW.parseItem();
		ItemMeta meta = bow.getItemMeta();
		meta.setDisplayName(SkywarsUtils.parseItemName("select_kit"));
		bow.setItemMeta(meta);
		player.getInventory().setItem(0, bow);
		
		Skywars.get().NMS().sendTitle(player, "&eSkyWars", "&cInsane Mode");

		if (getStatus() != ArenaStatus.STARTING && this.getPlayers().size() >= this.getMinPlayers()) {
			startTimer(ArenaStatus.STARTING);
		}

		return true;
	}
	
	public void makeSpectator(SkywarsPlayer p) {
		if (p.isSpectator())
			return;
		
		p.setSpectator(true);
		Player player = p.getPlayer();
		
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
		player.getInventory().setArmorContents(null);
		
		SkywarsUtils.ClearPlayer(player);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setGameMode(GameMode.ADVENTURE);
		
		for (Player players : Bukkit.getOnlinePlayers()) {
			players.hidePlayer(player);
		}
		SkywarsUtils.GiveBedItem(player);

		for (SkywarsPlayer players : getPlayers()) {
			players.getPlayer().sendMessage(Messager.colorFormat("&c%s &edied", player.getName()));
		}

		removePlayer(p);

		p.getPlayer().teleport(SkywarsUtils.getCenteredLocation(getLocationInArena(getSpawn(p.number))));
		p.getPlayer().setVelocity(new Vector(0, 1f, 0));

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
		player.getPlayer().sendMessage(Messager.colorFormat("&eYou leaved arena %s", this.name));
		full = false;
		players.remove(player);
		removePlayer(player);
		if (this.getStatus() != ArenaStatus.ENDING) {
			for (SkywarsPlayer players : getPlayers()) {
				players.getPlayer().sendMessage(Messager.colorFormat("&7%s &eleaved (&b%s&e/&b%s&e)",
						player.getPlayer().getName(), getPlayers().size(), getMaxPlayers()));
			}
		}
		SkywarsUtils.ClearPlayer(player.getPlayer());
		player.getSavedPlayer().Restore();
		if(this.isInBoundaries(player.getPlayer()))
			SkywarsUtils.TeleportToLobby(player.getPlayer());
		if ((this.getStatus() == ArenaStatus.STARTING &&
				!forcedStart && getCurrentPlayers() < getMinPlayers())
				|| getCurrentPlayers() <= 0) {
			if(status == ArenaStatus.ENDING) stopGame();
			setStatus(ArenaStatus.WAITING);
			for (SkywarsPlayer players : getPlayers()) {
				players.getPlayer().sendMessage("&eCountdown stopped, not enough players!");
			}
			cancelTimer();
		}
	}
	
	public void kick(Player player) {
		kick(getPlayer(player));
	}
	
	public void kick(SkywarsPlayer player) {
		if(player == null) return;
		SkywarsUtils.ClearPlayer(player.getPlayer());
		player.getSavedPlayer().Restore();
		SkywarsUtils.TeleportToLobby(player.getPlayer());
	}

	void removePlayer(SkywarsPlayer player) {
		if (this.getStatus() == ArenaStatus.PLAYING) {
			if (getAlivePlayers().size() < 2) {
				List<SkywarsPlayer> winners = new ArrayList<>(this.getAlivePlayers());
				for (SkywarsPlayer players : getPlayers()) {
					SkywarsPlayer winner = null;
					if (winners.size() > 0) {
						winner = winners.get(0);
					}
					if (winners.size() <= 0 || winner == null) {
						players.getPlayer().sendMessage(Messager.color("&cnobody &ewon"));
					} else {
						players.getPlayer()
								.sendMessage(Messager.colorFormat("&c%s &ewon!", winner.getPlayer().getName()));
					}
				}
				startTimer(ArenaStatus.ENDING);
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
	}

	public void startTimer(ArenaStatus status) {
		setStatus(status);
		System.out.println(String.format("starting %s timer", status));
		if (status == ArenaStatus.STARTING) {
			if (forcedStart && forcedStartPlayer != null) {
				for (SkywarsPlayer player : getPlayers()) {
					player.getPlayer().sendMessage(
							Messager.colorFormat("&b&l%s &e&lforced the game to start!", forcedStartPlayer.getName()));
				}
			}
			task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				int time = 10 + 1;

				@Override
				public void run() {
					this.time--;
					countdown = time;
					
					if (this.time == 0) {
						startGame();
						cancelTimer();
						return;
					}

					for (SkywarsPlayer player : getPlayers()) {

						player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.NOTE_STICKS.bukkitSound(),
								5, 1f);
						if (time == 10) {
							if (this.time == 10) {
								Skywars.get().NMS().sendTitle(player.getPlayer(), Messager.color("&e10 seconds"),
										Messager.color("&eRight-click the bow to select a kit!"), 0, 50, 0);
							}
						} else if (time <= 5 || time % 5 == 0) {
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
		} else if (status == ArenaStatus.ENDING) {
			task = Bukkit.getScheduler().runTaskTimer(Skywars.get(), new Runnable() {
				int time = 60 + 1;

				@Override
				public void run() {
					this.time--;
					countdown = time;
					
					// todo: throw fireworks

					if (this.time == 0) {
						stopGame();
						cancelTimer();
						return;
					}
				}
			}, 0L, 20L);
		}
	}
	
	public void startGame() {
		cancelTimer();
		setStatus(ArenaStatus.PLAYING);
		calculateAndFillChests();
		for (Location spawn : getSpawns().values()) {
			Skywars.createCase(getLocationInArena(spawn), XMaterial.AIR.parseMaterial());
		}
		for (SkywarsPlayer player : getPlayers()) {
			if(player.isSpectator()) continue;
			SkywarsUtils.ClearPlayer(player.getPlayer());
			player.getPlayer().setGameMode(GameMode.SURVIVAL);
			Kit kit = player.getKit();
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
					setInvencibility(false);
				}
			}, 60);
		}
	}
	
	public boolean stopGame() {
		if (getStatus() != ArenaStatus.PLAYING && getStatus() != ArenaStatus.ENDING)
			return false;
		cancelTimer();
		for(SkywarsPlayer player : getPlayers()) {
			kick(player);
		}
		clear();
		setStatus(ArenaStatus.WAITING);
		return true;
	}

	public void clear() {
		System.out.println("Clearing arena " + this.name);
		players.clear();
		if(getWorldName() != null) {			
			for(Entity i : Bukkit.getWorld(getWorldName()).getEntities()) {
				if(i instanceof Item && isInBoundaries(i.getLocation())) {
					i.remove();
				}
			}
		}
		pasteSchematic();
		resetCases();
	}
	
	public void resetCases() {
		for (Location spawn : getSpawns().values()) {
			Skywars.createCase(getLocationInArena(spawn), XMaterial.RED_STAINED_GLASS.parseMaterial());
		}
	}
	
	public ArrayList<String> getProblems() {
		ArrayList<String> problems = new ArrayList<String>();
		if(!Bukkit.getServer().getWorlds().stream().map(world -> world.getName())
				.collect(Collectors.toList()).contains(getWorldName()))
			problems.add("World " + getWorldName() +" does not exist");
		if (getSpawn(getPlayers().size()) == null)
			problems.add(String.format("Spawn %s not set", getPlayers().size()));
		if (Skywars.get().getLobby() == null)
			problems.add("Main lobby not set");
		if (maxPlayers <= 0)
			problems.add("Max players not set");
		if (schematicFilename == null)
			problems.add("Schematic not set");
		if (location == null)
			problems.add("No location set");
		if (isFull())
			problems.add("Arena is full");
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
		if(loadedSchematic == null) loadSchematic();
		return loc.getX()>getLocation().getX()-loadedSchematic.getWidth()/2
		&& loc.getX()<getLocation().getX()+loadedSchematic.getWidth()/2
		&& loc.getY()>getLocation().getY()-loadedSchematic.getHeight()/2
		&& loc.getY()<getLocation().getY()+loadedSchematic.getHeight()/2
		&& loc.getZ()>getLocation().getZ()-loadedSchematic.getLength()/2
		&& loc.getZ()<getLocation().getZ()+loadedSchematic.getLength()/2;
	}
	
	public Location getSpawn(int index) {
		Location loc = spawns.get(index);
		if (loc == null || worldName == null)
			return null;
		loc.setWorld(Bukkit.getServer().getWorld(worldName));
		return loc;
	}

	public HashMap<Integer, Location> getSpawns() {
		return spawns;
	}
	
	public void setSpawn(int index, Location location) {
		this.spawns.put(index, location);
		setLocationConfig(String.format("spawn.%s", index), null);
	}

	public boolean removeSpawn(int index) {
		if (this.spawns.get(index) == null)
			return false;
		this.spawns.remove(index);
		setLocationConfig(String.format("spawn.%s", index), null);
		return true;
	}

	int getCurrentPlayers() {
		return this.players.size();
	}

	public ArrayList<SkywarsPlayer> getPlayers() {
		return this.players;
	}

	ArrayList<SkywarsPlayer> getAlivePlayers() {
		ArrayList<SkywarsPlayer> alivePlayers = new ArrayList<SkywarsPlayer>(getPlayers());
		alivePlayers.removeIf(p -> p.isSpectator());
		return alivePlayers;
	}

	ArrayList<SkywarsPlayer> getSpectators() {
		ArrayList<SkywarsPlayer> spectators = new ArrayList<SkywarsPlayer>(getPlayers());
		spectators.removeIf(p -> !p.isSpectator());
		return spectators;
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
		return players.get(index);
	}

	public SkywarsPlayer getPlayer(String name) {
		for (SkywarsPlayer players : getPlayers()) {
			if (players.getPlayer().getName().equals(name))
				return players;
		}
		return null;
	}

	public SkywarsPlayer getPlayer(Player player) {
		for (SkywarsPlayer players : getPlayers()) {
			if (players.getPlayer().getName().equals(player.getName()))
				return players;
		}
		return null;
	}

	public void pasteSchematic() {
		if (schematicFilename == null) {
			System.out.println("tried to paste schematic, but schematic is not set!");
			return;
		}
		if (location == null) {
			System.out.println("tried to paste schematic, but location is not set!");
			return;
		}
		System.out.println("Loading schematic for " + this.name);
		loadSchematic();
		System.out.println("Pasting schematic for " + this.name);
		SchematicHandler.pasteSchematic(location, loadedSchematic);
	}

	public void calculateSpawns() {
		//if(loadedSchematic == null)
			loadSchematic();
		
		// clear spawns
		spawns.clear();
		config.set("spawn", null);
		
		World world = location.getWorld();
		Vector offset = loadedSchematic.getOffset();
		ListTag tileEntities = loadedSchematic.getTileEntities();
		System.out.println("Calculating spawns for arena " + this.name);

		ArrayList<Location> beaconLocations = new ArrayList<Location>();

		for (Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Beacon")) {
				Location loc = calculatePositionWithOffset(values, world, offset)
					.add(new Vector(0,1,0));
					//.add(new Vector(0.5,1,0.5));
				System.out.println("beacon location: "
					+ loc.getBlockX() + ", "
					+ loc.getBlockY() + ", "
					+ loc.getBlockZ());
				beaconLocations.add(loc);
			}
		}
		
		System.out.println("calculating spawns for " + beaconLocations.size() + " beacons");
		
		// saving the number of beacons
		int totalBeacons = beaconLocations.size();
		
		// set the first spawn
		spawns.put(0, beaconLocations.get(0));
		beaconLocations.remove(0);
		
		for(int i = 1; i < totalBeacons+1; i++) {
			System.out.println("calculating spawn " + i);
			Location previousSpawn = spawns.get(i-1);
			Location closest = beaconLocations.get(0);
			if(beaconLocations.size() > 1) {
				for(Location currentSpawn : beaconLocations) {
					if(distance(previousSpawn, currentSpawn) < distance(previousSpawn, closest)) {
						closest = currentSpawn;
					}
				}
				System.out.println("distance from spawn " + (i-1) +
						" to spawn " + i + " is " + distance(previousSpawn, closest));
				beaconLocations.remove(closest);
			}
			System.out.println("spawn " + i + " set to "
			+ closest.getBlockX() + ", " + closest.getBlockY() + ", " + closest.getBlockZ());
			spawns.put(i, closest);
		}
		
		saveParametersInConfig();
		saveConfig();
	}

	Location calculateClosestLocation(Location loc, ArrayList<Location> locations) {
		if(locations.size() <= 1) return loc;
		Location closest = locations.get(0);
		for(Location l : locations) {
			if(distance(loc, l) < distance(loc, closest)) {
				closest = l;
			}
		}
		return closest;
	}

	public double distance(Location vec1, Location vec2) {
		//if(vec1 == null || vec2 == null) return -1;
		double dx = vec2.getX() - vec1.getX();
		double dy = vec2.getY() - vec1.getY();
		double dz = vec2.getZ() - vec1.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	Location calculatePositionWithOffset(Map<String, Tag> values, World world, Vector offset) {
		int x = (int) values.get("x").getValue();
		int y = (int) values.get("y").getValue();
		int z = (int) values.get("z").getValue();
		System.out.println("schematic values: " + x + ", " + y + ", " + z);
		return new Location(world,
			x + offset.getBlockX(),
			y + offset.getBlockY(),
			z + offset.getBlockZ());
	}

	void loadSchematic() {
		File schematicFile = Skywars.get().getSchematicFile(schematicFilename);
		if(schematicFile == null) {
			System.out.println("Could not get schematic file! (Maybe it doesnt exist?)");
			return;
		}
		try {
			loadedSchematic = SchematicHandler.loadSchematic(schematicFile);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not load schematic!");
		}
	}
	
	void calculateAndFillChests() {
		loadSchematic();
		World world = location.getWorld();
		Vector offset = loadedSchematic.getOffset();
		ListTag tileEntities = loadedSchematic.getTileEntities();
		System.out.println("Filling chests!");
		
		int filled = 0;
		
		for (Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Chest")) {
				Location loc = calculatePositionWithOffset(values, world, offset);
				ChestManager.fillChest(getLocationInArena(loc));
				filled++;
			}
		}
		
		System.out.println("Filled " + filled + " chests");
	}
	
	public String getName() {
		return name;
	}
	
	public int getMinPlayers() {
		return minPlayers;
	}

	public void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	
	public int getCountdown() {
		return countdown;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public YamlConfiguration getConfig() {
		return config;
	}

	public void setConfig(YamlConfiguration config) {
		this.config = config;
	}

	public String getWorldName() {
		return worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public ArenaStatus getStatus() {
		return status;
	}

	public void setStatus(ArenaStatus status) {
		this.status = status;
	}

	public String getSchematic() {
		return schematicFilename;
	}

	public void setSchematic(String schematic) {
		this.schematicFilename = schematic;
		// clear loaded schematic
		loadedSchematic = null;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public boolean isFull() {
		return full;
	}
	
	public boolean isInvencibility() {
		return invencibility;
	}

	public void setInvencibility(boolean invencibility) {
		this.invencibility = invencibility;
	}

}
