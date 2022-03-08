package me.brunorm.skywars;

import org.bukkit.entity.Player;

import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class SkywarsActionbar {

	public static void update(Player player) {
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			final SkywarsPlayer swp = arena.getPlayer(player);
			if (!arena.started() && !swp.isSpectator()) {
				Skywars.get().NMS().sendActionbar(player, Messager.color(
						SkywarsUtils.format(Skywars.langConfig.getString("actionbar.waiting"), player, arena, swp)));
			}

		}
	}
}
