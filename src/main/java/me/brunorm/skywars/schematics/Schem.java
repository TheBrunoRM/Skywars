package me.brunorm.skywars.schematics;

import java.util.Map;

import org.bukkit.util.Vector;
import org.jnbt.ListTag;
import org.jnbt.Tag;

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
public class Schem {

	private final byte[] blockData;
	private final Map<String, Tag> palette;
	private final short width;
	private final short length;
	private final short height;
	private final Vector offset;
	private final ListTag tileEntities;

	public Schem(byte[] blockData, Map<String, Tag> palette, short width, short length, short height, Vector offset,
			ListTag tileEntities) {
		this.blockData = blockData;
		this.palette = palette;
		this.width = width;
		this.length = length;
		this.height = height;
		this.offset = offset;
		this.tileEntities = tileEntities;
	}

	/**
	 * @return the block data
	 */
	public byte[] getBlockData() {
		return this.blockData;
	}

	/**
	 * @return the block palette
	 */
	public Map<String, Tag> getPalette() {
		return this.palette;
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

	public ListTag getTileEntities() {
		return this.tileEntities;
	}
}