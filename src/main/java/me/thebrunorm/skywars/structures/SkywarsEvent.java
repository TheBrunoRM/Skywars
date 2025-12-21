/* (C) 2021 Bruno */
package me.thebrunorm.skywars.structures;

import me.thebrunorm.skywars.Messager;

public class SkywarsEvent {

	Arena arena;
	SkywarsEventType type;
	int time;

	SkywarsEvent(Arena arena, SkywarsEventType type, int time) {
		this.arena = arena;
		this.type = type;
		this.time = time;
	}

	public Arena getArena() {
		return this.arena;
	}

	public int getTime() {
		return this.time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public SkywarsEventType getType() {
		return this.type;
	}

	public void setType(SkywarsEventType type) {
		this.type = type;
	}

	public void decreaseTime() {
		this.setTime(this.getTime() - 1);
		if (this.getType() == SkywarsEventType.REFILL)
			this.arena.displayChestHolograms(this.arena.getNextEventText());
	}

	public void run() {
		switch (this.type) {
		case REFILL:
			this.arena.broadcastRefillMessage();
			this.arena.fillChests();
			this.arena.displayChestHolograms(Messager.get("chest_holograms.refilled"));
			break;
		}
	}
}
