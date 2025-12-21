/* (C) 2021 Bruno */
package me.thebrunorm.skywars;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bukkit.plugin.PluginDescriptionFile;

import com.google.gson.Gson;

class Asset {
	String browser_download_url;
}

class Release {
	String tag_name;
	List<Asset> assets;
}

public class SkywarsUpdater {

	public static final PluginDescriptionFile pdf = Skywars.get().getDescription();

	public static boolean update(boolean applyUpdate) {

		final String JSON_URL = "https://api.github.com/repos/TheBrunoRM/Skywars/releases/latest";

		long start = Instant.now().toEpochMilli();
		try {
			Skywars.get().sendMessage(Messager.getMessage("UPDATER_CHECKING_FOR_UPDATES"));

			final String json = readUrl(JSON_URL);
			final Gson gson = new Gson();
			final Release release = gson.fromJson(json, Release.class);

			String version = release.tag_name;
			if (version.startsWith("v"))
				version = version.substring(1);

			Skywars.get().sendDebugMessage("&aLatest version: " + version);
			Skywars.get().sendDebugMessage("&6Current version: " + pdf.getVersion());

			if (pdf.getVersion().equalsIgnoreCase(version)) {
				Skywars.get().sendMessage(Messager.getMessage("UPDATER_LAST_VERSION", 
						Instant.now().toEpochMilli() - start));
				return false;
			}

			Skywars.get().sendMessage(Messager.getMessage("UPDATER_UPDATE_AVAILABLE", Instant.now().toEpochMilli() - start));

			final String FILE_URL = release.assets.get(0).browser_download_url;
			Skywars.get().sendDebugMessage("File URL: " + FILE_URL);

			if (!applyUpdate) {
				Skywars.get().sendMessage(Messager.getMessage("UPDATER_DOWNLOAD_HERE"));
				Skywars.get().sendMessage(Messager.getMessage("UPDATER_FILE_URL", FILE_URL));
				return true;
			}

			Skywars.get().sendMessage(Messager.getMessage("UPDATER_UPDATING_PLUGIN"));

			start = Instant.now().toEpochMilli();

			final InputStream in = new URL(FILE_URL).openStream();
			final Path path = Skywars.get().file().getAbsoluteFile().toPath();
			Skywars.get().sendDebugMessage("Download path: " + path);

			Files.write(path, IOUtils.toByteArray(in), StandardOpenOption.WRITE);

			Skywars.get().sendMessage(Messager.getMessage("UPDATER_PLUGIN_UPDATED", Instant.now().toEpochMilli() - start));
			Skywars.get().sendMessage(Messager.getMessage("UPDATER_UPDATE_WILL_BE_APPLIED"));
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			Skywars.get().sendMessage(Messager.getMessage("UPDATER_COULD_NOT_CHECK_UPDATES", Instant.now().toEpochMilli() - start));
			return false;
		}
	}

	private static String readUrl(String urlString) throws Exception {
		BufferedReader reader = null;
		try {
			final URL url = new URL(urlString);
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			final StringBuffer buffer = new StringBuffer();
			int read;
			final char[] chars = new char[1024];
			while ((read = reader.read(chars)) != -1)
				buffer.append(chars, 0, read);

			return buffer.toString();
		} finally {
			if (reader != null)
				reader.close();
		}
	}
}
