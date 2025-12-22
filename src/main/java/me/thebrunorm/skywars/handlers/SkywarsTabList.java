// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.handlers;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.singletons.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.entity.Player;

public enum SkywarsTabList {
	;

	public static void update(Player player) {
		if (!Skywars.get().getConfig().getBoolean("tabListEnabled", true)) return;

		final Arena arena = Skywars.get().getPlayerArena(player);
		final SkywarsUser user = arena.getUser(player);

		String tabList = getTabList(player, user, arena);

		Skywars.get().NMS().sendTablist(player,
				SkywarsUtils.format(MessageUtils.get("tabList." + tabList + ".header"), player, arena, user),
				SkywarsUtils.format(MessageUtils.get("tabList." + tabList + ".footer"), player, arena, user));
	}

	public static String getTabList(Player player, SkywarsUser user, Arena arena) {
		if (arena == null) return "lobby";

		String tabList = "arena.";
		switch (arena.getStatus()) {
			case WAITING:
			case STARTING:
			case RESTARTING:
				tabList += "intermission";
				break;
			case PLAYING:
				tabList += user.isSpectator() ? "spectator":"player";
				break;
			default:
				break;
		}

		return tabList;
	}
}
