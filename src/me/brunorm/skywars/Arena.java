package me.brunorm.skywars;

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
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jnbt.ListTag;
import org.jnbt.Tag;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.schematics.Schematic;
import me.brunorm.skywars.schematics.SchematicHandler;
import mrblobman.sounds.Sounds;

public class Arena {

	String name;
	int minPlayers;
	int maxPlayers;
	File file;
	YamlConfiguration config;
	ArenaStatus status;
	String worldName;
	String schematic;
	Schematic loadedSchematic;
	Location location;
	boolean full;
	int countdown;
	boolean forcedStart;
	Player forcedStartPlayer;
	boolean autoDetectSpawns = false;
	List<Item> droppedItems = new ArrayList<Item>();

	HashMap<Location, Boolean> chests = new HashMap<Location, Boolean>();
	HashMap<Integer, Location> spawns = new HashMap<Integer, Location>();
	private ArrayList<SkywarsPlayer> players = new ArrayList<SkywarsPlayer>();
	// ArrayList<SkywarsPlayer> spectators = new ArrayList<SkywarsPlayer>();

	Arena(String name) {
		this.name = name;
		this.status = ArenaStatus.DISABLED;
		// this.minPlayers = 2;
	}

	public void saveLocationConfig(String string, Location location) {
		// System.out.println("saving location " + string);
		if (location == null) {
			System.out.println("setting " + string + " to null");
			config.set(string, null);
		} else {
			System.out.println("saving " + string);
			config.set(string + ".x", location.getX());
			config.set(string + ".y", location.getY());
			config.set(string + ".z", location.getZ());
		}
		try {
			config.save(getFile());
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}

	public void saveConfig() {
		File arenaFile = new File(Skywars.arenasPath, String.format("%s.yml", this.getName()));
		if (!arenaFile.exists()) {
			try {
				arenaFile.createNewFile();
				// System.out.println("created new file for arena " + getName());
			} catch (IOException e) {
				// System.out.println("couldn't create file");
			}
		}
		// System.out.println("saving arena " + getName());
		setFile(arenaFile);
		config = YamlConfiguration.loadConfiguration(arenaFile);
		config.set("minPlayers", this.getMinPlayers());
		config.set("maxPlayers", this.getMaxPlayers());
		config.set("worldName", this.getWorldName());
		config.set("schematic", this.getSchematic());
		saveLocationConfig("location", location);
		config.set("disabled", this.getStatus() == ArenaStatus.DISABLED ? true : false);
		config.set("autoDetectSpawns", autoDetectSpawns);
		if (this.getSpawns() != null) {
			for (int i = 0; i < this.getSpawns().size(); i++) {
				Location spawn = this.getSpawn(i);
				// System.out.println(String.format("saving spawn %s of arena %s", i,
				// getName()));
				saveLocationConfig("spawn." + i, spawn);
			}
		}
		try {
			config.save(arenaFile);
		} catch (IOException e) {
			// System.out.println("couldn't save file");
		}
	}

	boolean JoinPlayer(Player player) {
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
		Location spawn = getSpawn(index);
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
		player.teleport(spawn);
		SkywarsUtils.ClearPlayer(player);
		SkywarsUtils.GiveBedItem(player);
		
		ItemStack bow = XMaterial.BOW.parseItem();
		ItemMeta meta = bow.getItemMeta();
		meta.setDisplayName(SkywarsUtils.parseItemName("select_kit"));
		bow.setItemMeta(meta);
		player.getInventory().setItem(0, bow);
		
		Skywars.get().NMS().sendTitle(player, "&eSkyWars", "&cInsane Mode");

		if (getStatus() != ArenaStatus.STARTING && this.getPlayers().size() >= this.getMinPlayers()) {
			StartTimer(ArenaStatus.STARTING);
		}

		return true;
	}

	void MakeSpectator(SkywarsPlayer p) {
		if (p.isSpectator())
			return;
		
		// players.remove(p);
		// spectators.add(p);
		p.setSpectator(true);
		Player player = p.getPlayer();
		
		// drop player inventory
		for (ItemStack i : player.getInventory().getContents()) {
			if (i != null) {
				droppedItems.add(player.getWorld().dropItemNaturally(player.getLocation(), i));
				player.getInventory().remove(i);
			}
		}
		// drop player armor
		for (ItemStack i : player.getInventory().getArmorContents()) {
			if (i != null && i.getType() != Material.AIR) {
				droppedItems.add(player.getWorld().dropItemNaturally(player.getLocation(), i));
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

		p.getPlayer().teleport(this.getSpawn(p.number));
		p.getPlayer().setVelocity(new Vector(0, 1f, 0));

		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				Skywars.get().NMS().sendTitle(player, "&c&lYOU DIED!", "&7You are now a spectator!", 0, 80, 0);
			}
		}, 20);
	}

