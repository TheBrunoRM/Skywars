package me.brunorm.skywars.events;

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

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.menus.ConfigMenu;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;
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
			Skywars.get().NMS().sendTitle(player, "&a&LDONE", "&eSaved spawns");
			player.playSound(player.getLocation(), Sounds.LEVEL_UP.bukkitSound(), 3, 1);
			player.sendMessage(Messager.color("&e&lYou exited &b&lspawn edit mode"));
		}
	}

	@EventHandler
	public void onPlayerItemHoldEvent(PlayerItemHeldEvent event) {
		if (SetupEvents.item == null)
			return;
		final ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if (newItem.equals(SetupEvents.item)) {
			Skywars.get().NMS().sendTitle(event.getPlayer(), "&6Please", "&eDrop the blaze rod to exit edit mode.");
		} else {
			Skywars.get().NMS().sendTitle(event.getPlayer(), "&cWarning!", "&eDrop the blaze rod to exit edit mode!");
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
				final Vector vector = loc.subtract(arena.getLocation()).toVector();
				Skywars.get().sendDebugMessage("spawn set to vector " + vector);
				map.setSpawn(n, vector);
				Skywars.get().NMS().sendTitle(player, "", String.format("&eSpawn %s set!", n + 1));
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				n--;
				if (map.getSpawns().get(n) == null) {
					Skywars.get().NMS().sendTitle(player, "", String.format("&cNo spawn &c!", n));
					return;
				}
				map.getSpawns().remove(n);
				Skywars.get().NMS().sendTitle(player, "", String.format("&cSpawn %s removed!", n + 1));
			}
		}
	}

}
