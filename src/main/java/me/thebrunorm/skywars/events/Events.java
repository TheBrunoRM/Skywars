/* (C) 2021 Bruno */
package me.thebrunorm.skywars.events;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.thebrunorm.skywars.ArenaStatus;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsUser;

public class Events implements Listener {

	@EventHandler
	void onMove(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);

		if (player.getWorld() != arena.getWorld())
			arena.leavePlayer(swp);

		if (arena.isInBoundaries(player))
			return;

		if (!arena.started())
			return;

		if (!swp.isSpectator())
			arena.makeSpectator(swp);
		else arena.teleportPlayerToOwnSpawnAsSpectator(swp);
	}

	@EventHandler
	void onLeave(PlayerQuitEvent event) {
		final Arena arena = Skywars.get().getPlayerArena(event.getPlayer());
		if (arena == null)
			return;
		arena.leavePlayer(event.getPlayer());
	}

	@EventHandler
	void onDamage(EntityDamageEvent event) {
		final Entity entity = event.getEntity();
		if (!(entity instanceof Player))
			return;
		final Player player = (Player) entity;
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swPlayer = arena.getUser(player);

		if (swPlayer.isSpectator()) {
			event.setCancelled(true);
			if (event.getCause() == DamageCause.VOID)
				//arena.goBackToCenter(player);
				arena.teleportPlayerToOwnSpawnAsSpectator(swPlayer);
			return;
		}

		if (arena.getStatus() != ArenaStatus.PLAYING || arena.isInvencibility()) {
			event.setCancelled(true);
		} else if (arena.getStatus() == ArenaStatus.PLAYING && event.getCause() == DamageCause.VOID) {
			event.setCancelled(true);
			arena.makeSpectator(swPlayer);
		}

		if (player.getHealth() - event.getDamage() <= 0) {
			// event.setCancelled(true);
			// instead of cancelling the event,
			// we set the damage to 0 so the damage sound sounds
			// and it doesnt feel like the player just disappears when we hit
			event.setDamage(0);
			arena.makeSpectator(swPlayer, swPlayer.getLastHit(), event.getCause());
		}
	}

	@EventHandler
	void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		final Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity))
			return;
		final LivingEntity livingEntity = (LivingEntity) entity;
		final Entity damager = event.getDamager();
		if (damager == null || !(damager instanceof Player))
			return;
		final Player attacker = (Player) damager;

		final Arena attackerArena = Skywars.get().getPlayerArena(attacker);
		if (attackerArena != null) {
			final SkywarsUser swAttacker = attackerArena.getUser(attacker);
			if (swAttacker != null && swAttacker.isSpectator()) {
				event.setCancelled(true);
			}
		}
		if (!(entity instanceof Player))
			return;
		final Player victim = (Player) entity;
		final Arena victimArena = Skywars.get().getPlayerArena(victim);
		if (victimArena == null)
			return;
		final SkywarsUser swVictim = victimArena.getUser(victim);
		swVictim.setLastHit(attacker);
		if (livingEntity.getHealth() - event.getDamage() <= 0) {
			// event.setCancelled(true);
			// instead of cancelling the event,
			// we set the damage to 0 so the damage sound sounds
			// and it doesnt feel like the player just disappears when we hit
			event.setDamage(0);
			victimArena.makeSpectator(swVictim, attacker);
		}
	}

	// prevent spectators from being targeted by mobs
	@EventHandler
	public void onMobTarget(EntityTargetLivingEntityEvent event) {
		if (!(event.getTarget() instanceof Player))
			return;
		final Player player = (Player) event.getTarget();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (!swp.isSpectator())
			return;
		event.setCancelled(true);
	}

	// prevent spectators from being hungry
	@EventHandler
	void onFoodLevelChange(FoodLevelChangeEvent event) {
		final Entity entity = event.getEntity();
		if (!(entity instanceof Player))
			return;
		final Player player = (Player) entity;
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null)
			return;
		if (!swp.isSpectator())
			return;

		event.setCancelled(true);
	}

	// prevent spectators from dropping items
	@EventHandler
	void onDrop(PlayerDropItemEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null)
			return;
		if (!swp.isSpectator() && arena.started())
			return;

		event.setCancelled(true);
	}

	@EventHandler
	void onBlockBreak(BlockBreakEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null)
			return;
		if (swp.isSpectator()) {
			event.setCancelled(true);
			return;
		}
		final Block block = event.getBlock();
		if (!(block.getState() instanceof Chest))
			return;
		final Chest chest = (Chest) block.getState();
		if (!arena.getChests().contains(chest))
			return;
		arena.removeChest(chest);
	}

	// prevent spectators from interacting
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null)
			return;
		if (swp.isSpectator()) {
			event.setCancelled(true);
			return;
		}

		final Block block = event.getClickedBlock();
		if (block == null)
			return;
		if (!(block.getState() instanceof Chest))
			return;
		final Chest chest = (Chest) block.getState();
		if (!arena.getChests().contains(chest))
			return;

		arena.addChestHologram(chest);
	}

	// prevent spectators from picking up items
	@EventHandler
	void onPickup(PlayerPickupItemEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null)
			return;
		if (!swp.isSpectator())
			return;

		event.setCancelled(true);
	}

	// prevent spectators from grabbing experience orbs
	@EventHandler
	void onEntityTarget(EntityTargetEvent event) {
		final Entity entity = event.getEntity();
		if (!(entity instanceof ExperienceOrb))
			return;

		final Entity target = event.getTarget();
		if (!(target instanceof Player))
			return;

		final Player player = (Player) target;
		final Arena arena = Skywars.get().getPlayerArena(player);

		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null)
			return;
		/*
		 * if ((arena.getStatus() != ArenaStatus.PLAYING && arena.getWinner() != swp) ||
		 * swp.isSpectator())
		 */
		if (!swp.isSpectator())
			return;

		event.setCancelled(true);
		event.setTarget(null);
		// TODO: do something else
	}

	// prevent spectators from obtaining XP
	@EventHandler
	void onXPChange(PlayerExpChangeEvent event) {
		final Player player = event.getPlayer();
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null || !swp.isSpectator())
			return;

		// this event can't be cancelled
		// TODO: check if this actually works
		player.setTotalExperience(player.getTotalExperience());
	}

	// prevent spectators from clicking their inventory
	@EventHandler
	void onInventoryClick(InventoryClickEvent event) {
		final HumanEntity whoClicked = event.getWhoClicked();
		if (!(whoClicked instanceof Player))
			return;
		final Player player = (Player) whoClicked;
		final Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null)
			return;
		final SkywarsUser swp = arena.getUser(player);
		if (swp == null)
			return;
		if (!swp.isSpectator() && arena.started())
			return;

		event.setCancelled(true);
	}
}
