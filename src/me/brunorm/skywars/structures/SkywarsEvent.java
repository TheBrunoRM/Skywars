package me.brunorm.skywars.structures;

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
		return arena;
	}
	
	public int getTime() {
		return time;
	}
	
	public void setTime(int time) {
		this.time = time;
	}
	
	public SkywarsEventType getType() {
		return type;
	}
	
	public void setType(SkywarsEventType type) {
		this.type = type;
	}

	public void decreaseTime() {
		setTime(getTime()-1);
	}

	public void run() {
		switch(type) {
		case REFILL:
			arena.broadcastRefillMessage();
			arena.calculateAndFillChests();
			break;
		}
	}
}
