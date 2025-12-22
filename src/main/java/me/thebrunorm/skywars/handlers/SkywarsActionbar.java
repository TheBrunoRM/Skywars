// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.handlers;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.singletons.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.entity.Player;

public enum SkywarsActionbar {
	;

	public static void update(Player player) {
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null) return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp.isSpectator())
			Skywars.get().NMS().sendActionbar(player, MessageUtils
					.color(SkywarsUtils.format(Skywars.langConfig.getString("actionbar.spectating"), player, arena, swp)));
		else if (!arena.started())
			Skywars.get().NMS().sendActionbar(player, MessageUtils
					.color(SkywarsUtils.format(Skywars.langConfig.getString("actionbar.waiting"), player, arena, swp)));
	}
}
