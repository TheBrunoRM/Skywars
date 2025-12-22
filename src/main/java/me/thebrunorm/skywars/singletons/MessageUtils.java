// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.singletons;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public enum MessageUtils {
	;

	public static final char ALT_COLOR_CHAR = '&';

	public static void send(CommandSender sender, String text, Object... format) {
		sender.sendMessage(color(text, format));
	}

	public static String color(String text, Object... format) {
		return MessageUtils.color(String.format(text, format));
	}

	public static String color(String text) {
		return ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, text);
	}

	public static void sendTranslated(CommandSender sender, String key, Object... args) {
		sender.sendMessage(get(key, args));
	}

	public static String get(String key, Object... args) {
		String msg = resolve(key);
		if (msg == null) return key;

		msg = applyFormat(msg, args);
		return ChatColor.translateAlternateColorCodes(ALT_COLOR_CHAR, msg);
	}

	public static String resolve(String key) {
		YamlConfiguration lang = Skywars.langConfig;
		Object value = lang.get(key);
		if (value instanceof List<?>)
			return String.join("\n", lang.getStringList(key));
		return lang.getString(key);
	}

	private static String applyFormat(String msg, Object... args) {
		for (int i = 0; i < args.length; i++) {
			msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
		}
		return msg;
	}

	public static String getFormattedMessage(String name, Player player, Arena arena, SkywarsUser swp,
											 Object... format) {
		return MessageUtils.color(SkywarsUtils.format(get(name, format), player, arena, swp));
	}

}
