package me.thebrunorm.skywars;

import org.bukkit.configuration.file.YamlConfiguration;

import com.cryptomorin.xseries.XMaterial;

public class SkywarsConfiguration {
	public boolean invencibilityEnabled = true;
	public long invencibilityTicks = 40L;
	public XMaterial defaultCaseMaterial = XMaterial.GLASS;

	public void load(YamlConfiguration config) {
		this.invencibilityEnabled = config.getBoolean("invencibility.enabled");
		this.invencibilityTicks = config.getLong("invencibility.ticks");
		this.defaultCaseMaterial = XMaterial.valueOf(config.getString("defaultCaseMaterial", "GLASS"));
	}
}
