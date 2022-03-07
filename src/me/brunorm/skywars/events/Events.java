package me.brunorm.skywars.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

import me.brunorm.skywars.ArenaStatus;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsPlayer;

public class Events implements Listener {
	
	@EventHandler
	void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if(arena.isInBoundaries(player)) return;
		if (!arena.started()) {}
			//arena.leavePlayer(player);
		else if(!swp.isSpectator())
			arena.makeSpectator(swp, null);
		else if (arena.isInBoundaries(arena.getLocation()))
			arena.goBackToCenter(player);
	}
	
	@EventHandler
	void onLeave(PlayerQuitEvent event) {
		Arena arena = Skywars.get().getPlayerArena(event.getPlayer());
		if (arena == null) return;
		arena.leavePlayer(event.getPlayer());
	}
	
	@EventHandler
	void onDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) return;
		Player player = (Player) entity;
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null) return;
		SkywarsPlayer swPlayer = arena.getPlayer(player);
		if(swPlayer.isSpectator()) event.setCancelled(true);
		if(arena.getStatus() != ArenaStatus.PLAYING || arena.isInvencibility()) {
			event.setCancelled(true);
		} else if(arena.getStatus() == ArenaStatus.PLAYING) {					
			if(swPlayer.isSpectator() && event.getCause() == DamageCause.VOID) {
				arena.goBackToCenter(player);
			} else if (event.getCause() == DamageCause.VOID) {
				event.setCancelled(true);
				arena.makeSpectator(swPlayer, null);
			}
		}
	}
	
	@EventHandler
	void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		if(!(entity instanceof LivingEntity)) return;
		LivingEntity livingEntity = (LivingEntity) entity;
		Entity damager = event.getDamager();
		if(damager == null || !(damager instanceof Player)) return;
		Player attacker = (Player) damager;
		Arena attackerArena = Skywars.get().getPlayerArena(attacker);
		if(attackerArena != null) {					
			SkywarsPlayer swAttacker = attackerArena.getPlayer(attacker);
			if(swAttacker != null && swAttacker.isSpectator()) {
				event.setCancelled(true);
			}
		}
		if (!(entity instanceof Player)) return;
		Player victim = (Player) entity;
		Arena victimArena = Skywars.get().getPlayerArena(victim);
		if(victimArena == null) return;
		SkywarsPlayer swVictim = victimArena.getPlayer(victim);
		if(livingEntity.getHealth() - event.getDamage() <= 0) {
			//event.setCancelled(true);
			// instead of cancelling the event,
			// we set the damage to 0 so the damage sound sounds
			// and it doesnt feel like the player just disappears when we hit
			event.setDamage(0);
			victimArena.makeSpectator(swVictim, attacker);
		}
		swVictim.setLastHit(attacker);
	}
	
	// prevent spectators from being targeted by mobs
    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getTarget();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if(!swp.isSpectator()) return;
		event.setCancelled(true);
    }
	
	// prevent spectators from being hungry
	@EventHandler
	void onFoodLevelChange(FoodLevelChangeEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) return;
		Player player = (Player) entity;
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if (swp == null) return;
		if (!swp.isSpectator()) return;
		
		event.setCancelled(true);
	}

	// prevent spectators from dropping items
	@EventHandler
	void onDrop(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if (swp == null) return;
		if(!swp.isSpectator()) return;
		
		event.setCancelled(true);
	}
	
	// prevent spectators from interacting
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if (swp == null) return;
		if(!swp.isSpectator()) return;
		
		event.setCancelled(true);
	}
	
	// prevent spectators from picking up items
	@EventHandler
	void onPickup(PlayerPickupItemEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if (swp == null) return;
		if(!swp.isSpectator()) return;
		
		event.setCancelled(true);
	}
	
	// prevent spectators from grabbing experience orbs
	@EventHandler
	void onEntityTarget(EntityTargetEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof ExperienceOrb)) return;
		
		Entity target = event.getTarget();
		if(!(target instanceof Player)) return;
		
		Player player = (Player) target;
		Arena arena = Skywars.get().getPlayerArena(player);
		
		if(arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if (swp == null) return;
		/*
		if ((arena.getStatus() != ArenaStatus.PLAYING
				&& arena.getWinner() != swp)
				|| swp.isSpectator())
		*/
		if(!swp.isSpectator()) return;
		
		event.setCancelled(true);
		event.setTarget(null);
		// TODO: do something else
	}
	
	// prevent spectators from obtaining XP
	@EventHandler
	void onXPChange(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if (swp == null) return;
		if(!swp.isSpectator()) return;
		
		// TODO: do something since this cant be cancelled
		//event.setCancelled(true);
	}
	
	// prevent spectators from clicking their inventory
	@EventHandler
	void onInventoryClick(InventoryClickEvent event) {
		HumanEntity whoClicked = event.getWhoClicked();
		if(!(whoClicked instanceof Player)) return;
		Player player = (Player) whoClicked;
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena == null) return;
		SkywarsPlayer swp = arena.getPlayer(player);
		if (swp == null) return;
		if(!swp.isSpectator()) return;
		
		event.setCancelled(true);
	}
}
