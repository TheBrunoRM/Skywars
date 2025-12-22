// Copyright (c) 2025 Bruno
package me.thebrunorm.skywars;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.configuration.file.YamlConfiguration;

public class SkywarsConfiguration {
	public boolean invincibilityEnabled = true;
	public long invincibilityTicks = 40L;
	public XMaterial defaultCaseMaterial = XMaterial.GLASS;

	public void load(YamlConfiguration config) {
		this.invincibilityEnabled = config.getBoolean("invincibility.enabled");
		this.invincibilityTicks = config.getLong("invincibility.ticks");
		this.defaultCaseMaterial = XMaterial.valueOf(config.getString("defaultCaseMaterial", "GLASS"));
	}
}
