package me.brunorm.skywars.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
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
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getTarget();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			if(swp.isSpectator()) event.setCancelled(true);
		}
    }
	
	@EventHandler
	void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		if (arena != null) {
			SkywarsPlayer swp = arena.getPlayer(event.getPlayer());
			if(!arena.isInBoundaries(event.getPlayer())) {
				if(arena.getWinner() != swp
						&& arena.getStatus() != ArenaStatus.PLAYING
						&& !swp.isSpectator())
					arena.leavePlayer(player);
				else if(!swp.isSpectator())
					arena.makeSpectator(swp);
				else
					arena.goBackToCenter(player);
			}
		}
	}
	
	@EventHandler
	void onLeave(PlayerQuitEvent event) {
		Arena arena = Skywars.get().getPlayerArena(event.getPlayer());
		if (arena != null) {
			arena.leavePlayer(event.getPlayer());
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
				SkywarsPlayer swPlayer = arena.getPlayer(player);
				if(swPlayer.isSpectator()) event.setCancelled(true);
				if(arena.getStatus() != ArenaStatus.PLAYING || arena.isInvencibility()) {
					System.out.println("Cancelling damage!");
					event.setCancelled(true);
				} else if(arena.getStatus() == ArenaStatus.PLAYING) {					
					if(swPlayer.isSpectator() && event.getCause() == DamageCause.VOID) {
						arena.goBackToCenter(player);
					} else if (event.getCause() == DamageCause.VOID || health - damage <= 0) {
						event.setCancelled(true);
						System.out.println("player damaged, made spectator");
						arena.makeSpectator(swPlayer);
					}
				}
			}
		}
	}
	
	@EventHandler
	void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		if(damager != null && damager instanceof Player) {
			Player attacker = (Player) damager;			
			Arena attackerArena = Skywars.get().getPlayerArena(attacker);
			if (attackerArena != null) {
				SkywarsPlayer swp = attackerArena.getPlayer(attacker);
				if(swp != null && swp.isSpectator()) {
					event.setCancelled(true);
				}
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
				if (swp.isSpectator()) {
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
