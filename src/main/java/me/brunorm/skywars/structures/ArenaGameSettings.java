package me.brunorm.skywars.structures;

import org.bukkit.WeatherType;

public class ArenaGameSettings {

	WeatherType weather;
	long time;
	ChestType chestType;

	ArenaGameSettings() {
		this.weather = WeatherType.CLEAR;
		this.time = 12000;
		this.chestType = ChestType.NORMAL;
	}

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