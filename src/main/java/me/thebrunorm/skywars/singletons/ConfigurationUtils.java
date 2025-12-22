// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.singletons;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.Skywars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

public enum ConfigurationUtils {
	;

	public static final int DEFAULT_BUFFER_SIZE = 8192;

	public static YamlConfiguration loadConfiguration(String fileName, String defaultFileName,
													  String altDefaultFileName) {
		if (Skywars.get().getResource(defaultFileName) == null)
			return loadConfiguration(fileName, altDefaultFileName);
		return createMissingKeys(loadConfiguration(fileName, defaultFileName), getDefaultConfig(altDefaultFileName),
				fileName);
	}

	public static YamlConfiguration loadConfiguration(String fileName, String defaultFileName) {
		final File file = new File(Skywars.get().getDataFolder(), fileName);
		if (!file.exists()) {
			Skywars.get().sendDebugMessage("creating file " + fileName);
			copyDefaultContentsToFile(defaultFileName, file);
		}
		return createMissingKeys(YamlConfiguration.loadConfiguration(file), getDefaultConfig(defaultFileName),
				fileName);
	}

	public static YamlConfiguration createMissingKeys(YamlConfiguration conf, YamlConfiguration defaultConfig,
													  String fileName) {

		try {
			final ConfigurationSection section = defaultConfig.getConfigurationSection("");

			boolean modified = false;

			for (final String key : section.getKeys(true)) {
				if (conf.get(key) == null) {
					Skywars.get().sendMessage("&cWarning: key &b%s &cis missing in file &b%s", key, fileName);
					modified = true;

					// setting the key in the configuration
					// so it uses the default value if is not set
					// but not saving it to prevent removing spaces and comments
					conf.set(key, defaultConfig.get(key));
				}
			}
			if (modified) {
				Skywars.get().sendMessage("&6The plugin will use the default values for the missing keys.");
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return conf;
	}

	public static YamlConfiguration getDefaultConfig(String defaultFileName) {
		final InputStream stream = Skywars.get().getResource(defaultFileName);
		if (stream == null) {
			Skywars.get().sendMessage("Could not get resource: " + defaultFileName);
			return null;
		}
		final Reader defaultConfigStream = new InputStreamReader(stream, StandardCharsets.UTF_8);
		return YamlConfiguration.loadConfiguration(defaultConfigStream);
	}

	public static void copyDefaultContentsToFile(String defaultFileName, File file) {
		try {
			Skywars.get().sendDebugMessage("copying default contents to file " + file.getPath());
			final File parent = new File(file.getParent());
			if (!parent.exists())
				parent.mkdir();
			if (!file.exists())
				file.createNewFile();
			final InputStream stream = Skywars.get().getResource(defaultFileName);
			if (stream == null) {
				Skywars.get().sendMessage("Could not get resource: " + defaultFileName);
				return;
			}
			copyInputStreamToFile(stream, file);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {

		try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
			int read;
			final byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
		}

	}

	public static void saveConfiguration(YamlConfiguration config, String path) {
		try {
			config.save(new File(Skywars.get().getDataFolder(), path));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Location getLocationConfig(ConfigurationSection section, YamlConfiguration config) {
		if (section == null)
			return null;
		String worldName = section.getString("world");
		if (worldName == null)
			worldName = config.getString("worldName");
		if (worldName == null) {
			Skywars.get().sendDebugMessage("warning, getlocationconfig: worldname is null");
			return null;
		}
		final World world = Bukkit.getWorld(worldName);
		if (world != null)
			return getLocationConfig(world, section);
		Skywars.get().sendDebugMessage("warning, getlocationconfig: world is null");
		return null;
	}

	public static Location getLocationConfig(World world, ConfigurationSection section) {
		if (section == null)
			return null;
		return new Location(world, section.getInt("x"), section.getInt("y"), section.getInt("z"));
	}

	@SuppressWarnings("unchecked")
	public static ItemStack parseItemFromConfig(Object a) {
		if (a instanceof String)
			return parseItemFromString((String) a);
		else if (a instanceof HashMap)
			return parseItemFromHashMap((HashMap<Object, Object>) a);
		else return null;
	}

	private static @Nullable ItemStack parseItemFromString(String a) {
		final String[] splitted = a.split("[\\s\\W]+");
		final Optional<XMaterial> xmat = XMaterial.matchXMaterial(splitted[0]);

		if (xmat.isEmpty())
			return null;

		final Material mat = xmat.get().parseMaterial();
		final int amount = splitted.length > 1 ? Integer.parseInt(splitted[1]):mat.getMaxStackSize();
		return new ItemStack(mat, amount);
	}

	private static @Nullable ItemStack parseItemFromHashMap(HashMap<Object, Object> hashmap) {
		if (hashmap.size() == 1) {
			final Entry<Object, Object> b = hashmap.entrySet().iterator().next();
			final Optional<XMaterial> xmat = XMaterial.matchXMaterial(b.getKey().toString());
			if (xmat.isEmpty())
				return null;
			final Material mat = xmat.get().parseMaterial();
			final int amount = Integer.parseInt(b.getValue().toString());
			return new ItemStack(mat, amount);
		}

		final Optional<XMaterial> xmat = XMaterial.matchXMaterial(hashmap.get("type").toString());
		if (xmat.isEmpty())
			return null;

		final Material mat = xmat.get().parseMaterial();
		int amount = mat.getMaxStackSize();
		if (hashmap.containsKey("amount"))
			try {
				amount = Integer.parseInt(hashmap.get("amount").toString());
			} catch (final NumberFormatException ignored) {
			}

		final ItemStack item = new ItemStack(mat, amount);
		final ItemMeta meta = item.getItemMeta();

		if (hashmap.containsKey("name")) {
			final String itemName = hashmap.get("name").toString();
			meta.setDisplayName(MessageUtils.color(itemName));
		}

		final Object lore = hashmap.get("lore");
		if (lore != null) {
			final List<String> loreLines = new ArrayList<>();
			if (lore instanceof String)
				loreLines.add(MessageUtils.color(lore.toString()));
			else if (lore instanceof ArrayList)
				for (final String loreLine : ((ArrayList<String>) lore))
					loreLines.add(MessageUtils.color(loreLine));
			meta.setLore(loreLines);
		}

		item.setItemMeta(meta);
		return item;
	}
}
