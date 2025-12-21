/* (C) 2021 Bruno */
package me.thebrunorm.skywars.structures;

import me.thebrunorm.skywars.MessageUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

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
			this.arena.displayChestHolograms(this.arena.getEventManager().getNextEventText());
	}

	public void run() {
		switch (this.type) {
			case REFILL:
				this.arena.fillChests();
				this.arena.displayChestHolograms(MessageUtils.get("chest_holograms.refilled"));
				break;
			case ENDER_DRAGON:
				Vector position = this.arena.getCenterBlock().add(new Vector(0, 100, 0));
				World world = this.arena.getWorld();
				world.setGameRuleValue("mobGriefing", "true");
				Location location = position.toLocation(world);
				world.spawnEntity(location, EntityType.ENDER_DRAGON);
				break;
		}
		this.arena.broadcastEventMessage(this.type);
	}
}
