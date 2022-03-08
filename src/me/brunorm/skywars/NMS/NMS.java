package me.brunorm.skywars.NMS;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NMS {

	public void sendParticles(Player player, Location loc, String particle, int amount);

	public void sendActionbar(Player player, String text);

	public void sendTitle(Player player, String title);

	public void sendTitle(Player player, String title, String subtitle);

	public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

}
