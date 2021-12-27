package me.brunorm.skywars.unused;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import me.brunorm.skywars.Skywars;

public class Scheduler {

	public static BukkitTask schedule(Runnable runnable, long delaySeconds) {
		return Bukkit.getScheduler().runTaskTimer(Skywars.get(), () -> runnable.run(), 0L, delaySeconds);
	}
	
}
