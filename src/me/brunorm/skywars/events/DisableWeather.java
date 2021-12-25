package me.brunorm.skywars.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class DisableWeather implements Listener {

	@EventHandler
	void onWeatherChange(WeatherChangeEvent event) {
		if(event.toWeatherState()) event.setCancelled(true);
	}
	
}
