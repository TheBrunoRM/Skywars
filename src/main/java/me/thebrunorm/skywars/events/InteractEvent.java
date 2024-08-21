package me.thebrunorm.skywars.events;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import me.thebrunorm.skywars.ArenaStatus;
import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.SkywarsUtils;
import me.thebrunorm.skywars.commands.CommandsUtils;
import me.thebrunorm.skywars.managers.ArenaManager;
import me.thebrunorm.skywars.managers.SignManager;
import me.thebrunorm.skywars.menus.GameOptionsMenu;
import me.thebrunorm.skywars.menus.KitsMenu;
import me.thebrunorm.skywars.menus.MapMenu;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;
import me.thebrunorm.skywars.structures.SkywarsUser;

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
			final FileConfiguration config = Skywars.get().getConfig();
			if (item.getType() == XMaterial.matchXMaterial(config.getString("item_types.KIT_SELECTOR")).get()
					.parseMaterial()) {
				if (!arena.started()) {
					event.setCancelled(true);
					KitsMenu.open(player);
				}
			}
			if (displayName.equals(SkywarsUtils.getItemNameFromConfig("LEAVE")) && item.getType() == XMaterial
					.matchXMaterial(config.getString("item_types.LEAVE")).get().parseMaterial()) {
				if (arena.getStatus() != ArenaStatus.PLAYING || swp.isSpectator()) {
					event.setCancelled(true);
					arena.leavePlayer(swp);
				}
			}
			if (displayName.equals(SkywarsUtils.getItemNameFromConfig("PLAY_AGAIN")) && item.getType() == XMaterial
					.matchXMaterial(config.getString("item_types.PLAY_AGAIN")).get().parseMaterial()) {
				if (!swp.isSpectator())
					return;
				event.setCancelled(true);
				player.sendMessage(Messager.color("&aSending you to another game..."));
				arena.leavePlayer(swp);
				ArenaManager.joinRandomMap(player);
			}
			if (item.getType() == XMaterial.matchXMaterial(config.getString("item_types.GAME_OPTIONS")).get()
					.parseMaterial()) {
				if (swp.isSpectator())
					return;
				if (arena.started())
					return;
				event.setCancelled(true);
				GameOptionsMenu.open(player);
			}
			if (item.getType() == XMaterial.matchXMaterial(config.getString("item_types.START_GAME")).get()
					.parseMaterial()) {
				if (arena.started())
					return;
				if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.start"))
					return;
				event.setCancelled(true);
				arena.softStart(player);
			}
			if (item.getType() == XMaterial.matchXMaterial(config.getString("item_types.STOP_GAME")).get()
					.parseMaterial()) {
				if (!arena.started())
					return;
				if (!CommandsUtils.permissionCheckWithMessage(player, "skywars.stop"))
					return;
				event.setCancelled(true);
				arena.clear();
			}
			return;
		} else {
			final SignManager signManager = Skywars.get().getSignManager();
			if (signManager == null)
				return;
			if (event.getClickedBlock() == null)
				return;
			if (!(event.getClickedBlock().getState() instanceof Sign))
				return;
			if (player.getGameMode() == GameMode.CREATIVE && player.isSneaking())
				return;

			final SkywarsMap map = signManager.getSigns().get(event.getClickedBlock().getLocation());
			if (map != null) {
				event.setCancelled(true);
				ArenaManager.joinMap(map, player);
				return;
			}

			final Sign sign = (Sign) event.getClickedBlock().getState();
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

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		final BlockState state = block.getState();
		if (!(state instanceof Sign))
			return;
		final Location loc = block.getLocation();
		final SignManager signManager = Skywars.get().getSignManager();
		final SkywarsMap map = signManager.getSigns().get(loc);
		if (map != null) {
			signManager.getSigns().remove(loc);
			final YamlConfiguration config = signManager.loadSignConfig();
			final List<String> signsConfig = config.getStringList("signs");
			signsConfig.remove(SignManager.formatElement(loc, map));
			config.set("signs", signsConfig);
			signManager.saveSigns(config);
			Skywars.get().sendDebugMessage("&eRemoved sign (&a%s&e): &b" + loc, map.getName());
		}
	}
}
