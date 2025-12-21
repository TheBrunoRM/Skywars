/* (C) 2021 Bruno */
package me.thebrunorm.skywars.handlers;

import me.thebrunorm.skywars.MessageUtils;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.entity.Player;

public class SkywarsTablist {
	public static void update(Player player) {
		final Arena arena = Skywars.get().getPlayerArena(player);
		SkywarsUser swp = null;
		String tablist = "";

		if (arena != null)
			swp = arena.getUser(player);
		if (arena == null) {
			tablist = "lobby";
		} else {
			tablist = "arena.";
			switch (arena.getStatus()) {
			case WAITING:
			case STARTING:
			case RESTARTING:
				tablist += "intermission";
				break;
			case PLAYING:
				tablist += swp.isSpectator() ? "spectator" : "player";
				break;
			default:
				break;

			}
		}
		Skywars.get().NMS().sendTablist(player,
			SkywarsUtils.format(MessageUtils.get("tablist." + tablist + ".header"), player, arena, swp),
			SkywarsUtils.format(MessageUtils.get("tablist." + tablist + ".footer"), player, arena, swp));
	}
}
