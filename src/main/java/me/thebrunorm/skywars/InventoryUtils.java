package me.thebrunorm.skywars;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryUtils {

	public static Inventory addItem(Inventory inventory, ItemStack item, int slot, String displayName,
			String... loreLines) {
		try {
			final ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(Messager.color(displayName));
			final List<String> lore = new ArrayList<String>();
			for (final String line : loreLines)
				lore.add(Messager.color("&e" + line));
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
