// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars.singletons;

import me.thebrunorm.skywars.Skywars;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

public enum SkywarsEconomy {
	;

	static Economy economy;
	static RegisteredServiceProvider<Economy> economyProvider;

	public static Economy getEconomy() {
		return economy;
	}

	public static void setup() {
		Skywars plugin = Skywars.get();
		boolean economyEnabled = Skywars.get().getConfig().getBoolean("economy.enabled");
		if (!economyEnabled) {
			plugin.sendMessage("&eEconomy (Vault): &6disabled in config.");
			return;
		}

		try {
			if (setupEconomy())
				plugin.sendMessage("&eEconomy (Vault): &a" + economyProvider.getPlugin().getName());
		} catch (final Exception e) {
			plugin.getLogger().log(Level.SEVERE, "Could not hook to Vault", e);
		}
	}

	private static boolean setupEconomy() {
		Skywars plugin = Skywars.get();
		if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
			plugin.getLogger().warning("&eEconomy (Vault): &6plugin not found.");
			return false;
		}

		economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider == null) {
			plugin.getLogger().warning("&eEconomy (Vault): &6plugin found &cbut no registered service provider!");
			return false;
		}
		economy = economyProvider.getProvider();
		return economy != null;
	}
}
