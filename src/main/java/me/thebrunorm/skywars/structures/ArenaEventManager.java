// Copyright (c) 2025 Bruno

package me.thebrunorm.skywars.structures;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.enums.ArenaStatus;
import me.thebrunorm.skywars.enums.SkywarsEventType;
import me.thebrunorm.skywars.singletons.MessageUtils;

import java.util.LinkedList;
import java.util.Queue;

public class ArenaEventManager {

	private final Arena arena;
	private final Queue<SkywarsEvent> events = new LinkedList<>();

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
			return MessageUtils.getMessage("events.no_event");
		final int time = event.getTime();
		final int minutes = time / 60;
		final int seconds = time % 60;
		final String timeString = String.format("%d:%02d", minutes, seconds);
		final String eventNameKey = String.format("events.%s.name", event.getType().name());
		return MessageUtils.color(Skywars.langConfig.getString("events.format", "<events.format>")
				.replaceAll("%name%", Skywars.langConfig.getString(eventNameKey, String.format("<%s>", eventNameKey)))
				.replaceAll("%time%", timeString));

	}

	public SkywarsEvent getNextEvent() {
		return events.peek();
	}

	public SkywarsEvent executeEvent() {
		SkywarsEvent event = events.poll();
		if (event == null) return null;
		event.run();
		return event;
	}

	public Queue<SkywarsEvent> getEvents() {
		return events;
	}
}
