package me.brunorm.skywars;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class Scheduler {

	public static BukkitTask schedule(Runnable runnable, long delaySeconds) {
		return Bukkit.getScheduler().runTaskTimer(Skywars.get(), () -> runnable.run(), 0L, delaySeconds);
	}
	
}
