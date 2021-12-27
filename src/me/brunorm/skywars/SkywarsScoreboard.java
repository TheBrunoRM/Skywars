package me.brunorm.skywars;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class SkywarsScoreboard {
	
	public static YamlConfiguration config = Skywars.get().scoreboardConfig;
	
	public static void update(Player player) {
		
		if (config == null) {
			return;
		}
		
		ArrayList<String> texts = new ArrayList<String>();

		Arena arena = Skywars.get().getPlayerArena(player);
		SkywarsPlayer swp = null;
		List<String> stringList = null;
		if (arena != null) {
			swp = arena.getPlayer(player);
			if (swp.isSpectator()) {
				stringList = config.getStringList("arena.spectator");
			} else {
				if (arena.getStatus() == ArenaStatus.PLAYING) {
					stringList = config.getStringList("arena.player");
				} else {
					stringList = config.getStringList("arena.intermission");
				}
			}

		} else {
			stringList = config.getStringList("lobby");
		}
		
		if (stringList != null) {
			ScoreboardManager manager = Bukkit.getScoreboardManager();
			Scoreboard board = manager.getNewScoreboard();
			// Team team = board.registerNewTeam("teamname");
			Objective objective = board.registerNewObjective("test", "dummy");

			objective.setDisplayName(Messager.color(config.getString("title")));
			objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			for (int i = 0; i < stringList.size(); i++) {
				String text = SkywarsUtils.format(stringList.get(i), player, arena, swp);
				texts.add(i, Messager.color(text));
			}
			
			int textIndex = 0;
			for (int i = texts.size(); i > 0; i--) {
				String text = texts.get(textIndex);
				if (text == null)
					continue;
				if (text.equals(""))
					text = Messager.color("&" + SkywarsUtils.colorSymbols[textIndex]);
				Score score = objective.getScore(text);
				score.setScore(i);
				textIndex++;
			}
			
			player.setScoreboard(board);
		}

	}
}
