// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.menus;

import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.singletons.MessageUtils;
import me.thebrunorm.skywars.singletons.SkywarsEconomy;
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

	private static final int INVENTORY_SIZE = 9 * 3; // 3 rows, 9 columns

	@EventHandler
	void onClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (PlayerInventoryManager.getCurrentMenu(player) != MenuType.KIT_SELECTION) return;

		event.setCancelled(true);
		ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null) return;

		String kitName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
		Kit kit = Skywars.get().getKit(kitName);

		if (kit == null) {
			player.sendMessage(MessageUtils.get("select_error"));
			return;
		}

		boolean selected = handleKitSelection(player, kit);
		if (selected) {
			Skywars.get().setPlayerKit(player, kit);
			player.sendMessage(MessageUtils.get("selected", kitName));
		}
		open(player);
	}

	private boolean handleKitSelection(Player player, Kit kit) {
		if (kit.getPrice() <= 0 || Skywars.get().getPlayerConfig(player).getStringList("ownedKits").contains(kit.getName())) {
			return true;
		} else if (SkywarsEconomy.getEconomy().getBalance(player) >= kit.getPrice()) {
			purchaseKit(player, kit);
			return true;
		} else {
			player.sendMessage(MessageUtils.get("unowned"));
			return false;
		}
	}

	public static void open(Player player) {
		Inventory inventory = createInventory();
		populateInventory(inventory, player);
		player.openInventory(inventory);
		PlayerInventoryManager.setMenu(player, MenuType.KIT_SELECTION);
	}

	private void purchaseKit(Player player, Kit kit) {
		YamlConfiguration config = Skywars.get().getPlayerConfig(player);
		List<String> ownedKits = config.getStringList("ownedKits");
		ownedKits.add(kit.getName());
		config.set("ownedKits", ownedKits);
		Skywars.get().savePlayerConfig(player, config);
		player.sendMessage(MessageUtils.get("kits.bought", kit.getDisplayName()));
	}

	private static Inventory createInventory() {
		return Bukkit.createInventory(null, INVENTORY_SIZE, MessageUtils.get("menu_titles.kits"));
	}

	private static void populateInventory(Inventory inventory, Player player) {
		Economy eco = SkywarsEconomy.getEconomy();
		double playerMoney = eco != null ? eco.getBalance(player) : 0;

		int index = 0;
		for (Kit kit : Skywars.get().getKits()) {
			ItemStack item = kit.getIcon();
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(MessageUtils.color("&a" + kit.getDisplayName()));

			List<String> lore = buildKitLore(kit, player, playerMoney);
			meta.setLore(lore);

			if (isSelectedKit(player, kit)) {
				applySelectedKitEnchantment(item, meta);
			}

			item.setItemMeta(meta);
			inventory.setItem(index++, item);
		}
	}

	private static List<String> buildKitLore(Kit kit, Player player, double playerMoney) {
		List<String> lore = new ArrayList<>();
		addKitItemsToLore(kit, lore);
		addKitStatusToLore(kit, player, playerMoney, lore);
		return lore;
	}

	private static boolean isSelectedKit(Player player, Kit kit) {
		return Skywars.get().getPlayerKit(player) == kit;
	}

	private static void applySelectedKitEnchantment(ItemStack item, ItemMeta meta) {
		item.addUnsafeEnchantment(Enchantment.LUCK, 1);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
	}

	private static void addKitItemsToLore(Kit kit, List<String> lore) {
		for (ItemStack i : kit.getItems()) {
			String itemName = WordUtils.capitalizeFully(i.getType().name().replace("_", " "));
			if (i.getAmount() > 1) itemName += " x" + i.getAmount();
			lore.add(MessageUtils.color("&8" + itemName));
		}
		lore.add(MessageUtils.color("&r"));
	}

	private static void addKitStatusToLore(Kit kit, Player player, double playerMoney, List<String> lore) {
		boolean owned = Skywars.get().getPlayerConfig(player).getStringList("ownedKits").contains(kit.getName());
		boolean selected = Skywars.get().getPlayerKit(player) == kit;
		boolean premium = kit.getPrice() > 0;

		if (selected) {
			lore.add(MessageUtils.get("kits.current_selected"));
		} else if (premium) {
			handlePremiumKit(kit, playerMoney, owned, lore);
		} else {
			lore.add(MessageUtils.get("kits.current_free"));
		}

		if (!selected) {
			lore.add(MessageUtils.get("kits.click_select"));
		}
	}

	private static void handlePremiumKit(Kit kit, double playerMoney, boolean owned, List<String> lore) {
		if (owned) {
			lore.add(MessageUtils.get("kits.owned"));
		} else if (playerMoney >= kit.getPrice()) {
			lore.add(MessageUtils.get("kits.price_good", kit.getPrice()));
			lore.add(MessageUtils.get("kits.your_money_more", playerMoney));
			lore.add(MessageUtils.get("kits.click_purchase"));
		} else {
			lore.add(MessageUtils.get("kits.price_bad", kit.getPrice()));
			lore.add(MessageUtils.get("kits.your_money_less", playerMoney));
			lore.add(MessageUtils.get("kits.unowned"));
		}
	}
}
