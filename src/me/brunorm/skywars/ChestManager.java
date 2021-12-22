package me.brunorm.skywars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

import com.cryptomorin.xseries.XMaterial;

public class ChestManager implements Listener {
	
	/*
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		Arena arena = Skywars.get().getPlayerArena(event.getPlayer());
		if(arena == null) return;
		HashMap<Location, Boolean> chests = arena.getChests();
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock != null && clickedBlock.getType() == XMaterial.CHEST.parseMaterial()) {
			Location location = clickedBlock.getLocation();
			if (chests.get(location) == null) {
				if(event.isCancelled()) return;
				chests.put(location, true);
				FillChest(location);
			}
		}
	}
	
	@EventHandler
	void onPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena != null && block.getType() == XMaterial.CHEST.parseMaterial()) {
			arena.setChest(block.getLocation(), true);
		}
	}
	*/

	public final static Block getTargetBlock(Player player, int range) {
		BlockIterator iter = new BlockIterator(player, range);
		Block lastBlock = iter.next();
		while (iter.hasNext()) {
			lastBlock = iter.next();
			if (lastBlock.getType() == XMaterial.AIR.parseMaterial()) {
				continue;
			}
			break;
		}
		return lastBlock;
	}

	/*
	public static void FillChest(Location location) {
		ItemStack wood = new ItemStack(Material.WOOD);
		wood.setAmount(32);
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemStack apple = new ItemStack(Material.APPLE);
		ItemStack golden = new ItemStack(Material.GOLDEN_APPLE);
		ItemStack[] items = {wood, sword, apple, golden};
		FillChest(location, items);
	}
	*/
	
	public static void FillChest(Location location) {
		System.out.println("filling block at " + location.getX() + " " + location.getY() + " " + location.getZ());
		Block block = location.getBlock();
		System.out.println("chest block type is " + block.getType());
		if(block.getType() != XMaterial.CHEST.parseMaterial()) return;
		Chest chest = (Chest) block.getState();
		Inventory inventory = chest.getBlockInventory();
		
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack wood = new ItemStack(XMaterial.OAK_WOOD.parseItem());
			wood.setAmount(32);
			ItemStack sword = new ItemStack(XMaterial.IRON_SWORD.parseItem());
			ItemStack apple = new ItemStack(XMaterial.APPLE.parseItem());
			apple.setAmount((int) Math.floor(Math.random()*3)+1);
			ItemStack golden = new ItemStack(XMaterial.GOLDEN_APPLE.parseItem());
			golden.setAmount((int) Math.floor(Math.random()*2)+1);
			ItemStack[] items = {wood, sword, apple, golden};
			
			ItemStack item = items[(int) Math.floor(Math.random() * items.length)];
			inventory.setItem((int) Math.floor(Math.random() * inventory.getSize()), item);
		}
		Bukkit.broadcastMessage("chest got filled");
	}

}
