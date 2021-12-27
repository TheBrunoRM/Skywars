package me.brunorm.skywars.structures;

import java.util.Collection;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import me.brunorm.skywars.SkywarsUtils;

public class SavedPlayer {
	
	Player player;
	ItemStack[] inventoryItems;
	ItemStack[] equipmentItems;
	GameMode gamemode;
	float exp;
	int level;
	int hunger;
	double health;
	double maxHealth;
	Collection<PotionEffect> potionEffects;
	boolean flying;
	boolean allowFlight;
	int fireTicks;
	int heldItemSlot;
	
	public SavedPlayer(Player player) {
		this.player = player;
		this.inventoryItems = player.getInventory().getContents();
		this.equipmentItems = player.getInventory().getArmorContents();
		this.gamemode = player.getGameMode();
		this.exp = player.getExp();
		this.level = player.getLevel();
		this.hunger = player.getFoodLevel();
		this.health = player.getHealth();
		this.maxHealth = player.getMaxHealth();
		this.potionEffects = player.getActivePotionEffects();
		this.flying = player.isFlying();
		this.allowFlight = player.getAllowFlight();
		this.fireTicks = player.getFireTicks();
		this.heldItemSlot = player.getInventory().getHeldItemSlot();
	}
	
	@SuppressWarnings("deprecation")
	public void Restore() {
		SkywarsUtils.ClearPlayer(player);
		
		// clear inventory
		
		player.getInventory().setContents(inventoryItems);
		player.getEquipment().setArmorContents(equipmentItems);
		player.updateInventory();

		// clear player
		player.setGameMode(gamemode);
		player.setExp(exp);
		player.setLevel(level);
		player.setFoodLevel(hunger);
		player.setHealth(health);
		player.setMaxHealth(maxHealth);
		player.setFlying(flying);
		player.setAllowFlight(allowFlight);
		player.setFireTicks(fireTicks);
		player.getInventory().setHeldItemSlot(heldItemSlot);
		
		for (PotionEffect effect : potionEffects) {
			player.addPotionEffect(effect);
		}
	}
}
