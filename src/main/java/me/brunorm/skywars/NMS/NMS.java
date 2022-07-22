package me.brunorm.skywars.NMS;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NMS {

	public void sendParticles(Player player, Location loc, String particle, int amount);

	public void sendActionbar(Player player, String text);

	public default void sendTitle(Player player, String title) {
		this.sendTitle(player, title, "");
	}

	public default void sendSubtitle(Player player, String subtitle) {
		this.sendTitle(player, "", subtitle);
	}

	public default void sendTitle(Player player, String title, String subtitle) {
		this.sendTitle(player, title, subtitle, 10, 70, 20);
	}

	public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

}
