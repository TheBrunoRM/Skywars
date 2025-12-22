// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.commands;

import me.thebrunorm.skywars.singletons.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WhereCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;
			sender.sendMessage(MessageUtils.getMessage("where", player.getWorld().getName()));
		} else {
			sender.sendMessage(MessageUtils.getMessage("where", MessageUtils.getMessage("the_console")));
		}
		return true;
	}

}
