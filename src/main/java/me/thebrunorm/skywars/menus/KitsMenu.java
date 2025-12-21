/* (C) 2021 Bruno */
package me.thebrunorm.skywars.menus;

import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Kit;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitsMenu implements Listener {

		public static void open(Player player) {
			final Inventory inventory = Bukkit.createInventory(null, 9 * 3, Messager.getMessage("KIT_MENU_TITLE"));
	
			final Economy eco = Skywars.get().getEconomy();
			double playerMoney = 0;
			if (eco != null)
				playerMoney = eco.getBalance(player);
	
			int index = 0;
			for (final Kit kit : Skywars.get().getKits()) {
				final ItemStack item = kit.getIcon();
				final ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(Messager.color("&a" + kit.getDisplayName()));
				final List<String> lore = new ArrayList<>();
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
				if (selected) {
					lore.add(Messager.color(Messager.getMessage("KIT_MENU_SELECTED_KIT")));
	
					// TODO: make it work
					item.addUnsafeEnchantment(Enchantment.LUCK, 1);
					meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				} else if (premium) {
					if (owned)
						lore.add(Messager.color(Messager.getMessage("KIT_MENU_YOU_OWN_THIS_KIT")));
					else if (!owned) {
						if (playerMoney >= kit.getPrice()) {
							lore.add(Messager.color(Messager.getMessage("KIT_MENU_PRICE"), kit.getPrice()));
							lore.add(Messager.color(Messager.getMessage("KIT_MENU_YOUR_MONEY"), playerMoney));
							lore.add(Messager.color(Messager.getMessage("KIT_MENU_CLICK_TO_PURCHASE")));
						} else {
							lore.add(Messager.color(Messager.getMessage("KIT_MENU_NOT_ENOUGH_MONEY_PRICE"), kit.getPrice()));
							lore.add(Messager.color(Messager.getMessage("KIT_MENU_NOT_ENOUGH_MONEY_YOUR_MONEY"), playerMoney));
							lore.add(Messager.color(Messager.getMessage("KIT_MENU_YOU_DONT_HAVE_THIS_KIT")));
						}
					}
				} else if (!premium)
					lore.add(Messager.color(Messager.getMessage("KIT_MENU_THIS_KIT_IS_FREE")));
	
				if (!selected && selectable)
					lore.add(Messager.color(Messager.getMessage("KIT_MENU_CLICK_TO_SELECT")));
	
				meta.setLore(lore);
				item.setItemMeta(meta);
				inventory.setItem(index, item);
	
				index++;
			}
			player.openInventory(inventory);
			PlayerInventoryManager.setMenu(player, MenuType.KIT_SELECTION);
		}
	@EventHandler
	void onClick(InventoryClickEvent event) {
		final Player player = (Player) event.getWhoClicked();
		if (PlayerInventoryManager.getCurrentMenu(player) != MenuType.KIT_SELECTION)
			return;
		event.setCancelled(true);
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null)
			return;
		final String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
		final Kit kit = Skywars.get().getKit(name);
		if (kit == null) {
			player.sendMessage(Messager.getMessage("ERROR_COULD_NOT_SELECT_KIT"));
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
				Skywars.get().savePlayerConfig(player, conf);
				selected = true;
				player.sendMessage(Messager.getMessage("KIT_BOUGHT_FORMAT", name));
			} else {
				player.sendMessage(Messager.getMessage("YOU_DONT_HAVE_THIS_KIT"));
			}
		}
		if (selected) {
			Skywars.get().setPlayerKit(player, kit);
			player.sendMessage(Messager.getMessage("KIT_SELECTED_FORMAT", name));
		}
		open(player);
	}

}
