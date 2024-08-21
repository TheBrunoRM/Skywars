package me.thebrunorm.skywars.schematics;

import java.util.HashMap;

import org.bukkit.util.Vector;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class Schem {

	private final Vector offset;
	private final byte[] blocks;
	private final short width;
	private final short height;
	private final short length;
	private final HashMap<Integer, String> dataMap;
	private final ListTag<CompoundTag> blockEntities;

	public Schem(Vector offset, short width, short length, short height, byte[] blocks,
			HashMap<Integer, String> dataMap, ListTag<CompoundTag> blockEntities) {
		this.offset = offset;
		this.blocks = blocks;
		this.dataMap = dataMap;
		this.blockEntities = blockEntities;
		this.width = width;
		this.height = height;
		this.length = length;
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

	public byte[] getBlockData() {
		return this.blocks;
	}

	public HashMap<Integer, String> getDataMap() {
		return this.dataMap;
	}

	public ListTag<CompoundTag> getBlockEntities() {
		return this.blockEntities;
	}
}