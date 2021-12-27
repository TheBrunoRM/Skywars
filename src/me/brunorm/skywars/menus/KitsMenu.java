package me.brunorm.skywars.menus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Kit;

public class KitsMenu implements Listener {

	static HashMap<Player, Inventory> inventories = new HashMap<Player, Inventory>();
	
	public static void open(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9 * 3, Messager.color("&aKits"));
		inventories.put(player, inventory);
		
		int index = 0;
		for (Kit kit : Skywars.get().getKits()) {
			ItemStack item = kit.getIcon();
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(Messager.color("&a" + kit.getDisplayName()));
			List<String> lore = new ArrayList<String>();
			for(ItemStack i : kit.getItems()) {
				lore.add(Messager.color("&8"
						+ WordUtils.capitalizeFully(i.getType().name().replace("_", " "))));
			}
			lore.add(Messager.color("&r"));
			if (Skywars.get().getPlayerKit(player) == kit)
				lore.add(Messager.color("&6Selected kit"));
			else
				lore.add(Messager.color("&eClick to select!"));
			meta.setLore(lore);
			item.setItemMeta(meta);
			inventory.setItem(index, item);
			
			index++;
		}
		player.openInventory(inventory);
	}
	
	@EventHandler
	void onClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		Inventory inventory = inventories.get(player);
		if (event.getInventory().equals(inventory)) {
			event.setCancelled(true);
			ItemStack clicked = event.getCurrentItem();
			if (clicked == null || clicked.getItemMeta() == null) return;
			String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
			Kit kit = Skywars.get().getKit(name);
			if(kit != null) {
				Skywars.get().setPlayerKit(player, kit);
				player.sendMessage("Selected kit " + name);
			} else {
				player.sendMessage(Messager.color("&cError: &7could not select kit"));
			}
		}
	}
	
}