	void LeavePlayer(Player player) {
		SkywarsPlayer p = getPlayer(player);
		if (p == null)
			return;
		LeavePlayer(p);
	}

	void LeavePlayer(SkywarsPlayer player) {
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
		SkywarsUtils.TeleportToLobby(player.getPlayer());
		if (this.getStatus() == ArenaStatus.STARTING &&
				!forcedStart && getCurrentPlayers() < getMinPlayers()) {
			setStatus(ArenaStatus.WAITING);
			for (SkywarsPlayer players : getPlayers()) {
				players.getPlayer().sendMessage("countdown stopped, not enough players");
			}
			cancelTimer();
		}
		if (getCurrentPlayers() <= 0) {
			System.out.println("stopping game because there are no more players");
			StopGame();
		}
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
						players.getPlayer().sendMessage(Messager.color("&cnobody &ewon!"));
					} else {
						players.getPlayer()
								.sendMessage(Messager.colorFormat("&c%s &ewon!", winner.getPlayer().getName()));
					}
				}
				StartTimer(ArenaStatus.ENDING);
			}
		}
	}

	BukkitTask task;

	void cancelTimer() {
		if (task == null)
			return;
		task.cancel();
	}

	void StartTimer(ArenaStatus status) {
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
						StartGame();
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
						StopGame();
						cancelTimer();
					}
				}
			}, 0L, 20L);
		}
	}

	int getCountdown() {
		return countdown;
	}

	void StartGame() {
		cancelTimer();
		setStatus(ArenaStatus.PLAYING);
		calculateAndFillChests();
		for (Location spawn : getSpawns().values()) {
			Skywars.createCase(spawn, XMaterial.AIR.parseMaterial());
		}
		for (SkywarsPlayer player : getPlayers()) {
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
		}
	}
	
	boolean StopGame() {
		if (!(getStatus() == ArenaStatus.PLAYING || getStatus() == ArenaStatus.ENDING))
			return false;
		Restart();
		return true;
	}

	void Restart() {
		cancelTimer();
		System.out.println("now stopping the game");
		for(int i = 0; i < getPlayers().size(); i++) {
			LeavePlayer(getPlayer(0));
		}
		for(Item i : droppedItems) {
			i.remove();
		}
		droppedItems.clear();
		chests.clear();
		PasteSchematic();
		setStatus(ArenaStatus.WAITING);
	}

	String getName() {
		return name;
	}

	boolean checkProblems() {
		return getProblems().size() <= 0;
	}

	ArrayList<String> getProblems() {
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
		if (schematic == null)
			problems.add("Schematic not set");
		if (location == null)
			problems.add("No location set");
		if (isFull())
			problems.add("Arena is full");
		return problems;
	}

	int getSpawn(Location location) {
		int i = 0;
		for (Location spawn : getSpawns().values()) {
			if ((int) spawn.getX() == location.getX() && spawn.getY() == location.getY()
					&& spawn.getZ() == location.getZ()) {
				return i;
			}
			i++;
		}
		return -1;
	}

	Location getSpawn(int index) {
		Location loc = spawns.get(index);
		if (loc == null || worldName == null)
			return null;
		loc.setWorld(Bukkit.getServer().getWorld(worldName));
		return loc;
	}

	public HashMap<Integer, Location> getSpawns() {
		return spawns;
	}
	
	Location setSpawn(int index, Location location) {
		if (location == null)
			return null;
		location.setX(Math.floor(location.getX()) + 0.5f);
		location.setY(Math.floor(location.getY()));
		location.setZ(Math.floor(location.getZ()) + 0.5f);
		this.spawns.put(index, location);
		saveLocationConfig(String.format("spawn.%s", index), location);
		return location;
	}

	boolean removeSpawn(int index) {
		if (this.spawns.get(index) == null)
			return false;
		this.spawns.remove(index);
		saveLocationConfig(String.format("spawn.%s", index), null);
		return true;
	}

	int getCurrentPlayers() {
		return this.players.size();
	}

	ArrayList<SkywarsPlayer> getPlayers() {
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

	SkywarsPlayer getPlayer(String name) {
		for (SkywarsPlayer players : getPlayers()) {
			if (players.getPlayer().getName().equals(name))
				return players;
		}
		return null;
	}

	SkywarsPlayer getPlayer(Player player) {
		for (SkywarsPlayer players : getPlayers()) {
			if (players.getPlayer().getName().equals(player.getName()))
				return players;
		}
		return null;
	}

	public void PasteSchematic() {
		if (schematic == null) {
			System.out.println("tried to paste schematic, but schematic is not set!");
			return;
		}
		if (location == null) {
			System.out.println("tried to paste schematic, but location is not set!");
			return;
		}
		loadSchematic();
		SchematicHandler.pasteSchematic(location, loadedSchematic);

		if (autoDetectSpawns) {
			CalculateSpawns();
		}
	}

	public void CalculateSpawns() {
		loadSchematic();
		for (int i = 0; i < getSpawns().values().size(); i++) {
			removeSpawn(0);
		}
		spawns.clear();
		World world = location.getWorld();
		Vector offset = loadedSchematic.getOffset();
		ListTag tileEntities = loadedSchematic.getTileEntities();
		System.out.println("Auto detecting spawns for arena " + this.name);

		ArrayList<Location> remainingSpawns = new ArrayList<Location>();

		for (Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Beacon")) {
				Location loc = calculatePositionWithOffset(values, world, offset)
					.add(new Vector(0,1,0));
				remainingSpawns.add(loc);
				// saveSpawn(getSpawns().size(), loc);
			}
		}
		
		System.out.println("calculating " + remainingSpawns.size() + " spawns");
		
		int total = remainingSpawns.size();
		
		// set the first spawn
		spawns.put(0, remainingSpawns.get(0));
		remainingSpawns.remove(0);
		
		for(int i = 1; i < total; i++) {
			System.out.println("calculating spawn " + i);
			Location previousSpawn = spawns.get(i-1);
			if(remainingSpawns.size() <= 1) break;
			Location closest = remainingSpawns.get(0);
			for(Location currentSpawn : remainingSpawns) {
				if(distance(previousSpawn, currentSpawn) < distance(previousSpawn, closest)) {
					closest = currentSpawn;
				}
			}
			remainingSpawns.remove(closest);
			spawns.put(i, closest.add(new Vector(0, 0.5, 0)));
		}
		
		for(int i = 0; i < spawns.size(); i++) {
			Location spawn = spawns.get(i);
			Location next = spawns.get(i+1);
			if(next == null) continue;
			System.out.println("distance from spawn " + i +
					" to spawn " + (i + 1) + " is " + distance(spawn, next));
		}
		
	}

	Location calculateClosestSpawn(Location loc, ArrayList<Location> spawns) {
		if(spawns.size() <= 1) return loc;
		Location closest = spawns.get(1);
		for(Location spawn : spawns) {
			if(distance(loc, spawn) <= 0) continue;
			if(distance(loc, spawn) < distance(loc, closest)) {
				System.out.println(distance(loc, spawn) + "<" + distance(loc, closest));
				closest = spawn;
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
		return new Location(world, x + this.location.getX() + offset.getX(),
			y + this.location.getY() + offset.getY(),
			z + this.location.getZ() + offset.getZ());
	}

	void loadSchematic() {
		try {
			loadedSchematic = SchematicHandler.loadSchematic(Skywars.get().getSchematicFile(schematic));
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
		System.out.println("Auto detecting spawns!");
		
		for (Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Chest")) {
				Location loc = calculatePositionWithOffset(values, world, offset);
				ChestManager.FillChest(loc);
			}
		}
	}

	public HashMap<Location, Boolean> getChests() {
		return chests;
	}

	public boolean getChest(Location location) {
		return chests.get(location) ? true : false;
	}

	public void setChest(Location location, boolean opened) {
		chests.put(location, opened);
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
		return schematic;
	}

	public void setSchematic(String schematic) {
		this.schematic = schematic;
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

}
