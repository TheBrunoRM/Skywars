/* (C) 2021 Bruno */
package me.thebrunorm.skywars.handlers;

import org.bukkit.entity.Player;

import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;

public class SkywarsActionbar {

	public static void update(Player player) {
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (arena.started() || swp.isSpectator())
			return;
		Skywars.get().NMS().sendActionbar(player, Messager
				.color(SkywarsUtils.format(Skywars.langConfig.getString("actionbar.waiting"), player, arena, swp)));
	}
}
