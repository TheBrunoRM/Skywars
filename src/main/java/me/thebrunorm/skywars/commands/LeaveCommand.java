/* (C) 2021 Bruno */
package me.thebrunorm.skywars.commands;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (CommandsUtils.consoleCheck(sender))
			return true;
		final Player player = (Player) sender;
		final Arena playerArena = Skywars.get().getPlayerArena(player);
		if (!CommandsUtils.isInArenaJoined(player))
			return true;
		playerArena.leavePlayer(player);
		return true;
	}

}
