package me.brunorm.skywars.commands;

import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;

public class CommandsUtils {
	
	public static boolean permissionCheckWithMessage(Player player, String permission) {
		if(!player.hasPermission(permission)) {
			player.sendMessage(Messager.color(Skywars.get().langConfig.getString("NO_PERMISSION")));
			return false;
		}
		return true;
	}
	
	public static boolean arenaCheckWithMessage(Player player) {
		if(Skywars.get().getPlayerArena(player) == null) {
			player.sendMessage(Messager.color(Skywars.get().langConfig.getString("NOT_JOINED")));
			return false;
		}
		return true;
	}
}
