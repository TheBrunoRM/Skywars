/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

public class ConfigurationUtils {

	public static final int DEFAULT_BUFFER_SIZE = 8192;

	private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {

		try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
			int read;
			final byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			outputStream.close();
		}

	}

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

	static YamlConfiguration getDefaultConfig(String defaultFileName) {
		try {
			final InputStream stream = Skywars.get().getResource(defaultFileName);
			if (stream == null) {
				Skywars.get().sendMessage("Could not get resource: " + defaultFileName);
				return null;
			}
			final Reader defaultConfigStream = new InputStreamReader(stream, "UTF-8");
			return YamlConfiguration.loadConfiguration(defaultConfigStream);
		} catch (final UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
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
		final Location loc = new Location(world, section.getInt("x"), section.getInt("y"), section.getInt("z"));
		return loc;
	}

	@SuppressWarnings("unchecked")
	public static ItemStack parseItemFromConfig(Object a) {
		if (a instanceof String) {
			final String[] splitted = ((String) a).split("[\\s\\W]+");
			final Optional<XMaterial> xmat = XMaterial.matchXMaterial(splitted[0]);
			if (!xmat.isPresent())
				return null;
			final Material mat = xmat.get().parseMaterial();
			final int amount = splitted.length > 1 ? Integer.parseInt(splitted[1]) : mat.getMaxStackSize();
			return new ItemStack(mat, amount);
		} else if (a instanceof HashMap) {
			final HashMap<Object, Object> hash = (HashMap<Object, Object>) a;
			if (hash.size() == 1) {
				final Entry<Object, Object> b = hash.entrySet().iterator().next();
				final Optional<XMaterial> xmat = XMaterial.matchXMaterial(b.getKey().toString());
				if (xmat == null)
					return null;
				final Material mat = xmat.get().parseMaterial();
				final int amount = Integer.parseInt(b.getValue().toString());
				return new ItemStack(mat, amount);
			} else {
				final Optional<XMaterial> xmat = XMaterial.matchXMaterial(hash.get("type").toString());
				if (xmat == null)
					return null;
				final Material mat = xmat.get().parseMaterial();
				int amount = mat.getMaxStackSize();
				if (hash.containsKey("amount"))
					try {
						amount = Integer.parseInt(hash.get("amount").toString());
					} catch (final NumberFormatException e) {
					}

				final ItemStack item = new ItemStack(mat, amount);
				final ItemMeta meta = item.getItemMeta();

				if (hash.containsKey("name")) {
					final String itemName = hash.get("name").toString();
					meta.setDisplayName(Messager.color(itemName));
				}

				final Object lore = hash.get("lore");
				if (lore != null) {
					final List<String> loreLines = new ArrayList<String>();
					if (lore instanceof String)
						loreLines.add(Messager.color(lore.toString()));
					else if (lore instanceof ArrayList)
						for (final String loreLine : ((ArrayList<String>) lore))
							loreLines.add(Messager.color(loreLine));
					meta.setLore(loreLines);
				}

				item.setItemMeta(meta);
				return item;
			}
		}
		return null;
	}
}
