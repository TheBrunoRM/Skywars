package me.brunorm.skywars.menus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.SkywarsMap;

public class MapMenu implements Listener {
	
	static HashMap<Player, Inventory> inventories = new HashMap<Player, Inventory>();
	
	public static void open(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9 * 6, Messager.color("&aMaps"));
		inventories.put(player, inventory);

		int index = 10;
		for (SkywarsMap map : Skywars.get().getMaps()) {
			//System.out.println("current index: " + index);
			if((index+1)%9==0)index+=2;
			List<String> lore = new ArrayList<String>();
			lore.clear();
			//lore.add(Messager.color("&7"));
			
			//lore.add(Messager.color("&8Solo Insane"));
			//lore.add(Messager.color("&7"));
			//lore.add(Messager.color("&7Servidores Disponibles: &a1"));
			//lore.add(Messager.color("&7Veces Unidas: &a0"));
			//lore.add(Messager.color("&7Selecciones de Mapa: &a1"));
			lore.add(Messager.color("&eClick to play!"));
			//lore.add(Messager.color("&eClick derecho para alternarlo como favorito!"));
			
			ItemStack item = new ItemStack(XMaterial.FIREWORK_STAR.parseItem());
			ItemMeta meta = item.getItemMeta();
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			
			meta.setDisplayName(Messager.colorFormat("&a%s", map.getName()));
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
			SkywarsMap map = Skywars.get().getMap(name);
			Skywars.get().joinMap(map, player);
		}
	}

}
