/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

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
			e.printStackTrace();
			plugin.sendMessage("&eEconomy (Vault): &ccould not hook.");
		}
	}

	private static boolean setupEconomy() {
		Skywars plugin = Skywars.get();
		if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
			plugin.sendMessage("&eEconomy (Vault): &6plugin not found.");
			return false;
		}

		economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider == null) {
			plugin.sendMessage("&eEconomy (Vault): &6plugin found &cbut no registered service provider!");
			return false;
		}
		economy = economyProvider.getProvider();
		return plugin.economy != null;
	}
}
