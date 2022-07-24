package me.brunorm.skywars.schematics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;
import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.Skywars;

public class SchematicHandler {

	static Class<?> blockClass;
	static Class<?> blockDataClass;
	static Class<?> slabType;
	static Method getBlockDataMethod;
	static Method setBlockDataMethod;

	public static void initializeReflection() {
		try {
			blockClass = Class.forName("org.bukkit.block.Block");
			blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
			slabType = Class.forName("org.bukkit.block.data.type.Slab");
			getBlockDataMethod = blockClass.getMethod("getBlockData");
			setBlockDataMethod = blockClass.getMethod("setBlockData", blockDataClass);
		} catch (final Exception e) {
		}
	}

	public static Vector calculatePositionWithOffset(Map<String, Tag> values, Vector offset) {
		final int x = (int) values.get("x").getValue();
		final int y = (int) values.get("y").getValue();
		final int z = (int) values.get("z").getValue();
		return new Vector(x + offset.getBlockX(), y + offset.getBlockY(), z + offset.getBlockZ());
	}

	public static Vector getVector(Map<String, Tag> values) {
		final int x = (int) values.get("x").getValue();
		final int y = (int) values.get("y").getValue();
		final int z = (int) values.get("z").getValue();
		return new Vector(x, y, z);
	}

	public static Location calculatePositionWithOffset(Map<String, Tag> values, World world, Vector offset) {
		final int x = (int) values.get("x").getValue();
		final int y = (int) values.get("y").getValue();
		final int z = (int) values.get("z").getValue();
		// Skywars.get().sendDebugMessage("schematic values: " + x + ", " + y + ", " +
		// z);
		return new Location(world, x + offset.getBlockX(), y + offset.getBlockY(), z + offset.getBlockZ());
	}

	public static HashMap<String, String> materials = new HashMap<String, String>();

	public static void loadMaterials() {
		Skywars.get().sendMessage("Loading materials...");
		final InputStream stream = Skywars.get().getResource("items.tsv");
		final Scanner myReader = new Scanner(stream);
		while (myReader.hasNextLine()) {
			final String d = myReader.nextLine();
			final String[] s = d.split("\t");
			final String _id = s[0];
			final String _data = s[1];
			String _mat = s[3].toUpperCase();
			if (Material.matchMaterial(_mat) == null)
				_mat = s[2].replaceAll(" ", "_").toUpperCase();
			// if(Material.matchMaterial(_mat) == null)
			// Skywars.get().sendDebugMessage("Could not find material for " +
			// String.join(", ", s));
			materials.put(_id + ":" + _data, _mat);
		}
		myReader.close();
		Skywars.get().sendDebugMessage("Loaded materials");
	}

	public static String getMaterialNameByIDAndData(int id, int data) {
		return materials.get(id + ":" + data);
	}

