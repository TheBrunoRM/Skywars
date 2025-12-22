/* (C) 2021 Bruno */
package me.thebrunorm.skywars.schematics;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.Skywars;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.stream.Collectors;

public enum SchematicHandler {
	;

	static Class<?> blockClass;
	static Class<?> slabType;
	static Class<?> blockDataClass;
	static Method getBlockDataMethod;
	static Method setBlockDataMethod;

	public static void initializeReflection() {
		try {
			// available in 1.8
			blockClass = Class.forName("org.bukkit.block.Block");

			// available since 1.13
			slabType = Class.forName("org.bukkit.block.data.type.Slab");
			blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
			getBlockDataMethod = blockClass.getMethod("getBlockData");
			setBlockDataMethod = blockClass.getMethod("setBlockData", blockDataClass);
		} catch (final Exception e) {
		}
	}

	public static Vector getVector(int[] pos) {
		return new Vector(pos[0], pos[1], pos[2]);
	}

	public static Vector getVector(CompoundTag tag) {
		final int x = tag.getInt("x");
		final int y = tag.getInt("y");
		final int z = tag.getInt("z");
		return new Vector(x, y, z);
	}

	public static Vector getVector(Map<String, IntTag> values) {
		final int x = values.get("x").asInt();
		final int y = values.get("y").asInt();
		final int z = values.get("z").asInt();
		return new Vector(x, y, z);
	}

	public static Vector calculatePositionWithOffset(Map<String, IntTag> values, Vector offset) {
		final Vector vector = getVector(values);
		return new Vector(vector.getX() + offset.getBlockX(), vector.getY() + offset.getBlockY(),
				vector.getZ() + offset.getBlockZ());
	}

	public static Location calculatePositionWithOffset(Map<String, IntTag> values, World world, Vector offset) {
		final Vector vector = getVector(values);
		return new Location(world, vector.getX() + offset.getBlockX(), vector.getY() + offset.getBlockY(),
				vector.getZ() + offset.getBlockZ());
	}

	public static HashMap<String, String> materials = new HashMap<>();

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
		final int length = schematic.getLength();
		final int width = schematic.getWidth();
		final int height = schematic.getHeight();
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

