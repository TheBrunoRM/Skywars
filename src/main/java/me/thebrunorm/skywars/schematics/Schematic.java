package me.thebrunorm.skywars.schematics;

import java.util.HashMap;

import org.bukkit.util.Vector;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class Schematic {

	private final Vector offset;
	private final short width;
	private final short height;
	private final short length;
	private final byte[] blocks;
	private final byte[] data;
	private final HashMap<Integer, String> dataMap;
	private final ListTag<CompoundTag> entities;

	public Schematic(Vector offset, short width, short length, short height, byte[] blocks, byte[] data,
			HashMap<Integer, String> dataMap, ListTag<CompoundTag> entities) {
		this.offset = offset;
		this.width = width;
		this.height = height;
		this.length = length;
		this.blocks = blocks;
		this.data = data;
		this.dataMap = dataMap;
		this.entities = entities;
	}

	public short getWidth() {
		return this.width;
	}

	public short getLength() {
		return this.length;
	}

	public short getHeight() {
		return this.height;
	}

	public Vector getOffset() {
		return this.offset;
	}

	public byte[] getData() {
		return this.data;
	}

	public byte[] getBlocks() {
		return this.blocks;
	}

	public HashMap<Integer, String> getDataMap() {
		return this.dataMap;
	}

	public ListTag<CompoundTag> getBlockEntities() {
		return this.entities;
	}
}