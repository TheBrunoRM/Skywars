package me.thebrunorm.skywars.events;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.menus.ConfigMenu;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;
import mrblobman.sounds.Sounds;

public class SetupEvents implements Listener {

	public static ItemStack item;

	@EventHandler
	void onDrop(PlayerDropItemEvent event) {
		final Player player = event.getPlayer();
		if (player == null)
			return;
		final Arena arena = ConfigMenu.currentArenas.get(player);
		if (arena == null)
			return;
		final SkywarsMap map = arena.getMap();
		if (map == null)
			return;
		if (event.getItemDrop().getItemStack().equals(item)) {
			item = null;
			player.teleport(ConfigMenu.playerLocations.get(player));
			// Skywars.get().clearArena(arena);
			map.saveParametersInConfig();
			map.saveConfig();
			ConfigMenu.playerLocations.put(player, null);
			ConfigMenu.currentArenas.remove(player);
			event.getItemDrop().remove();
			Skywars.get().NMS().sendTitle(player, Messager.getMessage("SETUP_DONE_TITLE"), Messager.getMessage("SETUP_SPAWNS_SAVED_SUBTITLE"));
			player.playSound(player.getLocation(), Sounds.LEVEL_UP.bukkitSound(), 3, 1);
			player.sendMessage(Messager.getMessage("SETUP_EXIT_SPAWN_EDIT_MODE"));
		}
	}

	@EventHandler
	public void onPlayerItemHoldEvent(PlayerItemHeldEvent event) {
		if (SetupEvents.item == null)
			return;
		final ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if (newItem != null && newItem.equals(SetupEvents.item)) {
			Skywars.get().NMS().sendTitle(event.getPlayer(), Messager.getMessage("SETUP_PLEASE_TITLE"), Messager.getMessage("SETUP_DROP_TO_EXIT_EDIT_MODE"));
		} else {
			Skywars.get().NMS().sendTitle(event.getPlayer(), Messager.getMessage("SETUP_WARNING_TITLE"), Messager.getMessage("SETUP_DROP_TO_EXIT_EDIT_MODE_EXCLAMATION"));
		}
	}

	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (player == null)
			return;
		if (item == null)
			return;
		final Block block = event.getClickedBlock();
		if (block == null)
			return;
		final Arena arena = ConfigMenu.currentArenas.get(player);
		if (arena == null)
			return;
		final SkywarsMap map = arena.getMap();
		if (event.getItem() == null)
			return;
		if (event.getItem().equals(item)) {
			event.setCancelled(true);
			int n = map.getSpawns().size();
			final float t = 1;
			player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), 3, 1 + t);
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				final Location loc = block.getLocation();
				loc.add(new Vector(0.5, 1, 0.5));
				final Vector vector = loc.subtract(arena.getCenterBlock()).toVector();
				Skywars.get().sendDebugMessage("spawn set to vector " + vector);
				map.setSpawn(n, vector);
				Skywars.get().NMS().sendTitle(player, "", Messager.getFormattedMessage("SETUP_SPAWN_SET", player, null, null, n + 1));
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				n--;
				if (map.getSpawns().get(n) == null) {
					Skywars.get().NMS().sendTitle(player, "", Messager.getFormattedMessage("SETUP_NO_SPAWN", player, null, null, n));
					return;
				}
				map.getSpawns().remove(n);
				Skywars.get().NMS().sendTitle(player, "", Messager.getFormattedMessage("SETUP_SPAWN_REMOVED", player, null, null, n + 1));
			}
		}
	}

}
