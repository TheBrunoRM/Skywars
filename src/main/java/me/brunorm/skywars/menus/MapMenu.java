package me.brunorm.skywars.menus;

import java.util.ArrayList;
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
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;

public class MapMenu implements Listener {

	public static void open(Player player) {
		final Inventory inventory = Bukkit.createInventory(null, 9 * 6, Messager.color("&aMaps"));
		PlayerInventoryManager.setInventory(player, inventory);

		int index = 10;
		for (final SkywarsMap map : Skywars.get().getMaps()) {
			// Skywars.get().sendDebugMessage("current index: " + index);
			if ((index + 1) % 9 == 0)
				index += 2;
			final List<String> lore = new ArrayList<String>();
			lore.clear();
			// lore.add(Messager.color("&7"));

			// lore.add(Messager.color("&8Solo Insane"));
			// lore.add(Messager.color("&7"));
			// lore.add(Messager.color("&7Servidores Disponibles: &a1"));
			// lore.add(Messager.color("&7Veces Unidas: &a0"));
			// lore.add(Messager.color("&7Selecciones de Mapa: &a1"));

			final ArrayList<Arena> arenas = Skywars.get().getArenasByMap(map);
			final int players = arenas.stream().map(arena -> arena.getAlivePlayerCount()).reduce(0, (a, b) -> a + b);

			lore.add(Messager.colorFormat("&eCurrent arenas: &a%s", Skywars.get().getArenasByMap(map).size()));
			lore.add(Messager.colorFormat("&eCurrent players: &a%s", players));
			lore.add(Messager.color("&eClick to play!"));
			// lore.add(Messager.color("&eClick derecho para alternarlo como favorito!"));

			final ItemStack item = new ItemStack(XMaterial.FIREWORK_STAR.parseItem());
			final ItemMeta meta = item.getItemMeta();
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
		final Player player = (Player) event.getWhoClicked();
		final Inventory inventory = PlayerInventoryManager.getInventory(player);
		if (event.getInventory().equals(inventory)) {
			event.setCancelled(true);
			final ItemStack clicked = event.getCurrentItem();
			if (clicked == null || clicked.getItemMeta() == null)
				return;
			final String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
			final SkywarsMap map = Skywars.get().getMap(name);
			Skywars.get().joinMap(map, player);
		}
	}

}
