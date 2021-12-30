package me.brunorm.skywars.schematics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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

public class SchematicHandler {
	
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
	
	@SuppressWarnings("deprecation")
	public static void pasteSchematic(Location loc, Schematic schematic) {
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
					Block block = new Location(world, x + loc.getX() + offset.getX(), y + loc.getY() + offset.getY(),
							z + loc.getZ() + offset.getZ()).getBlock();
					if (blocks[index] < 0) {
						skipped.add((int) blocks[index]);
						continue;
					}
					if(XMaterial.isNewVersion()) {
						
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
