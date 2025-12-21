/* (C) 2021 Bruno */
package me.thebrunorm.skywars.commands;

import me.thebrunorm.skywars.MessageUtils;
import me.thebrunorm.skywars.Skywars;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandsUtils {

	public static boolean consoleCheck(CommandSender sender) {
		if (sender instanceof Player)
			return false;
		sender.sendMessage(MessageUtils.getMessage("CANT_EXECUTE_COMMAND_IN_CONSOLE"));
		return true;
	}

	public static boolean lacksPermission(CommandSender sender, String permission) {
		if (sender.hasPermission(permission))
			return false;
		sender.sendMessage(MessageUtils.color(Skywars.langConfig.getString("NO_PERMISSION")));
		return true;
	}

	public static boolean hasPermission(Player player, String permission) {
		if (player.hasPermission(permission))
			return true;
		player.sendMessage(MessageUtils.color(Skywars.langConfig.getString("NO_PERMISSION")));
		return false;
	}

	public static boolean isInArenaJoined(Player player) {
		if (Skywars.get().getPlayerArena(player) != null)
			return true;
		player.sendMessage(MessageUtils.color(Skywars.langConfig.getString("NOT_JOINED")));
		return false;
	}
}
