package me.brunorm.skywars;

import org.bukkit.entity.Player;

public class SkywarsActionbar {

	public static void update(Player player) {
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			if (arena.getStatus() == ArenaStatus.STARTING) {
				SkywarsPlayer swp = arena.getPlayer(player);
				Skywars.get().NMS().sendActionbar(player, Messager.color(Skywars.get().langConfig
						.getString("actionbar.waiting").replaceAll("%kit%", swp.getKit().getName())));
			}

		}
	}

}
