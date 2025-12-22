// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.handlers;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.enums.ArenaStatus;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.singletons.SkywarsUtils;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public enum SkywarsScoreboard {
	;

	final static ScoreboardManager manager = Bukkit.getScoreboardManager();
	final static String scoreboardObjectiveName = Skywars.get().getConfig().getString("scoreboard.objective_name", "skywars");

	public static void update(Player player) {

		if (!Skywars.get().getConfig().getBoolean("scoreboard.enabled", true)) return;

		final ArrayList<String> texts = new ArrayList<>();
		final Arena arena = Skywars.get().getPlayerArena(player);
		final SkywarsUser user = arena.getUser(player);
		List<String> stringList = getStringList(arena, user, player);

		// TODO maybe there's a way to reutilize the board
		// instead of creating one each time
		final Scoreboard board = manager.getNewScoreboard();

		if (stringList == null || stringList.size() <= 0) {
			final Objective current = player.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
			if (current != null && current.getName().equals(scoreboardObjectiveName))
				player.setScoreboard(board);
			return;
		}

		final Objective objective = board.registerNewObjective(scoreboardObjectiveName, "");

		objective.setDisplayName(SkywarsUtils.format(MessageUtils.get("scoreboard.title"), player, arena, user));
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		for (int i = 0; i < stringList.size(); i++) {
			final String text = SkywarsUtils.format(stringList.get(i), player, arena, user);
			texts.add(i, MessageUtils.color(text));
		}

		int textIndex = 0;
		for (int i = texts.size(); i > 0; i--) {
			String text = texts.get(textIndex);
			if (text == null) continue;
			if (text.isEmpty())
				text = MessageUtils.color("&" + SkywarsUtils.COLOR_SYMBOLS[textIndex]);
			final Score score = objective.getScore(text);
			score.setScore(i);
			textIndex++;
		}

		try {
			player.setScoreboard(board);
		} catch (final Exception e) {
			Skywars.get().getLogger().log(Level.SEVERE, "Could not set scoreboard for player: " + player, e);
		}
	}

	public static List<String> getStringList(Arena arena, SkywarsUser user, Player player) {

		if (arena == null) {
			final List<String> worldNames = Skywars.get().getConfig().getStringList("lobbyScoreboardWorlds").stream()
					.map(String::toLowerCase).collect(Collectors.toList());
			if (worldNames.size() <= 0 || worldNames.contains(player.getWorld().getName().toLowerCase())) {
				return Skywars.langConfig.getStringList("scoreboard.lobby");
			}
			return null;
		}

		if (user.isSpectator()) {
			return Skywars.langConfig.getStringList("scoreboard.arena.spectator");
		}
		if (arena.getStatus() == ArenaStatus.PLAYING) {
			return Skywars.langConfig.getStringList("scoreboard.arena.player");
		}

		return Skywars.langConfig.getStringList("scoreboard.arena.intermission");
	}
}
