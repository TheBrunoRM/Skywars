package me.brunorm.skywars.events;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.menus.ArenaMenu;
import me.brunorm.skywars.menus.KitsMenu;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class InteractEvent implements Listener {
	
	@SuppressWarnings("deprecation")
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {		
			SkywarsPlayer swp = arena.getPlayer(player);
			ItemStack item;
			item = player.getItemInHand();
			if (item.getType() == XMaterial.BOW.parseMaterial()) {
				if (arena.getStatus() == ArenaStatus.STARTING) {
					KitsMenu.open(player);
					event.setCancelled(true);
				}
			}
			if (item.getType() == XMaterial.RED_BED.parseMaterial()) {
				if (arena.getStatus() != ArenaStatus.PLAYING
						|| swp.isSpectator()) {
					arena.leavePlayer(swp);
					event.setCancelled(true);
				}
			}
		} else if (Skywars.get().getConfig().getBoolean("signsEnabled")
				&& event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == XMaterial.OAK_SIGN.parseMaterial()
					|| event.getClickedBlock().getType() == XMaterial.OAK_WALL_SIGN.parseMaterial()) {
				if(player.getGameMode() == GameMode.CREATIVE && player.isSneaking())
					return;
				Sign sign = (Sign) event.getClickedBlock().getState();
				if (sign.getLine(1).equals(Messager.color("&e[&bSkyWars&e]"))) {
					String arenaName = sign.getLine(2);
					if (arenaName != null) {
						Arena newArena = Skywars.get().getArena(ChatColor.stripColor(arenaName));
						if (newArena != null) {
							newArena.joinPlayer(player);
						} else
							player.sendMessage(String.format("arena %s not found", arenaName));
					}
				}
				if (sign.getLine(1).equals("random")) {
					if (sign.getLine(2).equals("skywars")) {
						event.setCancelled(true);
						Arena randomArena = Skywars.get().getRandomJoinableArena();
						randomArena.joinPlayer(player);
					}
				}
				if (sign.getLine(1).equals("play")) {
					if (sign.getLine(2).equals("skywars")) {
						event.setCancelled(true);
						ArenaMenu.open(player);
					}
				}
			}
		}
	}
}
