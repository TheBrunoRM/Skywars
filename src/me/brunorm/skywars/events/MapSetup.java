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

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.menus.MapSetupMenu;
import me.brunorm.skywars.structures.SkywarsMap;
import mrblobman.sounds.Sounds;

public class MapSetup implements Listener {

	public static ItemStack item;

	@EventHandler
	void onDrop(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if (player == null)
			return;
		SkywarsMap map = MapSetupMenu.currentMaps.get(player);
		if (map == null)
			return;
		if (event.getItemDrop().getItemStack().equals(item)) {
			player.teleport(MapSetupMenu.playerLocations.get(player));
			MapSetupMenu.playerLocations.put(player, null);
			MapSetupMenu.currentMaps.remove(player);
			event.getItemDrop().remove();
			Skywars.get().NMS().sendTitle(player, "&c&lDISABLED", "&eSpawn edit mode");
			player.playSound(player.getLocation(), Sounds.LEVEL_UP.bukkitSound(), 3, 1);
			player.sendMessage(Messager.color("&e&lYou exited &b&lspawn edit mode"));
		}
	}

	@EventHandler
	public void onPlayerItemHoldEvent(PlayerItemHeldEvent event) {
		if (MapSetup.item == null)
			return;
		ItemStack item = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
		if (item != null && item.equals(MapSetup.item)) {
			Skywars.get().NMS().sendTitle(event.getPlayer(), "&cWarning!", "&eDrop the blaze rod to exit edit mode!");
		}
	}
	
	@SuppressWarnings("unused")
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
		SkywarsMap map = MapSetupMenu.currentMaps.get(player);
		if (map == null)
			return;
		if (event.getItem() == null)
			return;
		if (event.getItem().equals(item)) {
			int n = map.getSpawns().size();
			float t = (float) n / (map.getMaxPlayers() - 1);
			player.playSound(player.getLocation(), Sounds.NOTE_PLING.bukkitSound(), 3, 1 + t);
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				map.setSpawn(n, block.getLocation().add(new Vector(0.5, 1, 0.5)));
				Skywars.get().NMS().sendTitle(player, "", String.format("&eSpawn %s set!", n));
				if (n > map.getMaxPlayers()) {
					player.sendMessage(Messager.colorFormat("&cWarning: spawn overload! &6Max players is set to &b%s&6!"));
				}
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				n--;
				if (false/*!map.removeSpawn(n)*/) {
					Skywars.get().NMS().sendTitle(player, "", String.format("&cNo spawn &c!", n));
					return;
				}
				Skywars.get().NMS().sendTitle(player, "", String.format("&cSpawn %s removed!", n));
			}
			event.setCancelled(true);
		}
	}

}