package me.brunorm.skywars.events;

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

import me.bruno.skywars.menus.ArenaSetupMenu;
import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;
import mrblobman.sounds.Sounds;

public class ArenaSetup implements Listener {

	public static ItemStack item;

	@EventHandler
	void onDrop(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if (player == null)
			return;
		Arena arena = ArenaSetupMenu.currentArenas.get(player);
		if (arena == null)
			return;
		if (event.getItemDrop().getItemStack().equals(item)) {
			player.teleport(ArenaSetupMenu.playerLocations.get(player));
			ArenaSetupMenu.playerLocations.put(player, null);
			ArenaSetupMenu.currentArenas.remove(player);
			event.getItemDrop().remove();
			Skywars.get().NMS().sendTitle(player, "&c&lDISABLED", "&eSpawn edit mode");
			player.playSound(player.getLocation(), Sounds.LEVEL_UP.bukkitSound(), 3, 1);
			player.sendMessage(Messager.color("&e&lYou exited &b&lspawn edit mode"));
		}
	}

	@EventHandler
	public void onPlayerItemHoldEvent(PlayerItemHeldEvent event) {
		if (ArenaSetup.item == null)
			return;
		ItemStack item = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
		if (item != null && item.equals(ArenaSetup.item)) {
			Skywars.get().NMS().sendTitle(event.getPlayer(), "&cWarning!", "&eDrop the blaze rod to exit edit mode!");
		}
	}
	
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (player == null)
			return;
		if (item == null)
			return;
		Block block = event.getClickedBlock();
		if (block == null)
			return;
		Arena arena = ArenaSetupMenu.currentArenas.get(player);
		if (arena == null)
			return;
		if (event.getItem() == null)
			return;
		if (event.getItem().equals(item)) {
			int n = arena.getSpawns().size();
			float t = (float) n / (arena.getMaxPlayers() - 1);
			player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), 3, 1 + t);
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				arena.setSpawn(n, block.getLocation().add(new Vector(0.5, 1, 0.5)));
				Skywars.get().NMS().sendTitle(player, "", String.format("&eSpawn %s set!", n));
				if (n > arena.getMaxPlayers()) {
					player.sendMessage(Messager.colorFormat("&cWarning: spawn overload! &6Max players is set to &b%s&6!"));
				}
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				n--;
				if (!arena.removeSpawn(n)) {
					Skywars.get().NMS().sendTitle(player, "", String.format("&cNo spawn &c!", n));
					return;
				}
				Skywars.get().NMS().sendTitle(player, "", String.format("&cSpawn %s removed!", n));
			}
			event.setCancelled(true);
		}
	}

}
