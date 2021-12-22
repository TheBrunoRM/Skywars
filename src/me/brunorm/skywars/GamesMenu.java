package me.brunorm.skywars;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

public class GamesMenu implements Listener {
	
	@SuppressWarnings("deprecation")
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			if (player.getItemInHand().getType() == XMaterial.BOW.parseMaterial()) {
				if (arena.getStatus() != ArenaStatus.PLAYING) {		
					KitsMenu.open(player);
				}
			}
			if (player.getItemInHand().getType() == XMaterial.RED_BED.parseMaterial()) {
				if (arena.getStatus() != ArenaStatus.PLAYING
						|| arena.getPlayer(player).isSpectator()) {
					arena.LeavePlayer(arena.getPlayer(player));
				}
			}
		}
	}
	
	static void OpenMenu(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9 * 3, "Skywars");
		PlayerInventoryManager.setInventory(player, inventory);
		ItemStack item = new ItemStack(XMaterial.BOW.parseItem());
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("�aclick to join random game");
		List<String> lore = new ArrayList<String>();
		lore.add(Messager.color("&eliterally just click this"));
		lore.add(Messager.color("&eto join the game that"));
		lore.add(Messager.color("&ehas the most players in it"));
		meta.setLore(lore);
		item.setItemMeta(meta);
		inventory.setItem(11, item);
		player.openInventory(inventory);
	}
	
	@EventHandler
	void onClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if(player == null) return;
		Inventory inventory = PlayerInventoryManager.getInventory(player);
		if(inventory == null) return;
		if (event.getInventory().getName().equals(inventory.getName())) {
			ItemStack clicked = event.getCurrentItem();
			if (clicked.getType() == XMaterial.BOW.parseMaterial()) {
				event.setCancelled(true);
				Arena arena = Skywars.get().getRandomJoinableArena();
				if (arena == null) {
					player.sendMessage("couldn't find joinable arena");
					return;
				}
				arena.JoinPlayer(player);
			}
		}
	}

}