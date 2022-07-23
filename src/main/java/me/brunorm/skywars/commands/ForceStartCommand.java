package me.brunorm.skywars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;

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
		final Arena playerArena = Skywars.get().getPlayerArena(player);
		playerArena.startGame();
		return true;
	}

}
