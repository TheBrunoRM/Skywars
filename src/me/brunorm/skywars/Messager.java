package me.brunorm.skywars;

import org.bukkit.ChatColor;

public class Messager {

	public static String color(String thing) {
		return ChatColor.translateAlternateColorCodes('&', thing);
	}
	
	public static String colorFormat(String thing, Object... format) {
		return ChatColor.translateAlternateColorCodes('&', String.format(thing, format));
	}

}
