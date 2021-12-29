package me.brunorm.skywars;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigurationUtils {
	
    public static final int DEFAULT_BUFFER_SIZE = 8192;
	
    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {
    	
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.close();
        }

    }
	
	public static YamlConfiguration loadConfiguration(String name, String defaultFileName) {
		File file = new File(Skywars.get().getDataFolder(), name);
		if (!file.exists()) {
			System.out.println("creating file " + name);
			copyDefaultContentsToFile(defaultFileName, file);
		}
		return createMissingKeys(
				YamlConfiguration.loadConfiguration(file),
				getDefaultConfig(defaultFileName));
	}
	
	public static YamlConfiguration createMissingKeys(YamlConfiguration conf, YamlConfiguration defaultConfig) {
		
		try {
			ConfigurationSection section = defaultConfig.getConfigurationSection("");
			
			boolean modified = false;
			
			for (String key : section.getKeys(true)) {
				if (conf.get(key) == null) {
					Skywars.get().sendMessage("&cWarning: key &b%s &cis not set.", key);
					modified = true;

					// setting the key in the configuration
					// so it uses the default value if is not set
					// but not saving it to prevent removing spaces and comments
					conf.set(key, defaultConfig.get(key));
				}
			}
			if(modified) {				
				Skywars.get().sendMessage("&cYou should not delete keys in the configuration files.");
				Skywars.get().sendMessage("&cThe plugin will use the default values for the deleted keys.");
			}
			return conf;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
    
	static YamlConfiguration getDefaultConfig(String defaultFileName) {
		try {
			Reader defaultConfigStream = new InputStreamReader(Skywars.get().getResource(defaultFileName), "UTF-8");
			return YamlConfiguration.loadConfiguration(defaultConfigStream);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static void copyDefaultContentsToFile(String defaultFileName, File file) {
		try {
			if(!file.exists()) file.createNewFile();
			//System.out.println("copying default contents to file " + file.getName());
			try {
				copyInputStreamToFile(Skywars.get().getResource(defaultFileName), file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveConfiguration(YamlConfiguration config, String path) {
		try {
			config.save(new File(Skywars.get().getDataFolder(), path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
