package me.brunorm.skywars;

import org.bukkit.entity.Player;

import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class SkywarsActionbar {

	public static void update(Player player) {
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			if (arena.getStatus() != ArenaStatus.PLAYING && !swp.isSpectator()) {
				Skywars.get().NMS().sendActionbar(player, Messager.color(
						SkywarsScoreboard.format(Skywars.get().langConfig
								.getString("actionbar.waiting"), player, arena, swp)));
			}

		}
	}

}
