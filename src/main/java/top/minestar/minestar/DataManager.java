package top.minestar.minestar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    
    private final Plugin plugin;
    private final Gson gson;
    private final File dataFile;
    
    public DataManager(Plugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "wanted_data.json");
        
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }
    
    public void saveData() {
        try {
            Map<UUID, Integer> playerStars = ((MinestarWanted) plugin).getPlayerStars();
            
            Map<String, Integer> serializableData = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : playerStars.entrySet()) {
                serializableData.put(entry.getKey().toString(), entry.getValue());
            }
            
            String json = gson.toJson(serializableData);
            
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            FileWriter writer = new FileWriter(dataFile);
            writer.write(json);
            writer.close();
            
            plugin.getLogger().info("Data saved successfully to: " + dataFile.getAbsolutePath());
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("No data file found. Starting with empty data.");
            return;
        }
        
        try {
            FileReader reader = new FileReader(dataFile);
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> serializableData = gson.fromJson(reader, type);
            reader.close();
            
            if (serializableData != null) {
                Map<UUID, Integer> playerStars = new HashMap<>();
                for (Map.Entry<String, Integer> entry : serializableData.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        playerStars.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID found in data file: " + entry.getKey());
                    }
                }
                
                ((MinestarWanted) plugin).setPlayerStars(playerStars);
                plugin.getLogger().info("Data loaded successfully! Loaded " + playerStars.size() + " player records.");
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load data: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            plugin.getLogger().severe("Error parsing data file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    
    public void clearData() {
        if (dataFile.exists()) {
            dataFile.delete();
            plugin.getLogger().info("Data file cleared!");
        }
        ((MinestarWanted) plugin).setPlayerStars(new HashMap<>());
    }
    

}
