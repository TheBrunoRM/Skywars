package me.brunorm.skywars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;

public class WhereCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;
			sender.sendMessage(Messager.getMessage("where", player.getWorld().getName()));
		} else {
			sender.sendMessage(Messager.getMessage("where", "the console"));
		}
		return true;
	}

}
