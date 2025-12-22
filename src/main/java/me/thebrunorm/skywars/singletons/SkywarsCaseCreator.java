// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.singletons;

import com.cryptomorin.xseries.XMaterial;
import me.thebrunorm.skywars.Skywars;
import org.bukkit.Location;
import org.bukkit.block.Block;

public enum SkywarsCaseCreator {
	;

	public static void createCase(Location location, XMaterial material) {
		if (Skywars.config.getBoolean("debug.bigCases")) {
			createBigCase(location, material);
			return;
		}
		final int[][] blocks = {
				// first layer
				{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1},
				// second layer
				{-1, 1, 0}, {1, 1, 0}, {0, 1, -1}, {0, 1, 1},
				// third layer
				{-1, 2, 0}, {1, 2, 0}, {0, 2, -1}, {0, 2, 1},
				// base and top
				{0, -1, 0}, {0, 3, 0},
				// base joints
				{-1, -1, 0}, {1, -1, 0}, {0, -1, -1}, {0, -1, 1},
				// top joints
				{-1, 3, 0}, {1, 3, 0}, {0, 3, -1}, {0, 3, 1},};
		final int[][] airBlocks = {{0, 0, 0}, {0, 1, 0}, {0, 2, 0}};
		for (final int[] relative : airBlocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(XMaterial.AIR.parseMaterial());
		}
		for (final int[] relative : blocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(material.parseMaterial());
			if (!XMaterial.isNewVersion()) {
				try {
					block.getClass().getMethod("setData", byte.class).invoke(block, material.getData());
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void createBigCase(Location location, XMaterial material) {
		final int[][] blocks = {
				// base
				{-1, -1, -1}, {0, -1, -1}, {1, -1, -1}, {-1, -1, 0}, {0, -1, 0}, {1, -1, 0}, {-1, -1, 1},
				{0, -1, 1}, {1, -1, 1},

				// top
				{-1, 3, -1}, {0, 3, -1}, {1, 3, -1}, {-1, 3, 0}, {0, 3, 0}, {1, 3, 0}, {-1, 3, 1},
				{0, 3, 1}, {1, 3, 1},

				// left wall
				{2, 0, -1}, {2, 0, 0}, {2, 0, 1}, {2, 1, -1}, {2, 1, 0}, {2, 1, 1}, {2, 2, -1},
				{2, 2, 0}, {2, 2, 1},

				// front wall
				{-1, 0, 2}, {0, 0, 2}, {1, 0, 2}, {-1, 1, 2}, {0, 1, 2}, {1, 1, 2}, {-1, 2, 2},
				{0, 2, 2}, {1, 2, 2},

				// right wall
				{-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1}, {-2, 1, -1}, {-2, 1, 0}, {-2, 1, 1}, {-2, 2, -1},
				{-2, 2, 0}, {-2, 2, 1},

				// back wall
				{-1, 0, -2}, {0, 0, -2}, {1, 0, -2}, {-1, 1, -2}, {0, 1, -2}, {1, 1, -2}, {-1, 2, -2},
				{0, 2, -2}, {1, 2, -2},};
		final int[][] airBlocks = {{-1, 0, -1}, {0, 0, -1}, {1, 0, -1}, {-1, 0, 0}, {0, 0, 0}, {1, 0, 0},
				{-1, 0, 1}, {0, 0, 1}, {1, 0, 1},

				{-1, 1, -1}, {0, 1, -1}, {1, 1, -1}, {-1, 1, 0}, {0, 1, 0}, {1, 1, 0}, {-1, 1, 1},
				{0, 1, 1}, {1, 1, 1},

				{-1, 2, -1}, {0, 2, -1}, {1, 2, -1}, {-1, 2, 0}, {0, 2, 0}, {1, 2, 0}, {-1, 2, 1},
				{0, 2, 1}, {1, 2, 1},};
		for (final int[] relative : airBlocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(XMaterial.AIR.parseMaterial());
		}
		for (final int[] relative : blocks) {
			final Block block = location.getBlock().getRelative(relative[0], relative[1], relative[2]);
			block.setType(material.parseMaterial());
			if (XMaterial.isNewVersion()) continue;

			try {
				block.getClass().getMethod("setData", byte.class).invoke(block, material.getData());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}
}
