package me.thebrunorm.skywars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Arena;

public class ForceStartCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (!CommandsUtils.consoleCheckWithMessage(sender))
			return true;
		final Player player = (Player) sender;
		if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.forcestart"))
			return true;
		if (!CommandsUtils.arenaCheckWithMessage(player))
			return true;
		final Arena arena = Skywars.get().getPlayerArena(player);
		arena.startGame(player);
		return true;
	}
}
