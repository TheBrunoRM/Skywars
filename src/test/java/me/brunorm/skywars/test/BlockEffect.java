package me.brunorm.skywars.test;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;

import me.brunorm.skywars.Skywars;

public class BlockEffect implements Listener {
	ArrayList<Vector> blockEffectList = new ArrayList<>();

	@SuppressWarnings("deprecation")
	@EventHandler
	void onMoveBlockEffect(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final World world = player.getWorld();
		final Location loc = player.getLocation().add(new Vector(0, -1, 0)).getBlock().getLocation();
		if (loc.getBlock().getType() == XMaterial.AIR.parseMaterial())
			return;
		final Vector v = new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		if (this.blockEffectList.contains(v))
			return;
		this.blockEffectList.add(v);
		Skywars.get().NMS().sendParticles(player, new Location(world, v.getX(), v.getY(), v.getZ()), "FIREWORKS_SPARK",
				10);
		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				player.sendBlockChange(loc, XMaterial.PINK_WOOL.parseMaterial(), (byte) Math.round(Math.random() * 15));
			}
		}, 5L);
		Bukkit.getScheduler().runTaskLater(Skywars.get(), new Runnable() {
			@Override
			public void run() {
				final Block block = world.getBlockAt(loc);
				player.sendBlockChange(loc, block.getType(), block.getData());
				BlockEffect.this.blockEffectList.remove(new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
			}
		}, 20L);
	}
}
