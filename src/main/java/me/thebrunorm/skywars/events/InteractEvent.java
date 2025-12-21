/* (C) 2021 Bruno */
package me.thebrunorm.skywars.events;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

	void handleNormalInteraction(PlayerInteractEvent event, Player player) {
		final SignManager signManager = Skywars.get().getSignManager();
		if (signManager == null)
			return;
		if (event.getClickedBlock() == null)
			return;
		if (!(event.getClickedBlock().getState() instanceof Sign sign))
			return;
		if (player.getGameMode() == GameMode.CREATIVE && player.isSneaking())
			return;

		final SkywarsMap map = signManager.getSigns().get(event.getClickedBlock().getLocation());

		if (map != null) {
			event.setCancelled(true);
			ArenaManager.joinMap(map, player);
			return;
		}

		String line1 = sign.getLine(1);
		String line2 = sign.getLine(2);

		if (line1.equals("random") && line2.equals("skywars")) {
			event.setCancelled(true);
			ArenaManager.joinRandomMap(player);
		} else if (line1.equals("play") && line2.equals("skywars")) {
			event.setCancelled(true);
			MapMenu.open(player);
		}
	}

	Material getConfiguredMaterial(FileConfiguration config, String key) {
		return XMaterial.matchXMaterial(config.getString("item_types." + key)).get().parseMaterial();
	}

	void handleArenaInteraction(PlayerInteractEvent event, Player player, Arena arena) {
		final SkywarsUser swp = arena.getUser(player);
		final ItemStack item = player.getItemInHand();
		final ItemMeta meta = item.getItemMeta();
		if (meta == null) return;

		final String displayName = meta.getDisplayName();
		if (displayName == null) return;

		final FileConfiguration config = Skywars.get().getConfig();
		Material itemType = item.getType();

		if (itemType == getConfiguredMaterial(config, "KIT_SELECTOR") && !arena.started()) {
			event.setCancelled(true);
			KitsMenu.open(player);
		}

		if (itemType == getConfiguredMaterial(config, "LEAVE") && displayName.equals(SkywarsUtils.getItemNameFromConfig("LEAVE"))) {
			if (arena.getStatus() != ArenaStatus.PLAYING || swp.isSpectator()) {
				event.setCancelled(true);
				arena.leavePlayer(swp);
			}
		}

		if (itemType == getConfiguredMaterial(config, "PLAY_AGAIN") && displayName.equals(SkywarsUtils.getItemNameFromConfig("PLAY_AGAIN"))) {
			if (swp.isSpectator()) {
				event.setCancelled(true);
				player.sendMessage(Messager.color("&aSending you to another game..."));
				arena.leavePlayer(swp);
				ArenaManager.joinRandomMap(player);
			}
		}

		if (itemType == getConfiguredMaterial(config, "GAME_OPTIONS") && !swp.isSpectator() && !arena.started()) {
			event.setCancelled(true);
			GameOptionsMenu.open(player);
		}

		if (itemType == getConfiguredMaterial(config, "START_GAME") && !arena.started() && CommandsUtils.permissionCheckWithMessage(player, "skywars.start")) {
			event.setCancelled(true);
			arena.softStart(player);
		}

		if (itemType == getConfiguredMaterial(config, "STOP_GAME") && arena.started() && CommandsUtils.permissionCheckWithMessage(player, "skywars.stop")) {
			event.setCancelled(true);
			arena.clear();
		}
	}


	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);

		if (arena == null)
			handleNormalInteraction(event, player);
		else
			handleArenaInteraction(event, player, arena);
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
