package me.brunorm.skywars.menus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
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
		final Inventory inventory = Bukkit.createInventory(null, 9 * 3, Messager.color("&aKits"));
		inventories.put(player, inventory);

		final double playerMoney = Skywars.get().getEconomy().getBalance(player);

		int index = 0;
		for (final Kit kit : Skywars.get().getKits()) {
			final ItemStack item = kit.getIcon();
			final ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(Messager.color("&a" + kit.getDisplayName()));
			final List<String> lore = new ArrayList<String>();
			for (final ItemStack i : kit.getItems()) {
				String itemName = WordUtils.capitalizeFully(i.getType().name().replace("_", " "));
				if (i.getAmount() > 1)
					itemName += " x" + i.getAmount();
				lore.add(Messager.color("&8" + itemName));
			}
			lore.add(Messager.color("&r"));
			final boolean owned = Skywars.get().getPlayerConfig(player).getStringList("ownedKits")
					.contains(kit.getName());
			final boolean selected = Skywars.get().getPlayerKit(player) == kit;
			final boolean premium = kit.getPrice() > 0;
			final boolean selectable = true;
			if (selected)
				lore.add(Messager.color("&6Selected kit"));
			else if (premium) {
				if (owned)
					lore.add(Messager.color("&aYou own this kit!"));
				else if (!owned) {
					if (playerMoney >= kit.getPrice()) {
						lore.add(Messager.color("&aPrice: " + kit.getPrice()));
						lore.add(Messager.color("&6Your money: " + playerMoney));
						lore.add(Messager.color("&aClick to purchase this kit!"));
					} else {
						lore.add(Messager.color("&cPrice: " + kit.getPrice()));
						lore.add(Messager.color("&6Your money: " + playerMoney));
						lore.add(Messager.color("&cYou don't have this kit!"));
					}
				}
			} else if (!premium)
				lore.add(Messager.color("&aThis kit is free!"));
			if (!selected && selectable) {
				lore.add(Messager.color("&eClick to select!"));
			}
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
		final Inventory inventory = inventories.get(player);
		if (!event.getInventory().equals(inventory))
			return;
		event.setCancelled(true);
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null)
			return;
		final String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
		final Kit kit = Skywars.get().getKit(name);
		if (kit == null) {
			player.sendMessage(Messager.color("&cError: &7could not select kit"));
			return;
		}
		boolean selected = false;
		if (kit.getPrice() <= 0) {
			selected = true;
		} else {
			if (Skywars.get().getPlayerConfig(player).getStringList("ownedKits").contains(kit.getName())) {
				selected = true;
			} else if (Skywars.get().getEconomy().getBalance(player) >= kit.getPrice()) {
				final YamlConfiguration conf = Skywars.get().getPlayerConfig(player);
				final List<String> list = conf.getStringList("ownedKits");
				list.add(kit.getName());
				conf.set("ownedKits", list);
				Skywars.get().savePlayerConfig(player);
				selected = true;
				player.sendMessage(Messager.color("&bBought kit &e" + name));
			} else {
				player.sendMessage(Messager.color("&cYou don't have this kit!"));
			}
		}
		if (selected) {
			Skywars.get().setPlayerKit(player, kit);
			player.sendMessage(Messager.color("&aSelected kit &e" + name));
		}
		open(player);
	}

}
