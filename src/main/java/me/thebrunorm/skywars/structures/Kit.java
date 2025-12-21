/* (C) 2021 Bruno */
package me.thebrunorm.skywars.structures;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class Kit {

	private String name;
	private String displayName;
	private ItemStack icon;
	private ItemStack[] items;
	private double price;
	private YamlConfiguration config;
	private File file;

	public Kit(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name.toLowerCase();
	}

	public String getDisplayName() {
		if (this.displayName == null || this.displayName.isEmpty() || this.displayName.isEmpty())
			return this.getName();
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public ItemStack getIcon() {
		return this.icon;
	}

	public void setIcon(ItemStack icon) {
		this.icon = icon;
	}

	public ItemStack[] getItems() {
		return this.items;
	}

	public void setItems(ItemStack[] items) {
		this.items = items;
	}

	public double getPrice() {
		return this.price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public YamlConfiguration getConfig() {
		return this.config;
	}

	public void setConfig(YamlConfiguration config) {
		this.config = config;
	}

	public File getFile() {
		return this.file;
	}

	public void setFile(File file) {
		this.file = file;
	}

}
