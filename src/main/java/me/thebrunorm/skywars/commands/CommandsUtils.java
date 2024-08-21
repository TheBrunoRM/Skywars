package me.thebrunorm.skywars.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;

public class CommandsUtils {

	public static boolean consoleCheckWithMessage(CommandSender sender) {
		if (sender instanceof Player)
			return true;
		sender.sendMessage(Messager.getMessage("CANT_EXECUTE_COMMAND_IN_CONSOLE"));
		return false;
	}

	public static boolean permissionCheckWithMessage(CommandSender sender, String permission) {
		if (sender.hasPermission(permission))
			return true;
		sender.sendMessage(Messager.color(Skywars.langConfig.getString("NO_PERMISSION")));
		return false;
	}

	public static boolean permissionCheckWithMessage(Player player, String permission) {
		if (player.hasPermission(permission))
			return true;
		player.sendMessage(Messager.color(Skywars.langConfig.getString("NO_PERMISSION")));
		return false;
	}

	public static boolean arenaCheckWithMessage(Player player) {
		if (Skywars.get().getPlayerArena(player) != null)
			return true;
		player.sendMessage(Messager.color(Skywars.langConfig.getString("NOT_JOINED")));
		return false;
	}
}
