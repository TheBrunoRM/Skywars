package me.brunorm.skywars;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsEvent;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class SkywarsScoreboard {

	public static String url = getUrl();
	public static String[] colorSymbols = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "f" };
	public static YamlConfiguration config = Skywars.get().scoreboardConfig;
	
	public static String format(String text, Player player, Arena arena, SkywarsPlayer swp) {
		return format(text, player, arena, swp, false);
	}
	
	public static String format(String text, Player player, Arena arena, SkywarsPlayer swp, boolean status) {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		String strDate = formatter.format(date);
		text = text.replaceAll(getVariableCode("date"), strDate)
				.replaceAll(getVariableCode("url"), url);
		
		if(player != null) {
			
			String balance = null;
			
			if(Skywars.get().getEconomy() != null) {
				balance = Double.toString(Skywars.get().getEconomy().getBalance(player));
			} else {
				balance = "Vaultn't";
			}
			text = text.replaceAll(getVariableCode("coins"), balance)
					.replaceAll(getVariableCode("totalkills"),
				Integer.toString(Skywars.get().getPlayerTotalKills(player)))
					.replaceAll(getVariableCode("kit"),
							Skywars.get().getPlayerKit(player).getDisplayName());
		}
		
		if(arena != null) {
			List<SkywarsPlayer> players = new ArrayList<>(arena.getPlayers());
			players.removeIf(p -> p.isSpectator());
			
			// this prevents a stack overflow error
			if(!status) text = text.replaceAll(getVariableCode("status"),
					format(SkywarsUtils.getStatus(arena), player, arena, swp, true));
			
			SkywarsEvent event = arena.getNextEvent();
			String eventText;
			if(event != null) eventText = String.format("%s (%s)", event.getType(), event.getTime());
			else eventText = "No event";
			
			text = text.replaceAll(getVariableCode("map"), arena.getName())
					.replaceAll(getVariableCode("arena"), arena.getName())
					.replaceAll(getVariableCode("event"), eventText)
					.replaceAll(getVariableCode("players"), Integer.toString(players.size()))
					.replaceAll(getVariableCode("maxplayers"), Integer.toString(arena.getMaxPlayers()))
					.replaceAll(getVariableCode("seconds"), Integer.toString(arena.getCountdown()));
		}
		
		if(swp != null) {
			text = text.replaceAll(getVariableCode("kills"), Integer.toString(swp.getKills()));
		}
		
		return text;
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
				String text = format(stringList.get(i), player, arena, swp);
				texts.add(i, Messager.color(text));
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
}
