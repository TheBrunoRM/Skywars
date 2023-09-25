package me.brunorm.skywars.handlers;

import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsUser;

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
				SkywarsUtils.format(Messager.get("tablist." + tablist + ".header"), player, arena, swp),
				SkywarsUtils.format(Messager.get("tablist." + tablist + ".footer"), player, arena, swp));
	}
}
