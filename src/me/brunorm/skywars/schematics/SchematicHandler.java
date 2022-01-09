package me.brunorm.skywars.schematics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
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

	public static Vector calculatePositionWithOffset(Map<String, Tag> values, Vector offset) {
		int x = (int) values.get("x").getValue();
		int y = (int) values.get("y").getValue();
		int z = (int) values.get("z").getValue();
		return new Vector(
			x + offset.getBlockX(),
			y + offset.getBlockY(),
			z + offset.getBlockZ());
	}
	
	public static Vector getVector(Map<String, Tag> values) {
		int x = (int) values.get("x").getValue();
		int y = (int) values.get("y").getValue();
		int z = (int) values.get("z").getValue();
		return new Vector(x,y,z);
	}
	
	public static Location calculatePositionWithOffset(Map<String, Tag> values, World world, Vector offset) {
		int x = (int) values.get("x").getValue();
		int y = (int) values.get("y").getValue();
		int z = (int) values.get("z").getValue();
		//System.out.println("schematic values: " + x + ", " + y + ", " + z);
		return new Location(world,
			x + offset.getBlockX(),
			y + offset.getBlockY(),
			z + offset.getBlockZ());
	}
	
	public static HashMap<String, String> materials = new HashMap<String, String>();
	
	public static void loadMaterials() {
		System.out.println("Loading materials...");
		InputStream stream = Skywars.get().getResource("resources/items.tsv");
    	Scanner myReader = new Scanner(stream);
        while (myReader.hasNextLine()) {
          String d = myReader.nextLine();
          String[] s = d.split("\t");
          String _id = s[0];
          String _data = s[1];
          String _mat = s[3].toUpperCase();
          if(Material.matchMaterial(_mat) == null)
        	  _mat = s[2].replaceAll(" ", "_").toUpperCase();
          //if(Material.matchMaterial(_mat) == null)
        	  //System.out.println("Could not find material for " + String.join(", ", s));
          materials.put(_id + ":" + _data, _mat);
        }
        myReader.close();
		System.out.println("Loaded materials");
	}
	
	public static String getMaterialNameByIDAndData(int id, int data) {
		return materials.get(id+":"+data);
	}
	
	public static void clear(Location loc, Schematic schematic) {
		World world = loc.getWorld();
		short length = schematic.getLength();
		short width = schematic.getWidth();
		short height = schematic.getHeight();
		Vector offset = schematic.getOffset();
		
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {
					Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
							z + loc.getZ() + offset.getZ()).getBlock();
					block.setType(XMaterial.AIR.parseMaterial(), true);
				}
			}
		}
	}
	
	public static void rotateBlock(Block block, BlockFace blockFace) {
		try {			
			Class<?> blockClass = Class.forName("org.bukkit.block.Block");
			Method getBlockDataMethod = blockClass.getMethod("getBlockData");
			Method setBlockDataMethod = blockClass.getMethod("setBlockData", BlockData.class);
			BlockData blockData = (BlockData) getBlockDataMethod.invoke(block);
			if (blockData instanceof Directional) {
				//System.out.println(block.getType().toString() + " facing " + blockFace.toString());
				if (blockData instanceof Orientable) {
					((Orientable) blockData).setAxis(convertBlockFaceToAxis(blockFace));
				}
				if (blockData instanceof Rotatable) {
					((Rotatable) blockData).setRotation(blockFace);
				}
				((Directional) blockData).setFacing(blockFace);
				setBlockDataMethod.invoke(block, blockData);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static BlockFace convertNumberToBlockFace(int n) {
		switch(n) {
		case 0:
			return BlockFace.UP;
		case 1:
			return BlockFace.SOUTH;
		case 2:
			return BlockFace.EAST;
		case 3:
			return BlockFace.NORTH;
		case 4:
			return BlockFace.WEST;
		default:
			return BlockFace.UP;
		}
 	}
	
	private static Axis convertBlockFaceToAxis(BlockFace face) {
	    switch (face) {
	        case NORTH:
	        case SOUTH:
	            return Axis.Z;
	        case EAST:
	        case WEST:
	            return Axis.X;
	        case UP:
	        case DOWN:
	            return Axis.Y;
	        default:
	            return Axis.X;
	    }
	}
	
	@SuppressWarnings("deprecation")
	public static void pasteSchematic(Location loc, Schematic schematic) {
		loadMaterials();
		World world = loc.getWorld();
		byte[] blocks = schematic.getBlocks();
		byte[] blockData = schematic.getData();

		short length = schematic.getLength();
		short width = schematic.getWidth();
		short height = schematic.getHeight();

		Vector offset = schematic.getOffset();
		ListTag tileEntities = schematic.getTileEntities();

		ArrayList<Integer> skipped = new ArrayList<>();
		
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {
					int index = y * width * length + z * width + x;
					int id = blocks[index];
					Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
							z + loc.getZ() + offset.getZ()).getBlock();
					if (blocks[index] < 0) {
						skipped.add((int) blocks[index]);
						continue;
					}
					if(XMaterial.isNewVersion()) {
						String name = null;
						switch(id) {
						case 17: // log
							name = getMaterialNameByIDAndData(id, blockData[index]%4);
							break;
						case (byte) 162: // log2
							name = getMaterialNameByIDAndData(id, blockData[index]%2);
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
						if(name != null) {							
							Material mat = Material.getMaterial(name);
							if(mat == null)
								System.out.println("Unknown material: " + name);
							else
								block.setType(mat);
							int n = (int) Math.floor(blockData[index]/4);
							List<Integer> ids = new ArrayList<Integer>();
							ids.add(50);
							ids.add(54);
							ids.add(61);
							ids.add(66);
							// some blocks are not allowed to be facing up, so we skip the first value (up)
							if(ids.contains(id)) n++;
							rotateBlock(block, convertNumberToBlockFace(n));
						} else
							System.out.println("Could not set block for ID "
									+ id + ":" + blockData[index]);
					} else {
						if (!(blocks[index] == 0 && (block.getType() == XMaterial.WATER.parseMaterial()
								|| block.getType() == XMaterial.LAVA.parseMaterial()))) {
							block.setTypeIdAndData(blocks[index], blockData[index], false);
						}
						block.setTypeIdAndData(blocks[index], blockData[index], true);
					}
				}
			}
		}
		
		for(Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if(values.get("id").getValue().equals("Sign")) {
				// TODO: parse sign
				/*
				System.out.println("its a sign");
				int x = (int) values.get("x").getValue();
				int y = (int) values.get("y").getValue();
				int z = (int) values.get("z").getValue();
				Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
						z + loc.getZ() + offset.getZ()).getBlock();
				Sign sign = (Sign) block.getState();
				System.out.println(sign.getBlock());
				System.out.println(values);
				sign.setLine(0, getSignText((String) values.get("Text1").getValue()));
				sign.setLine(1, getSignText((String) values.get("Text2").getValue()));
				sign.setLine(2, getSignText((String) values.get("Text3").getValue()));
				sign.setLine(3, getSignText((String) values.get("Text4").getValue()));
				*/
			}
			int x = (int) values.get("x").getValue();
			int y = (int) values.get("y").getValue();
			int z = (int) values.get("z").getValue();
			Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
					z + loc.getZ() + offset.getZ()).getBlock();
			switch(values.get("id").getValue().toString()) {
			case "Beacon":
				block.setType(XMaterial.BEACON.parseMaterial());
				break;
			case "Chest":
				block.setType(XMaterial.CHEST.parseMaterial());
				break;
			}
		}
	}
	
	@SuppressWarnings("unused")
	public static void pasteSchematic(Location loc, Schem schematic) {
		World world = loc.getWorld();
		byte[] blockData = schematic.getBlockData();
		Map<String, Tag> palette = schematic.getPalette();

		short length = schematic.getLength();
		short width = schematic.getWidth();
		short height = schematic.getHeight();

		Vector offset = schematic.getOffset();
		ListTag tileEntities = schematic.getTileEntities();

		ArrayList<Integer> skipped = new ArrayList<>();
		
		System.out.println("SCHEM DEBUG");
		System.out.println("blockData: " + blockData);
		System.out.println("palette: " + palette);
		
		for(Tag tag : tileEntities.getValue()) {
			@SuppressWarnings("unchecked")
			Map<String, Tag> values = (Map<String, Tag>) tag.getValue();
			if(values.get("id").getValue().equals("Sign")) {
				// TODO: parse sign
			}
			if(values.get("id").getValue().equals("Beacon")) {
				int x = (int) values.get("x").getValue();
				int y = (int) values.get("y").getValue();
				int z = (int) values.get("z").getValue();
				
				Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
						z + loc.getZ() + offset.getZ()).getBlock();
				block.setType(XMaterial.BEACON.parseMaterial());
			}
		}
	}
	
	public static String getSignText(String text) {
		try {
			return text.split("\\{\"extra\":\\[\"")[1].split("\"\\]")[0];
		} catch(Exception e) {
			return "";
		}
	}
	
	public static Schematic loadSchematic(File file) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		NBTInputStream nbtStream = new NBTInputStream(stream);
		
		CompoundTag schematicTag = (CompoundTag) nbtStream.readTag();
		nbtStream.close();
		if (!schematicTag.getName().equals("Schematic")) {
			throw new IllegalArgumentException("Tag \"Schematic\" does not exist or is not first");
		}

		Map<String, Tag> schematic = schematicTag.getValue();
		String materials = getChildTag(schematic, "Materials", StringTag.class).getValue();
		if (materials.equals("Alpha")) {
			// handle schematic file from below 1.13
			if (!schematic.containsKey("Blocks")) {
				throw new IllegalArgumentException("Schematic file is missing a \"Blocks\" tag");
			}
			
			short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
			short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
			short height = getChildTag(schematic, "Height", ShortTag.class).getValue();
			
			int offsetX = getChildTag(schematic, "WEOffsetX", IntTag.class).getValue();
			int offsetY = getChildTag(schematic, "WEOffsetY", IntTag.class).getValue();
			int offsetZ = getChildTag(schematic, "WEOffsetZ", IntTag.class).getValue();
			
			Vector offset = new Vector(offsetX, offsetY, offsetZ);
			
			ListTag tileEntities = getChildTag(schematic, "TileEntities", ListTag.class);
			
			byte[] blocks = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
			byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
			
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
		Tag tag = items.get(key);
		if (!expected.isInstance(tag)) {
			throw new IllegalArgumentException(key + " tag is not of tag type " + expected.getName());
		}
		return expected.cast(tag);
	}
}
