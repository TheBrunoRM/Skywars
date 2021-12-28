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
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getTarget();
		Arena arena = Skywars.get().getPlayerArena(player);
		if(arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			if(swp.isSpectator())
				event.setCancelled(true);
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
					arena.makeSpectator(swp, null);
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
		if (entity instanceof Player) {
			Player player = (Player) entity;
			Arena arena = Skywars.get().getPlayerArena(player);
			if (arena != null) {
				SkywarsPlayer swPlayer = arena.getPlayer(player);
				if(swPlayer.isSpectator()) event.setCancelled(true);
				if(arena.getStatus() != ArenaStatus.PLAYING || arena.isInvencibility()) {
					event.setCancelled(true);
				} else if(arena.getStatus() == ArenaStatus.PLAYING) {					
					if(swPlayer.isSpectator() && event.getCause() == DamageCause.VOID) {
						arena.goBackToCenter(player);
					} else if (event.getCause() == DamageCause.VOID) {
						event.setCancelled(true);
						System.out.println("player damaged, made spectator");
						arena.makeSpectator(swPlayer, null);
					}
				}
			}
		}
	}
	
	@EventHandler
	void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		
		Entity entity = event.getEntity();
		LivingEntity livingEntity = (LivingEntity) entity;
		Entity damager = event.getDamager();
		
		if (damager instanceof Player) {
			Player attacker = (Player) damager;
			Arena attackerArena = Skywars.get().getPlayerArena(attacker);
			if(attackerArena != null) {					
				SkywarsPlayer swAttacker = attackerArena.getPlayer(attacker);
				if(swAttacker != null && swAttacker.isSpectator()) {
					event.setCancelled(true);
				}
			}
			if (entity instanceof Player) {
				Player victim = (Player) entity;
				Arena victimArena = Skywars.get().getPlayerArena(victim);
				if(victimArena != null) {				
					SkywarsPlayer swVictim = victimArena.getPlayer(victim);
					if(livingEntity.getHealth() - event.getDamage() <= 0) {
						//event.setCancelled(true);
						// instead of cancelling the event,
						// we set the damage to 0 so the damage sound sounds
						// and it doesnt feel like the player just disappears when we hit
						event.setDamage(0);
						victimArena.makeSpectator(swVictim, attacker);
					}
				}
			}
		}
	}
	
	@EventHandler
	void onFoodLevelChange(FoodLevelChangeEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player) {
			Player player = (Player) entity;
			Arena arena = Skywars.get().getPlayerArena(player);
			if (arena != null) {
				SkywarsPlayer swp = arena.getPlayer(player);
				if (swp != null) {
					if (arena.getStatus() != ArenaStatus.PLAYING
							|| swp.isSpectator()) {
						event.setCancelled(true);
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
				if (arena.getStatus() != ArenaStatus.PLAYING
						|| swp.isSpectator()) {
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
				if (arena.getStatus() == ArenaStatus.WAITING
						|| swp.isSpectator()) {
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
						|| swp.isSpectator()) {					
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	void onEntityTarget(EntityTargetEvent event) {
		Entity entity = event.getEntity();
		Entity target = event.getTarget();
		if(target instanceof Player) {			
			Player player = (Player) target;
			Arena arena = Skywars.get().getPlayerArena(player);
			
			if(arena != null) {
				SkywarsPlayer swp = arena.getPlayer(player);
				if (swp != null) {
					if (arena.getStatus() != ArenaStatus.PLAYING
							|| swp.isSpectator()) {
						if (entity instanceof ExperienceOrb){
							event.setCancelled(true);
							event.setTarget(null);
							// TODO: do something else
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	void onXPChange(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		Arena arena = Skywars.get().getPlayerArena(player);
		
		if(arena != null) {
			SkywarsPlayer swp = arena.getPlayer(player);
			if (swp != null) {
				if (arena.getStatus() != ArenaStatus.PLAYING
						|| swp.isSpectator()) {
					// TODO: do something since this cant be cancelled
					//event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	void onInventoryCLick(InventoryClickEvent event) {
		HumanEntity whoClicked = event.getWhoClicked();
		if(whoClicked instanceof Player) {
			Player player = (Player) whoClicked;
			Arena arena = Skywars.get().getPlayerArena(player);
			if(arena != null) {
				SkywarsPlayer swp = arena.getPlayer(player);
				if (swp != null) {
					if(arena.getStatus() != ArenaStatus.PLAYING
							|| swp.isSpectator()) {
						event.setCancelled(true);
					}
				}
			}
		}
	}
}
