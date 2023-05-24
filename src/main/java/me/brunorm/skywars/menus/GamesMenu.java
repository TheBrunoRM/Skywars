package me.brunorm.skywars.menus;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;

public class GamesMenu implements Listener {

	public static void open(Player player) {
		final Inventory inventory = Bukkit.createInventory(null, 9 * 3, "Skywars");
		PlayerInventoryManager.setMenu(player, MenuType.GAMES_MENU);
		final ItemStack item = new ItemStack(XMaterial.BOW.parseItem());
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Messager.color("&aclick to join random game"));
		final List<String> lore = new ArrayList<String>();
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
		final Player player = (Player) event.getWhoClicked();
		if (player == null)
			return;
		final Inventory inv = event.getInventory();
		if (inv == null)
			return;
		final MenuType menu = PlayerInventoryManager.getCurrentMenu(player);
		if (menu != MenuType.GAMES_MENU)
			return;
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null)
			return;
		if (clicked.getType() == XMaterial.BOW.parseMaterial()) {
			event.setCancelled(true);
			final Arena arena = Skywars.get().getRandomJoinableArena();
			if (arena == null) {
				player.sendMessage("couldn't find joinable arena");
				return;
			}
			arena.joinPlayer(player);
		}
	}

}