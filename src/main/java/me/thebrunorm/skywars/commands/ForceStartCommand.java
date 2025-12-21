/* (C) 2021 Bruno */
package me.thebrunorm.skywars.commands;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForceStartCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (CommandsUtils.consoleCheck(sender))
			return true;
		final Player player = (Player) sender;
		if (!CommandsUtils.hasPermission(player, "skywars.forcestart"))
			return true;
		if (!CommandsUtils.isInArenaJoined(player))
			return true;
		final Arena arena = Skywars.get().getPlayerArena(player);
		arena.startGame(player);
		return true;
	}
}
