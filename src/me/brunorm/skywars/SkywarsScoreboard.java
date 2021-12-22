package me.brunorm.skywars;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class SkywarsScoreboard {

	static String url = getUrl();

	public static String format(String text, Arena arena, SkywarsPlayer player) {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		String strDate = formatter.format(date);

		List<SkywarsPlayer> players = new ArrayList<>(arena.getPlayers());
		players.removeIf(p -> p.isSpectator());

		text = text.replaceAll(getVariableCode("map"), arena.getName())
				.replaceAll(getVariableCode("players"), Integer.toString(players.size()))
				.replaceAll(getVariableCode("maxplayers"), Integer.toString(arena.getMaxPlayers()))
				.replaceAll(getVariableCode("status"), getStatus(arena))
				.replaceAll(getVariableCode("kills"), Integer.toString(player.getKills()))
				.replaceAll(getVariableCode("date"), strDate).replaceAll(getVariableCode("url"), url);
		return ChatColor.translateAlternateColorCodes('&', text);
	}

	public static String getVariableCode(String thing) {
		return String.format("%%%s%%", thing);
	}

	public static String getUrl() {
		FileConfiguration config = Skywars.get().getConfig();
		url = config.getString("url");
		if (url != null)
			return url;
		else
			return "www.skywars.com";
	}

	public static String getStatus(Arena arena) {
		YamlConfiguration config = Skywars.get().langConfig;
		String status;

		if (arena.getStatus() == ArenaStatus.STARTING) {
			status = config.getString("status.starting").replaceAll(getVariableCode("seconds"),
					Integer.toString(arena.countdown));
		} else if (arena.getStatus() == ArenaStatus.WAITING) {
			status = config.getString("status.starting");
		} else {
			status = config.getString("status.ending").replaceAll(getVariableCode("seconds"),
					Integer.toString(arena.countdown));
		}

		return status;
	}

	public static void update(Player player) {

		YamlConfiguration config = Skywars.get().scoreboardConfig;

		if (config == null) {
			System.out.println("no config, is null");
			return;
		}

		ScoreboardManager manager = Bukkit.getScoreboardManager();
		Scoreboard board = manager.getNewScoreboard();
		// Team team = board.registerNewTeam("teamname");
		Objective objective = board.registerNewObjective("test", "dummy");

		objective.setDisplayName(Messager.color(config.getString("title")));
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		String[] colorSymbols = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "f" };
		ArrayList<String> texts = new ArrayList<String>();

		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			List<String> arenaBoard = null;
			if (swp.isSpectator()) {
				arenaBoard = config.getStringList("arena.spectator");
			} else {
				if (arena.getStatus() == ArenaStatus.PLAYING) {
					arenaBoard = config.getStringList("arena.player");
				} else {
					arenaBoard = config.getStringList("arena.intermission");
				}
			}

			if (arenaBoard != null) {
				for (int i = 0; i < arenaBoard.size(); i++) {
					String text = format(arenaBoard.get(i), arena, swp);
					texts.add(i, text);
				}
			}
		}

		int textIndex = 0;
		for (int i = texts.size(); i > 0; i--) {
			String text = texts.get(textIndex);
			if (text == null)
				continue;
			if (text.equals(""))
				text = Messager.color("&" + colorSymbols[textIndex]);
			Score score = objective.getScore(text);
			score.setScore(i);
			textIndex++;
		}

		player.setScoreboard(board);
	}
}
