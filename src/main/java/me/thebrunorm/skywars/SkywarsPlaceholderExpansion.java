/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.thebrunorm.skywars.structures.Kit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkywarsPlaceholderExpansion extends PlaceholderExpansion {

	@Override
	public @NotNull String getIdentifier() {
		return "skywars";
	}

	@Override
	public @NotNull String getAuthor() {
		return "BrunoRM";
	}

	@Override
	public @NotNull String getVersion() {
		return "1.0.0";
	}

	@Override
	public String onRequest(OfflinePlayer offline, String params) {
		final Player player = offline.getPlayer();
		final YamlConfiguration config = Skywars.get().getPlayerConfig(offline);
		final int solo_games = config.getInt("stats.solo.deaths") + config.getInt("stats.solo.wins");
		final int team_games = config.getInt("stats.team.deaths") + config.getInt("stats.team.wins");
		switch (params.toLowerCase()) {
		case "version":
			return Skywars.get().version;
		case "map":
		case "arena":
		case "game":
		case "match":
			if (player == null)
				return MessageUtils.get("not_online");
			return Skywars.get().getPlayerArena(player).getMap().getName();
		case "kills":
		case "game_kills":
			if (player == null)
				return MessageUtils.get("not_online");
			return String.valueOf(Skywars.get().getSkywarsUser(player).getKills());

		// solo
		case "solo_games":
			return String.valueOf(solo_games);
		case "solo_kills":
			return String.valueOf(config.getInt("stats.solo.kills"));
		case "solo_deaths":
			return String.valueOf(config.getInt("stats.solo.deaths"));
		case "solo_wins":
			return String.valueOf(config.getInt("stats.solo.wins"));
		// team
		case "team_games":
			return String.valueOf(team_games);
		case "team_kills":
			return String.valueOf(config.getInt("stats.team.kills"));
		case "team_deaths":
			return String.valueOf(config.getInt("stats.team.deaths"));
		case "team_wins":
			return String.valueOf(config.getInt("stats.team.wins"));
		// total
		case "totalkills":
			return String.valueOf(config.getInt("stats.solo.kills") + config.getInt("stats.team.kills"));
		case "deaths":
		case "totaldeaths":
		case "lostgames":
			return String.valueOf(config.getInt("stats.solo.deaths") + config.getInt("stats.team.deaths"));
		case "wins":
		case "winnedgames":
			return String.valueOf(config.getInt("stats.solo.wins") + config.getInt("stats.team.wins"));
		case "games":
		case "totalgames":
		case "playedgames":
			return String.valueOf(solo_games + team_games);

		case "souls":
			return String.valueOf(config.getInt("souls"));
		case "kit":
			final Kit kit = Skywars.get().getPlayerKit(offline);
			if (kit == null)
				return MessageUtils.get("none");
			return String.valueOf(kit.getDisplayName());
		case "case":
			return config.getString("case", MessageUtils.get("none"));
		default:
			return null;
		}
	}

}