	public static void clear(Location loc, Schematic_old schematic) {
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
	public static void pasteSchematic_old(Location loc, Schematic_old schematic) {
		loadMaterials();
		final World world = loc.getWorld();
		final byte[] blocks = schematic.getBlocks();
		final byte[] blockData = schematic.getData();

		final short length = schematic.getLength();
		final short width = schematic.getWidth();
		final short height = schematic.getHeight();

		final Vector offset = schematic.getOffset();
		final ListTag<CompoundTag> tileEntities = schematic.getTileEntities();

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
						if (id == 54)
							Skywars.get().sendDebugMessage("data for chest: " + blockData[index]);
						block.setTypeIdAndData(id, blockData[index], true);
					}
				}
			}
		}

		Skywars.get().sendDebugMessage("Skipped " + skipped.size() + " blocks: "
				+ String.join(", ", skipped.stream().map(a -> a.toString()).collect(Collectors.toList())));

		for (final CompoundTag values : tileEntities) {
			if (values.getString("id").equals("Sign")) {
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
			final int x = values.getInt("x");
			final int y = values.getInt("y");
			final int z = values.getInt("z");
			final Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
					z + loc.getZ() + offset.getZ()).getBlock();
			switch (values.getString("id")) {
			case "Beacon":
				block.setType(XMaterial.BEACON.parseMaterial());
				break;
			case "Chest":
				// block.setType(XMaterial.CHEST.parseMaterial());
				break;
			}
		}
	}

	public static byte getHorizontalIndex(String direction) {
		return getHorizontalIndex(direction, (byte) 0);
	}

	public static byte getHorizontalIndex(String direction, byte offset) {
		switch (direction.toLowerCase()) {
		case "south":
			return (byte) (offset + 1);
		case "west":
			return (byte) (offset + 2);
		case "east":
			return (byte) (offset + 3);
		case "north":
			return (byte) (offset + 4);
		default:
			return 0;
		}
	}

	@SuppressWarnings("deprecation")
	public static void pasteSchematic(Location loc, Schematic schematic) {
		if (XMaterial.isNewVersion()) {
			Skywars.get().sendMessage("Can't paste schematic: schematic files are not supported in 1.13+");
			return;
		}

		final World world = loc.getWorld();
		final byte[] blocks = schematic.getBlocks();
		final byte[] blockData = schematic.getData();
		final HashMap<Integer, String> dataMap = schematic.getDataMap();

		final short length = schematic.getLength();
		final short width = schematic.getWidth();
		final short height = schematic.getHeight();

		final Vector offset = schematic.getOffset();
		final ListTag<CompoundTag> blockEntities = schematic.getBlockEntities();

		final ArrayList<Integer> skipped = new ArrayList<>();

		Skywars.get().sendDebugMessage("size: " + length + ", " + width + ", " + height);

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {
					final int index = y * width * length + z * width + x;
					final int id = blocks[index];
					final Block block = new Location(world, x + loc.getX() + offset.getX(),
							y + loc.getY() + offset.getY(), z + loc.getZ() + offset.getZ()).getBlock();
					// schem file
					if (dataMap != null && blockData == null) {
						final String data = dataMap.get(id);
						final String matName = data.split("minecraft:")[1].split("\\[")[0];
						String[] metadata = {};
						if (data.endsWith("]"))
							metadata = data.split("\\[")[1].split("\\]")[0].split(",");
						final XMaterial xmat = XMaterial.matchXMaterial(matName).get();
						if (xmat == null) {
							Skywars.get().sendDebugMessage("Could not get material for: " + matName);
							continue;
						}
						final Material mat = xmat.parseMaterial();
						// Skywars.get().sendDebugMessage("matName: " + matName);
						// Skywars.get().sendDebugMessage("mat: " + mat);
						if (mat == null) {
							skipped.add(id);
							continue;
						}
						block.setType(mat);
						// Skywars.get().sendDebugMessage("setting block at " + block.getLocation() + "
						// to " + mat);
						for (final String m : metadata) {
							// final String key = m.split("=")[0];
							final String value = m.split("=")[1];
							block.setData(getHorizontalIndex(value, (byte) 2));
							// Skywars.get().sendDebugMessage("setting metadata of " + mat + " to: " +
							// String.join(", ",
							// metadata));
							// block.setMetadata(, new FixedMetadataValue(Skywars.get(), m.split("=")[1]));
						}
					} else {
						// schematic file
						if (blocks[index] < 0) {
							skipped.add((int) blocks[index]);
							continue;
						}
						block.setTypeIdAndData(id, blockData[index], true);
					}
				}
			}
		}

		for (final CompoundTag values : blockEntities) {
			switch (values.getString("id").toLowerCase()) {
			case "sign":
				// TODO: parse sign
				break;
			case "beacon":
				/*
				 * final int x = values.getInt("x"); final int y = values.getInt("y"); final int
				 * z = values.getInt("z");
				 *
				 * final Block block = new Location(world, x + loc.getX() + offset.getX(), y +
				 * loc.getY() + offset.getY(), z + loc.getZ() + offset.getZ()).getBlock();
				 * block.setType(XMaterial.BEACON.parseMaterial());
				 */
				break;
			case "chest":
				break;
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
		final NamedTag schematic = NBTUtil.read(file);

		final CompoundTag compound = (CompoundTag) schematic.getTag();
		final CompoundTag palette = compound.getCompoundTag("Palette");

		final HashMap<Integer, String> dataMap = new HashMap<>();
		if (palette != null) {
			final Iterable<Entry<String, Tag<?>>> set = palette.entrySet();

			for (final Entry<String, Tag<?>> entry : set) {
				final Tag<?> val = entry.getValue();
				if (!(val instanceof IntTag)) {
					Skywars.get().sendDebugMessage("Value in palette is not int: " + val.toString());
					continue;
				}
				final int i = ((IntTag) val).asInt();
				final String data = entry.getKey();
				dataMap.put(i, data);
			}
		}

		final short length = compound.getShort("Length");
		final short width = compound.getShort("Width");
		final short height = compound.getShort("Height");

		final CompoundTag metadata = compound.getCompoundTag("Metadata");
		Vector offset;
		if (metadata != null)
			offset = new Vector(metadata.getInt("WEOffsetX"), metadata.getInt("WEOffsetY"),
					metadata.getInt("WEOffsetZ"));
		else
			offset = new Vector(compound.getInt("WEOffsetX"), compound.getInt("WEOffsetY"),
					compound.getInt("WEOffsetZ"));

		Skywars.get().sendDebugMessage("offset: " + offset);

		byte[] blocks = compound.getByteArray("BlockData");
		// Skywars.get().sendDebugMessage("blocks: " +
		// Arrays.toString(Bytes.asList(blocks).toArray()));
		if (blocks.length <= 0)
			blocks = compound.getByteArray("Blocks");
		// Skywars.get().sendDebugMessage("blocks: " +
		// Arrays.toString(Bytes.asList(blocks).toArray()));

		final byte[] blockData = compound.getByteArray("Data");
		// Skywars.get().sendDebugMessage("block data: " +
		// Arrays.toString(Bytes.asList(blockData).toArray()));

		// TODO check version
		ListTag<?> entitiesTag = compound.getListTag("BlockEntities");
		if (entitiesTag == null)
			entitiesTag = compound.getListTag("TileEntities");

		final ListTag<CompoundTag> entities = entitiesTag.asCompoundTagList();

		return new Schematic(offset, length, width, height, blocks, blockData, dataMap, entities);
	}
}
