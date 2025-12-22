/* (C) 2021 Bruno */
package me.thebrunorm.skywars.handlers;

import me.thebrunorm.skywars.MessageUtils;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.entity.Player;

public enum SkywarsActionbar {
	;

	public static void update(Player player) {
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (arena.started() || swp.isSpectator())
			return;
		Skywars.get().NMS().sendActionbar(player, MessageUtils
				.color(SkywarsUtils.format(Skywars.langConfig.getString("actionbar.waiting"), player, arena, swp)));
	}
}
