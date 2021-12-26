package me.brunorm.skywars;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

public class ChestManager implements Listener {
	
	public static void fillChest(Location location) {
		Block block = location.getBlock();
		if(block.getType() != XMaterial.CHEST.parseMaterial()) return;
		Chest chest = (Chest) block.getState();
		Inventory inventory = chest.getBlockInventory();
		inventory.clear();
		
		// swords
		
		ItemStack[] swords = {
				new ItemStack(XMaterial.IRON_SWORD.parseItem()),
				new ItemStack(XMaterial.STONE_SWORD.parseItem()),
				new ItemStack(XMaterial.WOODEN_SWORD.parseItem()),
				new ItemStack(XMaterial.GOLDEN_SWORD.parseItem()),
		};
		
		for(int i = 0; i < Math.floor(Math.random() * 2) - 1; i++) {			
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory),
					swords[(int) (Math.floor(Math.random() * swords.length+1)-1)]);
		}
		
		// bow and arrow
		
		ItemStack arrows = new ItemStack(XMaterial.BOW.parseItem());
		arrows.setAmount(Math.random() > 0.5 ? 32 : 16);
		
		if(Math.random() > 0.5) {
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), new ItemStack(XMaterial.BOW.parseItem()));
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), arrows);
		}
		
		// misc
		
		ItemStack snowballs = new ItemStack(XMaterial.SNOWBALL.parseItem());
		snowballs.setAmount(Math.random() > 0.5 ? 32 : 16);
		
		ItemStack eggs = new ItemStack(XMaterial.EGG.parseItem());
		eggs.setAmount(Math.random() > 0.5 ? 32 : 16);
		
		inventory.setItem(SkywarsUtils.getRandomSlot(inventory), snowballs);
		inventory.setItem(SkywarsUtils.getRandomSlot(inventory), eggs);
		
		ItemStack lava = new ItemStack(XMaterial.LAVA_BUCKET.parseItem());
		inventory.setItem(SkywarsUtils.getRandomSlot(inventory), lava);
		
		ItemStack water = new ItemStack(XMaterial.WATER_BUCKET.parseItem());
		inventory.setItem(SkywarsUtils.getRandomSlot(inventory), water);
		
		// blocks
		
		ItemStack wood = new ItemStack(XMaterial.OAK_WOOD.parseItem());
		wood.setAmount(16);
		
		ItemStack stone = new ItemStack(XMaterial.COBBLESTONE.parseItem());
		stone.setAmount(16);
		
		for(int i = 0; i < Math.floor(Math.random() * 3); i++) {			
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), Math.random() > 0.5 ? stone : wood);
		}
	}

}
