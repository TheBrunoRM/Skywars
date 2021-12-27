package me.brunorm.skywars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;

public class LeaveCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			Arena playerArena = Skywars.get().getPlayerArena(player);
			if (CommandsUtils.arenaCheckWithMessage(player))
				playerArena.leavePlayer(player);
		} else {
			sender.sendMessage(Messager.getMessage("CANT_EXECUTE_COMMAND_IN_CONSOLE"));
		}
		return true;
	}

}
