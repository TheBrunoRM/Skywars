package me.brunorm.skywars.structures;

import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Skywars;

public class SkywarsPlayer {
	
	Player player;
	Arena arena;
	int kills = 0;
	boolean spectator = false;
	int teamNumber;
	SavedPlayer savedPlayer;
	Player lastHit;
	long lastHitTimestamp;
	
	SkywarsPlayer(Player player, Arena arena, int number) {
		this.player = player;
		this.arena = arena;
		this.teamNumber = number;
	}
	
	public Player getLastHit() {
		return lastHit;
	}
	
	public void setLastHit(Player lastHit) {
		this.lastHit = lastHit;
		if(lastHit == null) return;
		long timestamp = new Date().getTime();
		this.lastHitTimestamp = timestamp;
		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				if(timestamp == lastHitTimestamp) {
					setLastHit(null);
				}
			}
		}, Skywars.get().getConfig().getLong("lastHitResetCooldown")*20);
	}
	
	public SavedPlayer getSavedPlayer() {
		return savedPlayer;
	}

	public void setSavedPlayer(SavedPlayer savedPlayer) {
		this.savedPlayer = savedPlayer;
	}

	public int getKills() {
		return this.kills;
	}
	
	public void incrementKills() {
		this.kills++;
	}
	
	public boolean isSpectator() {
		return spectator;
	}

	public void setSpectator(boolean spectator) {
		this.spectator = spectator;
	}
	
	public int getNumber() {
		return teamNumber;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	Arena getArena() {
		return arena;
	}
	
}
