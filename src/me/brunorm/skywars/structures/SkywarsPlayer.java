package me.brunorm.skywars.structures;

import org.bukkit.entity.Player;

public class SkywarsPlayer {
	
	Player player;
	Arena arena;
	int kills = 0;
	boolean spectator = false;
	int number;
	SavedPlayer savedPlayer;
	
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

	SkywarsPlayer(Player player, Arena arena) {
		this.player = player;
		this.arena = arena;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	Arena getArena() {
		return arena;
	}
	
}
