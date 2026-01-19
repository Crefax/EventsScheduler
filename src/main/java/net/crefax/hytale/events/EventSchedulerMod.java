package net.crefax.hytale.events;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import net.crefax.hytale.events.commands.EventsCommand;
import net.crefax.hytale.events.commands.EventsTriggerCommand;
import net.crefax.hytale.events.commands.EventsReloadCommand;
import net.crefax.hytale.events.manager.SchedulerManager;
import net.crefax.hytale.events.config.EventConfig;
import net.crefax.hytale.events.i18n.I18nManager;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Events Scheduler Plugin
 * 
 * Scheduled event and reward distribution system.
 * Supports interval and time-based event scheduling.
 * 
 * @author Crefax
 * @version 1.0.0
 */
public class EventSchedulerMod extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("EventScheduler");
    
    private static EventSchedulerMod instance;
    private SchedulerManager schedulerManager;
    private EventConfig config;
    private I18nManager i18n;

    public EventSchedulerMod(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        
        LOGGER.info("========================================");
        LOGGER.info("     Events Scheduler v1.0.0           ");
        LOGGER.info("     Scheduled Event System            ");
        LOGGER.info("========================================");
        
        // Load config
        this.config = new EventConfig();
        this.config.load();
        
        // Load i18n
        this.i18n = new I18nManager();
        this.i18n.setLanguage(config.getSettings().language);
        
        // Start scheduler
        this.schedulerManager = new SchedulerManager(this, config);
        
        // Register commands
        this.getCommandRegistry().registerCommand(new EventsCommand(this));
        this.getCommandRegistry().registerCommand(new EventsTriggerCommand(this));
        this.getCommandRegistry().registerCommand(new EventsReloadCommand(this));
        
        // Register events - player tracking
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        
        LOGGER.info("[EventScheduler] Plugin loaded successfully!");
    }
    
    private void onPlayerReady(PlayerReadyEvent event) {
        schedulerManager.registerPlayer(event.getPlayer());
    }
    
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        schedulerManager.unregisterPlayer(event.getPlayerRef());
    }

    public static EventSchedulerMod getInstance() {
        return instance;
    }

    public SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }

    public EventConfig getConfig() {
        return config;
    }

    public I18nManager getI18n() {
        return i18n;
    }
}
