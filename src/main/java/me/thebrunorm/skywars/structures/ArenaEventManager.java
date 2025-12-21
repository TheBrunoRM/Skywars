package me.thebrunorm.skywars.structures;

import me.thebrunorm.skywars.ArenaStatus;
import me.thebrunorm.skywars.MessageUtils;
import me.thebrunorm.skywars.Skywars;

import java.util.ArrayList;

public class ArenaEventManager {

	private final Arena arena;
	private final ArrayList<SkywarsEvent> events = new ArrayList<>();

	public ArenaEventManager(Arena arena) {
		this.arena = arena;
		this.events.add(new SkywarsEvent(arena, SkywarsEventType.REFILL, 60));
		this.events.add(new SkywarsEvent(arena, SkywarsEventType.REFILL, 60));
		this.events.add(new SkywarsEvent(arena, SkywarsEventType.ENDER_DRAGON, 60 * 3));
	}

	public String getNextEventText() {
		if (arena.getStatus() == ArenaStatus.RESTARTING)
			return Skywars.langConfig.getString("status.ended");
		final SkywarsEvent event = this.getNextEvent();
		if (event == null)
			return MessageUtils.getMessage("events.noevent");
		final int time = event.getTime();
		final int minutes = time / 60;
		final int seconds = time % 60;
		final String timeString = String.format("%d:%02d", minutes, seconds);
		return MessageUtils.color(Skywars.langConfig.getString("events.format")
			.replaceAll("%name%", Skywars.langConfig.getString("events." + event.getType().name().toLowerCase()))
			.replaceAll("%time%", timeString));

	}

	public SkywarsEvent getNextEvent() {
		if (this.events.isEmpty()) return null;
		return this.events.get(0);
	}

	public SkywarsEvent skipEvent() {
		return events.remove(0);
	}
}
