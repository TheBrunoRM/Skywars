package me.brunorm.skywars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;

public class WhereCommand implements CommandExecutor {
	
	String str = "&eYou are in &a%s";
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if(sender instanceof Player) {			
			Player player = (Player) sender;
			player.sendMessage(Messager.colorFormat(str, player.getWorld().getName()));
			return true;
		} else {
			sender.sendMessage(Messager.colorFormat(str, "the console"));
			return false;
		}
	}

}
