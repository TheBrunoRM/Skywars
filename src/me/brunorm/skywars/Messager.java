package me.brunorm.skywars;

import org.bukkit.ChatColor;

public class Messager {

	public static char ALT_COLOR_CHAR = '&';
	
	public static String color(String thing) {
		return ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, thing);
	}
	
	public static String colorFormat(String thing, Object... format) {
		return Messager.color(String.format(thing, format));
	}

}
