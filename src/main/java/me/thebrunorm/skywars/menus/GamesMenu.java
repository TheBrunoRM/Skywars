// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.menus;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.structures.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GamesMenu implements Listener {

	public static void open(Player player) {
		final Inventory inventory = Bukkit.createInventory(null, 9 * 3, "Skywars");
		PlayerInventoryManager.setMenu(player, MenuType.GAMES_MENU);
		final ItemStack item = new ItemStack(XMaterial.BOW.parseItem());
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(MessageUtils.color("&aclick to join random game"));
		final List<String> lore = new ArrayList<>();
		lore.add(MessageUtils.color("&eliterally just click this"));
		lore.add(MessageUtils.color("&eto join the game that"));
		lore.add(MessageUtils.color("&ehas the most players in it"));
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
