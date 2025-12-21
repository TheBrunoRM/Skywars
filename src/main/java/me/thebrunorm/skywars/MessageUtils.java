/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageUtils {

	public static final char ALT_COLOR_CHAR = '&';

	public static void send(CommandSender sender, String text, Object... format) {
		sender.sendMessage(color(text, format));
	}

	public static void sendTranslated(CommandSender sender, String key, Object... format) {
		sender.sendMessage(getMessage(key, format));
	}

	public static String color(String text) {
		return ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, text);
	}

	public static String color(String text, Object... format) {
		return MessageUtils.color(String.format(text, format));
	}

	public static String get(String name, Object... format) {
		return getMessage(name, format);
	}

	public static String getMessage(String name, Object... format) {
		String msg = "";
		if (Skywars.langConfig.get(name) instanceof List)
			msg = String.join("\n", Skywars.langConfig.getStringList(name));
		else
			msg = Skywars.langConfig.getString(name);
		if (msg == null)
			return name;
		for (int i = 0; i < format.length; i++) {
			msg = msg.replaceAll(String.format("\\{%s\\}", i), String.valueOf(format[i]));
		}
		return MessageUtils.color(msg);
	}

	public static String getFormattedMessage(String name, Player player, Arena arena, SkywarsUser swp,
			Object... format) {
		return MessageUtils.color(SkywarsUtils.format(getMessage(name, format), player, arena, swp));
	}

}
