/* (C) 2021 Bruno */
package me.thebrunorm.skywars.structures;

import java.util.ArrayList;

public class SkywarsTeam {

	int number;
	boolean disbanded = false;

	public SkywarsTeam(Arena arena) {
		this.arena = arena;
		this.number = arena.getTeams().size();
	}

	ArrayList<SkywarsUser> users = new ArrayList<SkywarsUser>();

	ArrayList<SkywarsUser> getUsers() {
		return this.users;
	}

	Arena arena;

	Arena getArena() {
		return this.arena;
	}

	boolean addUser(SkywarsUser user) {
		if (this.disbanded)
			return false;
		if (this.users.size() >= this.arena.getMap().teamSize)
			return false;
		this.users.add(user);
		return true;
	}

	public int getNumber() {
		return this.number;
	}

	public void disband() {
		this.disbanded = true;
		this.users.forEach(user -> user.leaveTeam());
		this.users.clear();
		this.arena.getTeams().remove(this);
	}

	public void removeUser(SkywarsUser user) {
		this.users.remove(user);
	}
}
