package me.brunorm.skywars;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

public class ChestManager {

	HashMap<String, YamlConfiguration> chestConfigurations = new HashMap<String, YamlConfiguration>();

	public HashMap<String, YamlConfiguration> getChestConfigurations() {
		return this.chestConfigurations;
	}

	public static void fillChest(Location location, boolean overpowered) {
		final Block block = location.getBlock();
		if (block.getType() != XMaterial.CHEST.parseMaterial())
			return;
		final Chest chest = (Chest) block.getState();
		final Inventory inventory = chest.getBlockInventory();
		inventory.clear();

		final YamlConfiguration config = Skywars.get().getChestManager().getChestConfigurations().get("normal");
		for (final Object a : config.getList("items")) {
			final ItemStack item = ConfigurationUtils.parseItemFromConfig(a);
			if (item == null)
				continue;
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), item);
		}
	}

	public void loadChests() {
		Skywars.get().sendDebugMessage("&eLoading chest configurations...");
		this.chestConfigurations.clear();
		final File folder = new File(Skywars.chestsPath);
		if (!folder.exists())
			folder.mkdirs();
		if (folder.listFiles().length <= 0) {
			Skywars.get().sendDebugMessage("&eSetting up default chest configuration.");
			ConfigurationUtils.copyDefaultContentsToFile("chests/normal.yml",
					new File(Skywars.chestsPath, "normal.yml"));
		}
		for (final File file : folder.listFiles()) {
			final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			this.chestConfigurations.put(file.getName().split("\\.(\\w+)$")[0], config);
		}
	}

}
