package me.brunorm.skywars.schematics;

import org.bukkit.util.Vector;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;

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
public class Schem
{
 
    private byte[] blockData;
    private CompoundTag palette;
    private short width;
    private short length;
    private short height;
    private Vector offset;
    private ListTag tileEntities;
 
    public Schem(byte[] blockData, CompoundTag palette, short width,
    		short length, short height, Vector offset, ListTag tileEntities)
    {
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
    public byte[] getBlockData()
    {
        return blockData;
    }
 
    /**
    * @return the block palette
    */
    public CompoundTag getPalette()
    {
        return palette;
    }
 
    /**
    * @return the width
    */
    public short getWidth()
    {
        return width;
    }
 
    /**
    * @return the lenght
    */
    public short getLength()
    {
        return length;
    }
 
    /**
    * @return the height
    */
    public short getHeight()
    {
        return height;
    }
    
    /**
    * @return the offset from WorldEdit
    */
    public Vector getOffset() {
    	return offset;
    }
    
    public ListTag getTileEntities() {
    	return tileEntities;
    }
}