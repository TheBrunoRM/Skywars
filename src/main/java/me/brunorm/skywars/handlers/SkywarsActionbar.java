package me.brunorm.skywars.handlers;

import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsUser;

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
