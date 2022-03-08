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
		SkywarsUtils.ClearPlayer(this.player);

		// clear inventory

		this.player.getInventory().setContents(this.inventoryItems);
		this.player.getEquipment().setArmorContents(this.equipmentItems);
		this.player.updateInventory();

		// clear player
		this.player.setGameMode(this.gamemode);
		this.player.setExp(this.exp);
		this.player.setLevel(this.level);
		this.player.setFoodLevel(this.hunger);
		this.player.setHealth(this.health);
		this.player.setMaxHealth(this.maxHealth);
		this.player.setFlying(this.flying);
		this.player.setAllowFlight(this.allowFlight);
		this.player.setFireTicks(this.fireTicks);
		this.player.getInventory().setHeldItemSlot(this.heldItemSlot);

		for (final PotionEffect effect : this.potionEffects) {
			this.player.addPotionEffect(effect);
		}
	}
}
