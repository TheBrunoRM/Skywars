package me.brunorm.skywars;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

public class ChestManager implements Listener {
	
	public static void fillChest(Location location, boolean overpowered) {
		Block block = location.getBlock();
		if(block.getType() != XMaterial.CHEST.parseMaterial()) return;
		Chest chest = (Chest) block.getState();
		Inventory inventory = chest.getBlockInventory();
		inventory.clear();
		
		if(overpowered) {
			
			// swords
			
			ItemStack sword = new ItemStack(XMaterial.DIAMOND_SWORD.parseItem());
			sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
			
			for(int i = 0; i < Math.floor(Math.random() * 2) - 1; i++) {			
				inventory.setItem(SkywarsUtils.getRandomSlot(inventory), sword);
			}
			
			// bow and arrow
			
			ItemStack arrows = new ItemStack(XMaterial.ARROW.parseMaterial(),
					Math.random() > 0.5 ? 32 : 16);
			
			ItemStack bow = new ItemStack(XMaterial.BOW.parseItem());
			bow.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
			
			if(Math.random() > 0.5) {
				inventory.setItem(SkywarsUtils.getRandomSlot(inventory), bow);
				inventory.setItem(SkywarsUtils.getRandomSlot(inventory), arrows);
			}
			
			// misc
			
			ItemStack snowballs = new ItemStack(XMaterial.SNOWBALL.parseItem());
			snowballs.setAmount(Math.random() > 0.5 ? 16 : 8);
			
			ItemStack eggs = new ItemStack(XMaterial.EGG.parseItem());
			eggs.setAmount(Math.random() > 0.5 ? 16 : 8);
			
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), snowballs);
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), eggs);
			
			ItemStack lava = new ItemStack(XMaterial.LAVA_BUCKET.parseItem());
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), lava);
			
			ItemStack water = new ItemStack(XMaterial.WATER_BUCKET.parseItem());
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory), water);
		} else {
			
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
			
			ItemStack arrows = new ItemStack(XMaterial.ARROW.parseMaterial(),
					Math.random() > 0.5 ? 32 : 16);
			
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
		}
		
		// blocks
		
		ItemStack wood = new ItemStack(XMaterial.OAK_WOOD.parseMaterial(), 16);
		
		ItemStack stone = new ItemStack(XMaterial.COBBLESTONE.parseMaterial(), 16);
		
		for(int i = 0; i < Math.ceil(Math.random() * 3); i++) {			
			inventory.setItem(SkywarsUtils.getRandomSlot(inventory),
					Math.random() > 0.5 ? stone : wood);
		}
	}

}
