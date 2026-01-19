package net.crefax.hytale.events.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Internationalization Manager
 * 
 * Handles loading and retrieving localized messages.
 * 
 * @author Crefax
 */
public class I18nManager {

    private static final Logger LOGGER = Logger.getLogger("EventScheduler");
    private static final String DEFAULT_LANGUAGE = "en";
    
    private final Map<String, JsonObject> languages = new HashMap<>();
    private String currentLanguage = DEFAULT_LANGUAGE;
    private JsonObject currentMessages;
    private JsonObject fallbackMessages;

    public I18nManager() {
        loadLanguage(DEFAULT_LANGUAGE);
        fallbackMessages = languages.get(DEFAULT_LANGUAGE);
        currentMessages = fallbackMessages;
    }

    /**
     * Load a language file from resources
     */
    public boolean loadLanguage(String langCode) {
        try {
            String path = "lang/" + langCode + ".json";
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            
            if (is == null) {
                LOGGER.warning("[EventScheduler] Language file not found: " + path);
                return false;
            }
            
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            
            languages.put(langCode, root);
            LOGGER.info("[EventScheduler] Language loaded: " + langCode);
            return true;
            
        } catch (Exception e) {
            LOGGER.severe("[EventScheduler] Error loading language " + langCode + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Set the current language
     */
    public void setLanguage(String langCode) {
        if (!languages.containsKey(langCode)) {
            if (!loadLanguage(langCode)) {
                LOGGER.warning("[EventScheduler] Failed to load language: " + langCode + ", using default");
                return;
            }
        }
        
        currentLanguage = langCode;
        currentMessages = languages.get(langCode);
        LOGGER.info("[EventScheduler] Language set to: " + langCode);
    }

    /**
     * Get a message by key
     */
    public String getMessage(String key) {
        return getMessage(key, new String[0]);
    }

    /**
     * Get a message by key with placeholder replacements
     * Placeholders are in format {0}, {1}, {2}, etc.
     */
    public String getMessage(String key, String... args) {
        String message = getRawMessage(key);
        
        // Replace numbered placeholders {0}, {1}, {2}, etc.
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
        }
        
        return message;
    }

    /**
     * Get raw message from current language, fallback to default if not found
     */
    private String getRawMessage(String key) {
        // Try current language first
        String message = getMessageFromObject(currentMessages, key);
        if (message != null) {
            return message;
        }
        
        // Fallback to default language
        if (currentMessages != fallbackMessages) {
            message = getMessageFromObject(fallbackMessages, key);
            if (message != null) {
                return message;
            }
        }
        
        // Key not found
        return "[Missing: " + key + "]";
    }

    /**
     * Get message from a JsonObject, supports nested keys with dot notation
     */
    private String getMessageFromObject(JsonObject obj, String key) {
        if (obj == null) return null;
        
        String[] parts = key.split("\\.");
        JsonObject current = obj;
        
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject(parts[i]);
        }
        
        String lastKey = parts[parts.length - 1];
        if (current.has(lastKey) && current.get(lastKey).isJsonPrimitive()) {
            return current.get(lastKey).getAsString();
        }
        
        return null;
    }

    /**
     * Get the prefix
     */
    public String getPrefix() {
        return getMessage("prefix");
    }

    /**
     * Get current language code
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Check if a language is loaded
     */
    public boolean isLanguageLoaded(String langCode) {
        return languages.containsKey(langCode);
    }

    /**
     * Reload all languages
     */
    public void reload() {
        languages.clear();
        loadLanguage(DEFAULT_LANGUAGE);
        fallbackMessages = languages.get(DEFAULT_LANGUAGE);
        
        if (!currentLanguage.equals(DEFAULT_LANGUAGE)) {
            loadLanguage(currentLanguage);
        }
        currentMessages = languages.getOrDefault(currentLanguage, fallbackMessages);
    }
}
