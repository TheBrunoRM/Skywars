package me.brunorm.skywars.schematics;

import org.bukkit.util.Vector;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

/*
*
*    This class is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This class is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this class.  If not, see <http://www.gnu.org/licenses/>.
*
*/

/**
 *
 * @author Max
 */
public class Schematic_old {

	private final byte[] blocks;
	private final byte[] data;
	private final short width;
	private final short length;
	private final short height;
	private final Vector offset;
	private final ListTag<CompoundTag> tileEntities;

	public Schematic_old(byte[] blocks, byte[] data, short width, short length, short height, Vector offset,
			ListTag<CompoundTag> tileEntities) {
		this.blocks = blocks;
		this.data = data;
		this.width = width;
		this.length = length;
		this.height = height;
		this.offset = offset;
		this.tileEntities = tileEntities;
	}

	/**
	 * @return the blocks
	 */
	public byte[] getBlocks() {
		return this.blocks;
	}

	/**
	 * @return the data
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * @return the width
	 */
	public short getWidth() {
		return this.width;
	}

	/**
	 * @return the length
	 */
	public short getLength() {
		return this.length;
	}

	/**
	 * @return the height
	 */
	public short getHeight() {
		return this.height;
	}

	/**
	 * @return the offset from WorldEdit
	 */
	public Vector getOffset() {
		return this.offset;
	}

	public ListTag<CompoundTag> getTileEntities() {
		return this.tileEntities;
	}
}