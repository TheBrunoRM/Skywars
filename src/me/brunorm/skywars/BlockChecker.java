package me.brunorm.skywars;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class BlockChecker {

	int[][] faces = { //
			{ 1, 0, 0 }, { -1, 0, 0 }, //
			{ 0, 0, 1 }, { 0, 0, -1 }, //
			{ 0, 1, 0 }, { 0, -1, 0 } //
	};

	@SuppressWarnings("unused")
	void checkBlock(Location location) {
		Block block = location.getBlock();
		for(int i = 0; i < faces.length; i++) {
			Vector face = new Vector(faces[i][0],faces[i][1],faces[i][2]);
			Block nextBlock = location.add(face).getBlock();
		}
	}

}
