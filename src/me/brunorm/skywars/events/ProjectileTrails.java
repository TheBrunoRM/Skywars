package me.brunorm.skywars.events;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.brunorm.skywars.Skywars;
import mrblobman.sounds.Sounds;

public class ProjectileTrails implements Listener {
	
	private Map<Projectile, BukkitTask> tasks = new HashMap<>();
	
	@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
        Projectile entity = e.getEntity();
        EntityType type = entity.getType();
	    if (e.getEntity().getShooter() instanceof Player) {
            if(type == EntityType.ARROW) {
		        tasks.put(e.getEntity(), new BukkitRunnable() {
		            @Override
		            public void run() {
	                    World w = entity.getWorld();
	                    Location l = entity.getLocation();
	    	            w.playEffect(l, Effect.SMOKE, 2);
		            }
		        }.runTaskTimer(Skywars.get(), 0L, 1L));
            }
	    }
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
	    if (e.getEntity().getShooter() instanceof Player) {
	        BukkitTask task = tasks.get(e.getEntity());
	        if (task != null) {
	            task.cancel();
	            tasks.remove(e.getEntity());
	            Location loc = e.getEntity().getLocation();
	            e.getEntity().getWorld().playEffect(loc, Effect.SMOKE, 1, 1);
	            e.getEntity().getWorld().playSound(loc, Sounds.EXPLODE.bukkitSound(), 3, 1);
	        }
	    }
	}
}
