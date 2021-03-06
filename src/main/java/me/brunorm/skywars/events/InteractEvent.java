package me.brunorm.skywars.events;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
import me.brunorm.skywars.menus.KitsMenu;
import me.brunorm.skywars.menus.MapMenu;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;
import me.brunorm.skywars.structures.SkywarsUser;

public class InteractEvent implements Listener {

	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			final SkywarsUser swp = arena.getUser(player);
			ItemStack item;
			item = player.getItemInHand();
			final String kitSelectorTypeName = Skywars.get().getConfig().getString("item_types.KIT_SELECTOR");
			final Material kitSelectorType = XMaterial.matchXMaterial(kitSelectorTypeName).get().parseMaterial();
			if (item.getType() == kitSelectorType) {
				if (!arena.started()) {
					KitsMenu.open(player);
					event.setCancelled(true);
				}
			}
			final String leaveTypeName = Skywars.get().getConfig().getString("item_types.LEAVE");
			final Material leaveType = XMaterial.matchXMaterial(leaveTypeName).get().parseMaterial();
			if (item.getType() == leaveType) {
				if (arena.getStatus() != ArenaStatus.PLAYING || swp.isSpectator()) {
					arena.leavePlayer(swp);
					event.setCancelled(true);
				}
			}
		} else if (Skywars.get().getConfig().getBoolean("signsEnabled") && event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == XMaterial.OAK_SIGN.parseMaterial()
					|| event.getClickedBlock().getType() == XMaterial.OAK_WALL_SIGN.parseMaterial()) {
				if (player.getGameMode() == GameMode.CREATIVE && player.isSneaking())
					return;
				final Sign sign = (Sign) event.getClickedBlock().getState();
				if (sign.getLine(1).equals(Messager.color("&e[&bSkyWars&e]"))) {
					final String mapName = sign.getLine(2);
					if (mapName != null) {
						final SkywarsMap map = Skywars.get().getMap(ChatColor.stripColor(mapName));
						if (map != null) {
							Skywars.get().joinMap(map, player);
						} else
							player.sendMessage(String.format("map %s not found", mapName));
					}
				}
				if (sign.getLine(1).equals("random")) {
					if (sign.getLine(2).equals("skywars")) {
						event.setCancelled(true);
						Skywars.get().joinRandomMap(player);
					}
				}
				if (sign.getLine(1).equals("play")) {
					if (sign.getLine(2).equals("skywars")) {
						event.setCancelled(true);
						MapMenu.open(player);
					}
				}
			}
		}
	}
}
