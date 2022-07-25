package me.brunorm.skywars;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

public class InventoryUtils {

	public static void addItem(Inventory inventory, String displayName) {
		addItem(inventory, displayName, Math.round(inventory.getSize() / 2));
	}

	public static void addItem(Inventory inventory, String displayName, int slot) {
		addItem(inventory, displayName, slot, XMaterial.BEDROCK.parseItem());
	}

	public static void addItem(Inventory inventory, String displayName, int slot, ItemStack itemStack) {
		addItem(inventory, displayName, slot, itemStack, 1);
	}

	public static void addItem(Inventory inventory, String displayName, int slot, ItemStack itemStack, int amount) {
		addItem(inventory, displayName, slot, itemStack, amount, new ArrayList<String>());
	}

	public static void addItem(Inventory inventory, String displayName, int slot, ItemStack item, int amount,
			List<String> lore) {
		item.setAmount(amount);
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Messager.color(displayName));
		meta.setLore(lore);
		item.setItemMeta(meta);
		inventory.setItem(slot, item);
	}
}
