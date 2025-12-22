/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class SkywarsEconomy {

	static Economy economy;
	Skywars plugin;
	RegisteredServiceProvider<Economy> economyProvider;

	public SkywarsEconomy(Skywars plugin) {
		this.plugin = plugin;
	}

	public static Economy getEconomy() {
		return economy;
	}

	private void setup() {
		boolean economyEnabled = Skywars.get().getConfig().getBoolean("economy.enabled");
		if (economyEnabled)
			try {
				if (setupEconomy()) {
					plugin.sendMessage("&eEconomy (Vault): &a" + this.economyProvider.getPlugin().getName());
				}
			} catch (final Exception e) {
				plugin.sendMessage("&eEconomy (Vault): &ccould not hook.");
				e.printStackTrace();
			}
		else
			plugin.sendMessage("&eEconomy (Vault): &6disabled in config.");
	}

	private boolean setupEconomy() {
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
