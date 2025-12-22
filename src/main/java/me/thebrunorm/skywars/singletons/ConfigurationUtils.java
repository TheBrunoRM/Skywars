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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
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
		Vector vector = getVectorFromConfigSection(section);
		return new Location(world, vector.getX(), vector.getY(), vector.getZ());
	}

	public static Vector getVectorFromConfigSection(ConfigurationSection section) {
		return new Vector(section.getInt("x"), section.getInt("y"), section.getInt("z"));
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

		if (!xmat.isPresent()) return null;

		final Material mat = xmat.get().parseMaterial();
		if (mat == null) return null;

		final int amount = getItemAmountOrDefault(splitted[1], mat);
		return new ItemStack(mat, amount);
	}

	@SuppressWarnings("unchecked")
	private static @Nullable ItemStack parseItemFromHashMap(HashMap<Object, Object> hashmap) {

		if (hashmap.size() == 1) {
			final Entry<Object, Object> b = hashmap.entrySet().iterator().next();
			final Optional<XMaterial> xmat = XMaterial.matchXMaterial(b.getKey().toString());
			if (!xmat.isPresent()) return null;

			final Material mat = xmat.get().parseMaterial();
			final int amount = Integer.parseInt(b.getValue().toString());
			return new ItemStack(mat, amount);
		}

		final Optional<XMaterial> xmat = XMaterial.matchXMaterial(hashmap.get("type").toString());

		Material mat = xmat.isPresent() ? xmat.get().parseMaterial() : Material.BEDROCK;
		int amount = getItemAmountOrDefault(hashmap.get("amount").toString(), mat);

		ItemStack item = new ItemStack(mat, amount);
		ItemMeta meta = item.getItemMeta();

		if (hashmap.containsKey("name"))
			meta.setDisplayName(MessageUtils.color(hashmap.get("name").toString()));

		if (hashmap.containsKey("lore")) {
			List<String> lore = new ArrayList<>();
			Object loreObj = hashmap.get("lore");

			if (loreObj instanceof List<?>)
				for (Object line : (List<?>) loreObj)
					lore.add(MessageUtils.color(line.toString()));
			else
				lore.add(MessageUtils.color(loreObj.toString()));

			meta.setLore(lore);
		}

		if (hashmap.containsKey("enchants")) {
			HashMap<String, Object> enchants = (HashMap<String, Object>) hashmap.get("enchants");
			for (Entry<String, Object> e : enchants.entrySet()) {
				Enchantment enchant = Enchantment.getByName(e.getKey().toUpperCase());
				if (enchant != null)
					meta.addEnchant(enchant, Integer.parseInt(e.getValue().toString()), true);
			}
		}

		if (meta instanceof PotionMeta) {

			PotionMeta potionMeta = (PotionMeta) meta;

			// Custom effects
			if (hashmap.containsKey("effects")) {
				HashMap<String, Object> effects = (HashMap<String, Object>) hashmap.get("effects");

				for (Entry<String, Object> e : effects.entrySet()) {
					PotionEffectType effect = PotionEffectType.getByName(e.getKey().toUpperCase());
					if (effect != null) {
						int level = Integer.parseInt(e.getValue().toString()) - 1;
						potionMeta.addCustomEffect(
								new PotionEffect(effect, 20 * 30, level),
								true
						);
					}
				}
			}
		}

		item.setItemMeta(meta);
		return item;
	}

	static int getItemAmountOrDefault(String text, Material mat) {
		if (!text.isEmpty())
			try {
				return Integer.parseInt(text);
			} catch (Exception ignored) {
			}
		return mat.getMaxStackSize();
	}

}
