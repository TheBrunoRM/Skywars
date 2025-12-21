/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {

	public static Inventory addItem(Inventory inventory, ItemStack item, int slot, String displayName,
			String... loreLines) {
		try {
			final ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(MessageUtils.color(displayName));
			final List<String> lore = new ArrayList<>();
			for (final String line : loreLines)
				lore.add(MessageUtils.color("&e" + line));
			meta.setLore(lore);
			item.setItemMeta(meta);
			inventory.setItem(slot, item);
		} catch (final Exception e) {
			e.printStackTrace();
			Skywars.get().sendDebugMessage("Could not set item for inventory.");
		}
		return inventory;
	}

	public static Inventory addItem(Inventory inventory, Material material, int slot, String displayName,
			String... loreLines) {
		return addItem(inventory, new ItemStack(material), slot, displayName, loreLines);
	}
}
