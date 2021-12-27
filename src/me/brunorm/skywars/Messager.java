package me.brunorm.skywars;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class Messager {

	public static char ALT_COLOR_CHAR = '&';
	
	public static String color(String thing) {
		return ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, thing);
	}
	
	public static String colorFormat(String thing, Object... format) {
		return Messager.color(String.format(thing, format));
	}
	
	public static String getMessage(String name, Object... format) {
		String msg = Skywars.get().langConfig.getString(name);
		if(msg == null) return null;
		for(int i = 0; i < format.length; i++) {
			msg = msg.replaceAll(String.format("\\{%s\\}", i), (String) format[i]);
		}
		return Messager.color(msg);
	}
	
	public static String getFormattedMessage(String name, Player player, Arena arena, SkywarsPlayer swp, Object... format) {
		return Messager.color(SkywarsUtils.format(getMessage(name, format), player, arena, swp));
	}

}
