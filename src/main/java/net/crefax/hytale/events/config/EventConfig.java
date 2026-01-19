package net.crefax.hytale.events.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event Configuration Manager
 * 
 * Handles loading and parsing of events.json configuration.
 * 
 * @author Crefax
 */
public class EventConfig {

    private static final Logger LOGGER = Logger.getLogger("EventScheduler");
    private static final String CONFIG_FOLDER = "mods/EventScheduler";
    private static final String CONFIG_FILE = "events.json";
    
    private String broadcastPrefix = "[Events] ";
    private List<IntervalEvent> intervalEvents = new ArrayList<>();
    private List<ScheduledEvent> scheduledEvents = new ArrayList<>();
    private Settings settings = new Settings();

    public void load() {
        try {
            // Create config folder
            Path configDir = Paths.get(CONFIG_FOLDER);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                LOGGER.info("[EventScheduler] Config folder created: " + configDir.toAbsolutePath());
            }
            
            Path configFile = configDir.resolve(CONFIG_FILE);
            
            // Copy from JAR if file doesn't exist
            if (!Files.exists(configFile)) {
                createDefaultConfig(configFile);
            }
            
            // Read config file
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            // Prefix
            if (root.has("broadcastPrefix")) {
                this.broadcastPrefix = root.get("broadcastPrefix").getAsString();
            }

            // Interval Events
            if (root.has("intervalEvents")) {
                JsonArray arr = root.getAsJsonArray("intervalEvents");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject obj = arr.get(i).getAsJsonObject();
                    IntervalEvent event = new IntervalEvent();
                    event.name = obj.get("name").getAsString();
                    event.enabled = obj.get("enabled").getAsBoolean();
                    event.interval = obj.get("interval").getAsInt();
                    event.broadcastMessage = obj.has("broadcastMessage") ? obj.get("broadcastMessage").getAsString() : "";
                    
                    event.commands = new ArrayList<>();
                    JsonArray cmds = obj.getAsJsonArray("commands");
                    for (int j = 0; j < cmds.size(); j++) {
                        event.commands.add(cmds.get(j).getAsString());
                    }
                    
                    intervalEvents.add(event);
                }
            }

            // Scheduled Events
            if (root.has("scheduledEvents")) {
                JsonArray arr = root.getAsJsonArray("scheduledEvents");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject obj = arr.get(i).getAsJsonObject();
                    ScheduledEvent event = new ScheduledEvent();
                    event.name = obj.get("name").getAsString();
                    event.enabled = obj.get("enabled").getAsBoolean();
                    event.broadcastMessage = obj.has("broadcastMessage") ? obj.get("broadcastMessage").getAsString() : "";
                    
                    event.times = new ArrayList<>();
                    JsonArray times = obj.getAsJsonArray("times");
                    for (int j = 0; j < times.size(); j++) {
                        event.times.add(times.get(j).getAsString());
                    }
                    
                    event.commands = new ArrayList<>();
                    JsonArray cmds = obj.getAsJsonArray("commands");
                    for (int j = 0; j < cmds.size(); j++) {
                        event.commands.add(cmds.get(j).getAsString());
                    }
                    
                    scheduledEvents.add(event);
                }
            }

            // Settings
            if (root.has("settings")) {
                JsonObject s = root.getAsJsonObject("settings");
                if (s.has("timezone")) settings.timezone = s.get("timezone").getAsString();
                if (s.has("debugMode")) settings.debugMode = s.get("debugMode").getAsBoolean();
                if (s.has("minPlayersRequired")) settings.minPlayersRequired = s.get("minPlayersRequired").getAsInt();
                if (s.has("cooldownBetweenEvents")) settings.cooldownBetweenEvents = s.get("cooldownBetweenEvents").getAsInt();
            }

            LOGGER.info("[EventScheduler] Config loaded: " + intervalEvents.size() + " interval, " + scheduledEvents.size() + " scheduled events.");

        } catch (Exception e) {
            LOGGER.severe("[EventScheduler] Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createDefaultConfig(Path configFile) {
        try {
            // Read default config from JAR
            InputStream is = getClass().getClassLoader().getResourceAsStream("config/events.json");
            if (is != null) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Files.writeString(configFile, content, StandardCharsets.UTF_8);
                is.close();
                LOGGER.info("[EventScheduler] Default config created: " + configFile.toAbsolutePath());
            } else {
                // Create default config manually
                String defaultConfig = """
                {
                    "broadcastPrefix": "[Events] ",
                    
                    "intervalEvents": [
                        {
                            "name": "hourly_reward",
                            "enabled": true,
                            "interval": 3600,
                            "commands": ["give Weapon_Sword_Cobalt"],
                            "broadcastMessage": "Hourly reward! Cobalt Sword distributed to all players!"
                        }
                    ],
                    
                    "scheduledEvents": [
                        {
                            "name": "daily_reward",
                            "enabled": true,
                            "times": ["09:00", "18:00"],
                            "commands": ["give Weapon_Sword_Crude", "give Tool_Pickaxe_Crude"],
                            "broadcastMessage": "Daily reward distributed!"
                        }
                    ],
                    
                    "settings": {
                        "timezone": "Europe/London",
                        "debugMode": false,
                        "minPlayersRequired": 0,
                        "cooldownBetweenEvents": 0
                    }
                }
                """;
                Files.writeString(configFile, defaultConfig, StandardCharsets.UTF_8);
                LOGGER.info("[EventScheduler] Default config created manually: " + configFile.toAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.severe("[EventScheduler] Error creating default config: " + e.getMessage());
        }
    }

    public void reload() {
        intervalEvents.clear();
        scheduledEvents.clear();
        load();
    }
    
    public String getConfigPath() {
        return Paths.get(CONFIG_FOLDER, CONFIG_FILE).toAbsolutePath().toString();
    }

    // Getters
    public String getBroadcastPrefix() { return broadcastPrefix; }
    public List<IntervalEvent> getIntervalEvents() { return intervalEvents; }
    public List<ScheduledEvent> getScheduledEvents() { return scheduledEvents; }
    public Settings getSettings() { return settings; }

    // Inner classes
    public static class IntervalEvent {
        public String name;
        public boolean enabled;
        public int interval; // in seconds
        public List<String> commands;
        public String broadcastMessage;
    }

    public static class ScheduledEvent {
        public String name;
        public boolean enabled;
        public List<String> times; // "HH:mm" format
        public List<String> commands;
        public String broadcastMessage;
    }

    public static class Settings {
        public String timezone = "Europe/London";
        public boolean debugMode = false;
        public int minPlayersRequired = 0;
        public int cooldownBetweenEvents = 0;
    }
}
