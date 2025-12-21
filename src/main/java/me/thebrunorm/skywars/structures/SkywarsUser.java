/* (C) 2021 Bruno */
package me.thebrunorm.skywars.structures;

import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.thebrunorm.skywars.Skywars;

public class SkywarsUser {

	Player player;
	Arena arena;
	int kills = 0;
	boolean spectator = false;
	int teamNumber;
	SavedPlayer savedPlayer;
	Player lastHit;
	long lastHitTimestamp;
	SkywarsTeam team;

	SkywarsUser(Player player, SkywarsTeam team, int number) {
		this.player = player;
		this.team = team;
		team.addUser(this);
		this.arena = team.getArena();
		this.teamNumber = number;
	}

	public Player getLastHit() {
		return this.lastHit;
	}

	public void setLastHit(Player lastHit) {
		this.lastHit = lastHit;
		if (lastHit == null)
			return;
		final long timestamp = new Date().getTime();
		this.lastHitTimestamp = timestamp;
		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				if (timestamp == SkywarsUser.this.lastHitTimestamp) {
					SkywarsUser.this.setLastHit(null);
				}
			}
		}, Skywars.get().getConfig().getLong("lastHitResetCooldown") * 20);
	}

	public SavedPlayer getSavedPlayer() {
		return this.savedPlayer;
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
		return this.spectator;
	}

	public void setSpectator(boolean spectator) {
		this.spectator = spectator;
	}

	public int getNumber() {
		return this.teamNumber;
	}

	public Player getPlayer() {
		return this.player;
	}

	Arena getArena() {
		return this.arena;
	}

	SkywarsTeam getTeam() {
		return this.team;
	}

	public boolean leaveTeam() {
		if (this.getTeam() == null)
			return false;
		this.team.removeUser(this);
		this.team = null;
		return true;
	}

}
