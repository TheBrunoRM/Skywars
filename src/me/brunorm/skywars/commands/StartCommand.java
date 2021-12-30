package me.brunorm.skywars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;

public class StartCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			Arena playerArena = Skywars.get().getPlayerArena(player);
			if(CommandsUtils.permissionCheckWithMessage(player, "skywars.start")) {
				if (CommandsUtils.arenaCheckWithMessage(player)) {
					playerArena.softStart(player);
					return true;
				}
			}
		}
		return false;
	}

}