	public static void clear(Location loc, Schematic schematic) {
		if (schematic == null)
			return;

		final World world = loc.getWorld();
		final short length = schematic.getLength();
		final short width = schematic.getWidth();
		final short height = schematic.getHeight();
		final Vector offset = schematic.getOffset();

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {
					final Block block = new Location(world, x + loc.getX() + offset.getX(),
							y + loc.getY() + offset.getY(), z + loc.getZ() + offset.getZ()).getBlock();
					block.setType(XMaterial.AIR.parseMaterial(), true);
				}
			}
		}
	}

	public static String getColor(int id) {
		switch (id) {
		case 0:
			return "WHITE";
		case 1:
			return "ORANGE";
		case 2:
			return "MAGENTA";
		case 3:
			return "LIGHT_BLUE";
		case 4:
			return "YELLOW";
		case 5:
			return "LIME";
		case 6:
			return "PINK";
		case 7:
			return "GRAY";
		case 8:
			return "LIGHT_GRAY";
		case 9:
			return "CYAN";
		case 10:
			return "PURPLE";
		case 11:
			return "BLUE";
		case 12:
			return "BROWN";
		case 13:
			return "GREEN";
		case 14:
			return "RED";
		case 15:
			return "BLACK";
		default:
			return "WHITE";
		}
	}

	public static String getColorableMaterialName(int id) {
		switch (id) {
		case 35:
			return "WOOL";
		case 160:
			return "STAINED_GLASS_PANE";
		default:
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public static void pasteSchematic(Location loc, Schematic schematic) {
		loadMaterials();
		final World world = loc.getWorld();
		final byte[] blocks = schematic.getBlocks();
		final byte[] blockData = schematic.getData();

		final short length = schematic.getLength();
		final short width = schematic.getWidth();
		final short height = schematic.getHeight();

		final Vector offset = schematic.getOffset();
		final ListTag tileEntities = schematic.getTileEntities();

		final ArrayList<Integer> skipped = new ArrayList<>();

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {
					final int index = y * width * length + z * width + x;
					final int id = blocks[index];
					final Block block = new Location(world, x + loc.getX() + offset.getX(),
							y + loc.getY() + offset.getY(), z + loc.getZ() + offset.getZ()).getBlock();
					if (blocks[index] < 0) {
						skipped.add((int) blocks[index]);
						continue;
					}
					if (XMaterial.isNewVersion()) {
						// 1.13+ method for setting blocks
						String name = null;
						switch (id) {
						case 17: // log
							name = getMaterialNameByIDAndData(id, blockData[index] % 4);
							break;
						case (byte) 162: // log2
							name = getMaterialNameByIDAndData(id, blockData[index] % 2);
							break;
						case 50: // torch
						case 54: // chest
						case 61: // furnace
						case 66: // rail
							name = getMaterialNameByIDAndData(id, 0);
							break;
						default:
							name = getMaterialNameByIDAndData(id, blockData[index]);
						}
						if (name != null) {
							boolean doubleSlab = false;
							if (name.startsWith("DOUBLE") && name.endsWith("SLAB")) {
								name = getMaterialNameByIDAndData(id + 1, blockData[index]);
								doubleSlab = true;
							}
							Material mat = Material.getMaterial(name);
							if (mat != null) {
								// torches are not marked as "WALL_TORCH" in the schematic
								// so we check for the data value to set it ourselves
								if (mat == Material.valueOf("TORCH") && blockData[index] > 0)
									mat = Material.valueOf("WALL_TORCH");
								final String m = getColorableMaterialName(id);
								// if the material is colorizable (e.g. wool)
								// then get the color from the id (e.g. 14 => red)
								// and get the full material name (e.g. RED_WOOL)
								if (m != null && blockData[index] > 0)
									mat = Material.valueOf(getColor(blockData[index]) + "_" + m);
								block.setType(mat);
								if (doubleSlab) {
									try {
										final Object slab = getBlockDataMethod.invoke(block);
										final Method setTypeMethod = slabType.getMethod("setType",
												slabType.getDeclaredField("Type").getClass());
										setTypeMethod.invoke(slab, slabType.getDeclaredField("Type").getClass()
												.getDeclaredField("DOUBLE"));
										setBlockDataMethod.invoke(block, slab);
									} catch (final Exception e) {
										e.printStackTrace();
									}
								} else {
									final BlockState state = block.getState();
									state.setData(new MaterialData(mat, blockData[index]));
									state.update();
								}
							} else {
								Skywars.get().sendMessage("null material for %s, %s:%s", name, id, blockData[index]);
							}
						} else {
							Skywars.get().sendMessage("null name for %s:%s", id, blockData[index]);
						}
					} else {
						// 1.8 - 1.12 method for setting blocks
						block.setTypeIdAndData(id, blockData[index], true);
					}
				}
			}
		}

		Skywars.get().sendMessage("Skipped " + skipped.size() + " blocks.");

		for (final Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			final Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Sign")) {
				// TODO: parse sign
				/*
				 * //Skywars.get().sendDebugMessage("its a sign"); int x = (int)
				 * values.get("x").getValue(); int y = (int) values.get("y").getValue(); int z =
				 * (int) values.get("z").getValue(); Block block = new Location(world, x +
				 * loc.getX() + offset.getX(), y + loc.getY() + offset.getY(), z + loc.getZ() +
				 * offset.getZ()).getBlock(); Sign sign = (Sign) block.getState();
				 * //Skywars.get().sendDebugMessage(sign.getBlock());
				 * //Skywars.get().sendDebugMessage(values); for(int i = 0; i < 4; i++) {
				 * sign.setLine(i, getSignText((String) values.get("Text"+i).getValue())); }
				 */
			}
			final int x = (int) values.get("x").getValue();
			final int y = (int) values.get("y").getValue();
			final int z = (int) values.get("z").getValue();
			final Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
					z + loc.getZ() + offset.getZ()).getBlock();
			switch (values.get("id").getValue().toString()) {
			case "Beacon":
				block.setType(XMaterial.BEACON.parseMaterial());
				break;
			case "Chest":
				// block.setType(XMaterial.CHEST.parseMaterial());
				break;
			}
		}
	}

	@SuppressWarnings("unused")
	public static void pasteSchematic(Location loc, Schem schematic) {
		final World world = loc.getWorld();
		final byte[] blockData = schematic.getBlockData();
		final Map<String, Tag> palette = schematic.getPalette();

		final short length = schematic.getLength();
		final short width = schematic.getWidth();
		final short height = schematic.getHeight();

		final Vector offset = schematic.getOffset();
		final ListTag tileEntities = schematic.getTileEntities();

		final ArrayList<Integer> skipped = new ArrayList<>();

		Skywars.get().sendDebugMessage("SCHEM DEBUG");
		Skywars.get().sendDebugMessage("blockData: " + blockData);
		Skywars.get().sendDebugMessage("palette: " + palette);

		for (final Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			final Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if (values.get("id").getValue().equals("Sign")) {
				// TODO: parse sign
			}
			if (values.get("id").getValue().equals("Beacon")) {
				final int x = (int) values.get("x").getValue();
				final int y = (int) values.get("y").getValue();
				final int z = (int) values.get("z").getValue();

				final Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
						z + loc.getZ() + offset.getZ()).getBlock();
				block.setType(XMaterial.BEACON.parseMaterial());
			}
		}
	}

	public static String getSignText(String text) {
		try {
			return text.split("\\{\"extra\":\\[\"")[1].split("\"\\]")[0];
		} catch (final Exception e) {
			return "";
		}
	}

	public static Schematic loadSchematic(File file) throws IOException {
		final FileInputStream stream = new FileInputStream(file);
		final NBTInputStream nbtStream = new NBTInputStream(stream);

		final CompoundTag schematicTag = (CompoundTag) nbtStream.readTag();
		nbtStream.close();
		if (!schematicTag.getName().equals("Schematic")) {
			throw new IllegalArgumentException("Tag \"Schematic\" does not exist or is not first");
		}

		final Map<String, Tag> schematic = schematicTag.getValue();
		final String materials = getChildTag(schematic, "Materials", StringTag.class).getValue();
		if (materials.equals("Alpha")) {
			// handle schematic file from below 1.13
			if (!schematic.containsKey("Blocks")) {
				throw new IllegalArgumentException("Schematic file is missing a \"Blocks\" tag");
			}

			final short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
			final short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
			final short height = getChildTag(schematic, "Height", ShortTag.class).getValue();

			final int offsetX = getChildTag(schematic, "WEOffsetX", IntTag.class).getValue();
			final int offsetY = getChildTag(schematic, "WEOffsetY", IntTag.class).getValue();
			final int offsetZ = getChildTag(schematic, "WEOffsetZ", IntTag.class).getValue();

			final Vector offset = new Vector(offsetX, offsetY, offsetZ);

			final ListTag tileEntities = getChildTag(schematic, "TileEntities", ListTag.class);

			final byte[] blocks = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
			final byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();

			return new Schematic(blocks, blockData, width, length, height, offset, tileEntities);
		}
		return null;
	}

	/**
	 * Get child tag of a NBT structure.
	 *
	 * @param items    The parent tag map
	 * @param key      The name of the tag to get
	 * @param expected The expected type of the tag
	 * @return child tag casted to the expected type
	 * @throws DataException if the tag does not exist or the tag is not of the
	 *                       expected type
	 */
	private static <T extends Tag> T getChildTag(Map<String, Tag> items, String key, Class<T> expected)
			throws IllegalArgumentException {
		if (!items.containsKey(key)) {
			throw new IllegalArgumentException("Schematic file is missing a \"" + key + "\" tag");
		}
		final Tag tag = items.get(key);
		if (!expected.isInstance(tag)) {
			throw new IllegalArgumentException(key + " tag is not of tag type " + expected.getName());
		}
		return expected.cast(tag);
	}
}
