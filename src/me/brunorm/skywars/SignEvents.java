package me.brunorm.skywars;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.cryptomorin.xseries.XMaterial;

public class SignEvents implements Listener {
	
	@EventHandler
	void onSignChange(SignChangeEvent event) {
		if(event.getLine(1).equalsIgnoreCase("[SkyWars]")) {
			String arenaName = event.getLine(2);
			if(arenaName != null) {
				Arena arena = Skywars.get().getArena(arenaName);
				if(arena != null) {
					event.setLine(1, Messager.color("&e[&bSkyWars&e]"));
					event.setLine(2, Messager.color(String.format("&a%s", arenaName)));
				} else System.out.println("null arena " + arenaName);
			} else System.out.println("null arena name");
		} else System.out.println("line is not skywars: " + event.getLine(1));
	}
	
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null && event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == XMaterial.OAK_SIGN.parseMaterial()
					|| event.getClickedBlock().getType() == XMaterial.OAK_WALL_SIGN.parseMaterial()) {
				if(player.isSneaking() && player.getGameMode() == GameMode.CREATIVE) return;
				Sign sign = (Sign) event.getClickedBlock().getState();
				if (sign.getLine(1).equals(Messager.color("&e[&bSkyWars&e]"))) {
					String arenaName = sign.getLine(2);
					if (arenaName != null) {
						Arena newArena = Skywars.get().getArena(ChatColor.stripColor(arenaName));
						if (newArena != null) {
							newArena.JoinPlayer(player);
						} else
							player.sendMessage(String.format("arena %s not found", arenaName));
					}
				}
				if (sign.getLine(1).equals("click")) {
					if (sign.getLine(2).equals("skywars")) {
						Arena randomArena = Skywars.get().getRandomJoinableArena();
						randomArena.JoinPlayer(player);
					}
				}
			}
		}
	}
}
