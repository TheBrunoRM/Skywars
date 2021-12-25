package me.brunorm.skywars.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
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
		if (arena != null) {
			SkywarsPlayer swp = arena.getPlayer(event.getPlayer());
			if(!arena.isInBoundaries(event.getPlayer().getLocation())) {
				if(!swp.isSpectator())
					arena.MakeSpectator(swp);
				else
					arena.goBackToCenter(player);
			}
		}
	}
	
	@EventHandler
	void onLeave(PlayerQuitEvent event) {
		Arena arena = Skywars.get().getPlayerArena(event.getPlayer());
		if (arena != null) {
			arena.LeavePlayer(event.getPlayer());
		}
	}
	
	@EventHandler
	void onDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity && entity instanceof Player) {
			LivingEntity livingEntity = (LivingEntity) entity;
			double health = livingEntity.getHealth();
			double damage = event.getDamage();
			System.out.println(String.format("player with %s health got damaged %s",
					health, damage));
			Player player = (Player) entity;
			Arena arena = Skywars.get().getPlayerArena(player);
			if (arena != null) {
				SkywarsPlayer swPlayer = arena.getPlayer(player.getName());
				if(arena.getStatus() != ArenaStatus.PLAYING) {
					event.setCancelled(true);
				}
				if(swPlayer.isSpectator() && event.getCause() == DamageCause.VOID) {
					arena.goBackToCenter(player);
				}
				else if (event.getCause() == DamageCause.VOID || health - damage <= 0) {
					event.setCancelled(true);
					System.out.println("player damaged, made spectator");
					arena.MakeSpectator(swPlayer);
					return;
				}
			}
		}
	}
	
	@EventHandler
	void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		if(damager instanceof Player) {
			Player attacker = (Player) damager;			
			Arena attackerArena = Skywars.get().getPlayerArena(attacker);
			if (attackerArena.getPlayer(attacker).isSpectator()) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	void onFoodLevelChange(FoodLevelChangeEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity) {
			if (entity instanceof Player) {
				Player player = (Player) entity;
				Arena arena = Skywars.get().getPlayerArena(player);
				if (arena != null) {
					SkywarsPlayer swp = arena.getPlayer(player);
					if (swp != null) {
						if (arena.getStatus() != ArenaStatus.PLAYING) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}

	@EventHandler
	void onDrop(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			if (swp != null) {
				if (arena.getStatus() != ArenaStatus.PLAYING || swp.isSpectator()) {
					event.setCancelled(true);
				} else {
					arena.getDroppedItems().add(event.getItemDrop());
				}
			}
		}
	}
	
	@EventHandler
	void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			if (swp != null) {
				if (arena.getStatus() != ArenaStatus.PLAYING || swp.isSpectator()) {
					System.out.println("cancelling interaction");
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	void onPickUp(PlayerPickupItemEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		
		if(arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			if (swp != null) {
				if (arena.getStatus() != ArenaStatus.PLAYING
						|| swp.isSpectator()
						//|| !arena.droppedItems.contains(event.getItem())
					)
					event.setCancelled(true);
			}
		}
	}
}
