package me.brunorm.skywars.events;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.SkywarsUtils;
import me.brunorm.skywars.commands.CommandsUtils;
import me.brunorm.skywars.managers.ArenaManager;
import me.brunorm.skywars.menus.GameOptionsMenu;
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
			final ItemMeta meta = item.getItemMeta();
			if (meta == null)
				return;
			final String displayName = meta.getDisplayName();
			if (displayName == null)
				return;
			if (item.getType() == XMaterial
					.matchXMaterial(Skywars.get().getConfig().getString("item_types.KIT_SELECTOR")).get()
					.parseMaterial()) {
				if (!arena.started()) {
					KitsMenu.open(player);
					event.setCancelled(true);
				}
			}
			if (displayName.equals(SkywarsUtils.getItemNameFromConfig("LEAVE")) && item.getType() == XMaterial
					.matchXMaterial(Skywars.get().getConfig().getString("item_types.LEAVE")).get().parseMaterial()) {
				if (arena.getStatus() != ArenaStatus.PLAYING || swp.isSpectator()) {
					arena.leavePlayer(swp);
					event.setCancelled(true);
				}
			}
			if (displayName.equals(SkywarsUtils.getItemNameFromConfig("PLAY_AGAIN")) && item.getType() == XMaterial
					.matchXMaterial(Skywars.get().getConfig().getString("item_types.PLAY_AGAIN")).get()
					.parseMaterial()) {
				if (!swp.isSpectator())
					return;
				player.sendMessage(Messager.color("&aSending you to another game..."));
				arena.leavePlayer(swp);
				ArenaManager.joinRandomMap(player);
				event.setCancelled(true);
			}
			if (item.getType() == XMaterial
					.matchXMaterial(Skywars.get().getConfig().getString("item_types.GAME_OPTIONS")).get()
					.parseMaterial()) {
				if (swp.isSpectator())
					return;
				if (arena.started())
					return;
				GameOptionsMenu.open(player);
				event.setCancelled(true);
			}
			if (item.getType() == XMaterial.matchXMaterial(Skywars.get().getConfig().getString("item_types.START_GAME"))
					.get().parseMaterial()) {
				if (arena.started())
					return;
				if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.start"))
					return;
				arena.softStart(player);
				event.setCancelled(true);
			}
			if (item.getType() == XMaterial.matchXMaterial(Skywars.get().getConfig().getString("item_types.STOP_GAME"))
					.get().parseMaterial()) {
				if (!arena.started())
					return;
				if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.stop"))
					return;
				arena.clear();
				event.setCancelled(true);
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
						final SkywarsMap map = Skywars.get().getMapManager().getMap(ChatColor.stripColor(mapName));
						if (map != null) {
							ArenaManager.joinMap(map, player);
						} else
							player.sendMessage(String.format("map %s not found", mapName));
					}
				}
				if (sign.getLine(1).equals("random")) {
					if (sign.getLine(2).equals("skywars")) {
						event.setCancelled(true);
						ArenaManager.joinRandomMap(player);
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
