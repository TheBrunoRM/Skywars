package me.thebrunorm.skywars;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;

public class Messager {

	private Messager() {
	}

	public static final char ALT_COLOR_CHAR = '&';

	public static String color(String text) {
		return ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, text);
	}

	public static String color(String text, Object... format) {
		return Messager.color(String.format(text, format));
	}

	public static String get(String name, Object... format) {
		return getMessage(name, format);
	}

	public static String getMessage(String name, Object... format) {
		if (Skywars.langConfig == null) {
			return name; // Return the key itself if langConfig is not loaded yet
		}
		String msg = "";
		if (Skywars.langConfig.get(name) instanceof List)
			msg = String.join("\n", Skywars.langConfig.getStringList(name));
		else
			msg = Skywars.langConfig.getString(name);
		if (msg == null)
			return name;
		
		// Handle %s placeholders by replacing them with numbered placeholders first, then handle numbered ones
		if (format.length > 0) {
			for (int i = 0; i < format.length; i++) {
				// Replace first occurrence of %s with {i} to make it compatible with existing system
				msg = msg.replaceFirst("%s", String.format("{%d}", i));
			}
		}
		
		// Handle numbered placeholders like {0}, {1}, etc.
		for (int i = 0; i < format.length; i++) {
			msg = msg.replaceAll(String.format("\\{%s\\}", i), String.valueOf(format[i]));
		}
		
		return Messager.color(msg);
	}

	public static String getFormattedMessage(String name, Player player, Arena arena, SkywarsUser swp,
			Object... format) {
			if (Skywars.langConfig == null) {
				return name; // Return the key itself if langConfig is not loaded yet
			}
			return Messager.color(SkywarsUtils.format(getMessage(name, format), player, arena, swp));
		}
}
