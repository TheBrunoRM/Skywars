package me.brunorm.skywars.structures;

import org.bukkit.WeatherType;

public class ArenaGameSettings {

	Arena arena;
	long time;
	WeatherType weather;
	ChestType chestType;

	ArenaGameSettings(Arena arena) {
		this.arena = arena;
		this.time = 12000;
		this.weather = WeatherType.CLEAR;
		this.chestType = ChestType.NORMAL;
	}

	@Deprecated
	public void change(Object obj) {
		if (obj instanceof WeatherType)
			this.weather = (WeatherType) obj;
		else if (obj instanceof Long)
			this.time = (long) obj;
		else if (obj instanceof ChestType)
			this.chestType = (ChestType) obj;
		else
			return;
	}
}