/* (C) 2021 Bruno */
package me.thebrunorm.skywars.nms;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NMS {

	void sendParticles(Player player, Location loc, String particle, int amount);

	void sendActionbar(Player player, String text);

	default void sendTitle(Player player, String title) {
		this.sendTitle(player, title, "");
	}

	default void sendSubtitle(Player player, String subtitle) {
		this.sendTitle(player, "", subtitle);
	}

	default void sendTitle(Player player, String title, String subtitle) {
		this.sendTitle(player, title, subtitle, 10, 70, 20);
	}

	void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

}
