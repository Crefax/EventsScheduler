package net.crefax.hytale.events.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

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
    private static final String DEFAULT_LANGUAGE = "en-us";
    
    // Mapping from short codes to full locale codes
    private static final Map<String, String> LOCALE_MAPPING = new HashMap<>();
    static {
        LOCALE_MAPPING.put("en", "en-us");
        LOCALE_MAPPING.put("tr", "tr-tr");
        LOCALE_MAPPING.put("de", "de-de");
        LOCALE_MAPPING.put("fr", "fr-fr");
        LOCALE_MAPPING.put("es", "es-es");
        LOCALE_MAPPING.put("pt", "pt-br");
        LOCALE_MAPPING.put("ru", "ru-ru");
        LOCALE_MAPPING.put("zh", "zh-cn");
        LOCALE_MAPPING.put("ja", "ja-jp");
        LOCALE_MAPPING.put("ko", "ko-kr");
    }
    
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
     * Get a message by key (uses server default language)
     */
    public String getMessage(String key) {
        return getMessage(key, new String[0]);
    }

    /**
     * Get a message by key with placeholder replacements (uses server default language)
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
     * Get a message for a specific player based on their game language setting
     */
    public String getMessage(Player player, String key) {
        return getMessage(player, key, new String[0]);
    }

    /**
     * Get a message for a specific player with placeholder replacements
     * Uses the player's game language setting from PlayerRef.getLanguage()
     */
    public String getMessage(Player player, String key, String... args) {
        String playerLang = getPlayerLanguage(player);
        String message = getRawMessageForLanguage(playerLang, key);
        
        // Replace numbered placeholders {0}, {1}, {2}, etc.
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
        }
        
        return message;
    }

    /**
     * Get the player's language from their game settings
     */
    private String getPlayerLanguage(Player player) {
        if (player == null) {
            return currentLanguage;
        }
        
        try {
            PlayerRef playerRef = player.getPlayerRef();
            if (playerRef != null) {
                String lang = playerRef.getLanguage();
                if (lang != null && !lang.isEmpty()) {
                    // Normalize: replace underscore with hyphen and lowercase
                    lang = lang.toLowerCase().replace("_", "-");
                    
                    // If it's already in locale format (e.g., "tr-tr"), use it
                    if (lang.contains("-")) {
                        return lang;
                    }
                    
                    // Map short code to full locale (e.g., "tr" -> "tr-tr")
                    if (LOCALE_MAPPING.containsKey(lang)) {
                        return LOCALE_MAPPING.get(lang);
                    }
                    
                    // Try to construct locale (e.g., "fr" -> "fr-fr")
                    return lang + "-" + lang;
                }
            }
        } catch (Exception e) {
            // Fallback to server language if any error
            LOGGER.fine("[EventScheduler] Could not get player language: " + e.getMessage());
        }
        
        return currentLanguage;
    }

    /**
     * Get raw message for a specific language
     */
    private String getRawMessageForLanguage(String langCode, String key) {
        // Load language if not already loaded
        if (!languages.containsKey(langCode)) {
            if (!loadLanguage(langCode)) {
                langCode = currentLanguage;
            }
        }
        
        JsonObject langMessages = languages.get(langCode);
        
        // Try requested language first
        if (langMessages != null) {
            String message = getMessageFromObject(langMessages, key);
            if (message != null) {
                return message;
            }
        }
        
        // Fallback to current server language
        if (!langCode.equals(currentLanguage) && currentMessages != null) {
            String message = getMessageFromObject(currentMessages, key);
            if (message != null) {
                return message;
            }
        }
        
        // Fallback to default (English)
        if (fallbackMessages != null) {
            String message = getMessageFromObject(fallbackMessages, key);
            if (message != null) {
                return message;
            }
        }
        
        return "[Missing: " + key + "]";
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
     * Get the prefix (server default language)
     */
    public String getPrefix() {
        return getMessage("prefix");
    }

    /**
     * Get the prefix for a specific player
     */
    public String getPrefix(Player player) {
        return getMessage(player, "prefix");
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
