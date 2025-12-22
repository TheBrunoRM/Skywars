/* (C) 2021 Bruno */
package me.thebrunorm.skywars.handlers;

import me.thebrunorm.skywars.ArenaStatus;
import me.thebrunorm.skywars.MessageUtils;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public enum SkywarsScoreboard {
	;

	final static ScoreboardManager manager = Bukkit.getScoreboardManager();

	public static void update(Player player) {

		final ArrayList<String> texts = new ArrayList<>();

		final Arena arena = Skywars.get().getPlayerArena(player);
		SkywarsUser swp = null;
		List<String> stringList = null;
		if (arena != null) {
			swp = arena.getUser(player);
			if (swp.isSpectator()) {
				stringList = Skywars.langConfig.getStringList("scoreboard.arena.spectator");
			} else {
				if (arena.getStatus() == ArenaStatus.PLAYING) {
					stringList = Skywars.langConfig.getStringList("scoreboard.arena.player");
				} else {
					stringList = Skywars.langConfig.getStringList("scoreboard.arena.intermission");
				}
			}
		} else {
			final List<String> worldNames = Skywars.get().getConfig().getStringList("scoreboardWorlds").stream()
					.map(w -> w.toLowerCase()).collect(Collectors.toList());
			if (worldNames.size() <= 0 || worldNames.contains(player.getWorld().getName().toLowerCase())) {
				stringList = Skywars.langConfig.getStringList("scoreboard.lobby");
			}
		}

		if (stringList == null) {
			final Objective current = player.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
			if (current != null && current.getName() == "skywars")
				player.setScoreboard(manager.getNewScoreboard());
			return;
		}

		// TODO make it less intensive and use cache

		final Scoreboard board = manager.getNewScoreboard();
		final Objective objective = board.registerNewObjective("skywars", "");

		objective.setDisplayName(SkywarsUtils.format(MessageUtils.get("scoreboard.title"), player, arena, swp));
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		for (int i = 0; i < stringList.size(); i++) {
			final String text = SkywarsUtils.format(stringList.get(i), player, arena, swp);
			texts.add(i, MessageUtils.color(text));
		}

		int textIndex = 0;
		for (int i = texts.size(); i > 0; i--) {
			String text = texts.get(textIndex);
			if (text == null)
				continue;
			if (text.equals(""))
				text = MessageUtils.color("&" + SkywarsUtils.COLOR_SYMBOLS[textIndex]);
			final Score score = objective.getScore(text);
			score.setScore(i);
			textIndex++;
		}

		try {
			// just in case
			player.setScoreboard(board);
		} catch (final Exception e) {
			e.printStackTrace();
			Skywars.get().sendDebugMessage("Could not set scoreboard for player %s", player);
		}

	}
}
