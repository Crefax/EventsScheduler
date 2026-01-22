package net.crefax.hytale.events.manager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import net.crefax.hytale.events.EventSchedulerMod;
import net.crefax.hytale.events.config.EventConfig;
import net.crefax.hytale.events.config.EventConfig.IntervalEvent;
import net.crefax.hytale.events.config.EventConfig.ScheduledEvent;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Scheduler Manager
 * 
 * Manages interval and time-based events.
 * 
 * @author Crefax
 */
public class SchedulerManager {

    private static final Logger LOGGER = Logger.getLogger("EventScheduler");
    
    private final EventSchedulerMod plugin;
    private final EventConfig config;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> runningIntervalTasks = new HashMap<>();
    private final Map<String, Long> lastExecutionTime = new HashMap<>();
    private final Map<UUID, Player> onlinePlayers = new ConcurrentHashMap<>();
    private ScheduledFuture<?> timeCheckerTask;
    private boolean running = true;

    public SchedulerManager(EventSchedulerMod plugin, EventConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        startIntervalEvents();
        startTimeChecker();
        
        LOGGER.info("[EventScheduler] Scheduler started.");
    }
    
    /**
     * Register a player
     */
    public void registerPlayer(Player player) {
        UUID uuid = player.getPlayerRef().getUuid();
        onlinePlayers.put(uuid, player);
        LOGGER.fine("[EventScheduler] Player registered: " + player.getDisplayName());
    }
    
    /**
     * Unregister a player
     */
    public void unregisterPlayer(PlayerRef ref) {
        if (ref != null) {
            onlinePlayers.remove(ref.getUuid());
        }
    }

    /**
     * Start interval-based events
     */
    private void startIntervalEvents() {
        for (IntervalEvent event : config.getIntervalEvents()) {
            if (event.enabled) {
                scheduleIntervalEvent(event);
            }
        }
    }

    private void scheduleIntervalEvent(IntervalEvent event) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            try {
                executeEvent(event.name, event.commands, event.broadcastMessage);
            } catch (Exception e) {
                LOGGER.severe("[EventScheduler] Event execution error: " + e.getMessage());
            }
        }, event.interval, event.interval, TimeUnit.SECONDS);
        
        runningIntervalTasks.put(event.name, task);
        LOGGER.info("[EventScheduler] Interval event started: " + event.name + " (every " + event.interval + " seconds)");
    }

    /**
     * Check for time-based events
     */
    private void startTimeChecker() {
        timeCheckerTask = scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            try {
                checkScheduledEvents();
            } catch (Exception e) {
                LOGGER.severe("[EventScheduler] Time check error: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS); // Check every 30 seconds
    }

    private void checkScheduledEvents() {
        ZoneId zone = ZoneId.of(config.getSettings().timezone);
        LocalTime now = LocalTime.now(zone);
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        
        for (ScheduledEvent event : config.getScheduledEvents()) {
            if (!event.enabled) continue;
            
            for (String time : event.times) {
                if (shouldExecuteScheduledEvent(event.name, time, currentTime)) {
                    executeEvent(event.name, event.commands, event.broadcastMessage);
                    markExecuted(event.name + "_" + time);
                }
            }
        }
    }

    private boolean shouldExecuteScheduledEvent(String eventName, String scheduledTime, String currentTime) {
        if (!scheduledTime.equals(currentTime)) {
            return false;
        }
        
        String key = eventName + "_" + scheduledTime;
        long lastExec = lastExecutionTime.getOrDefault(key, 0L);
        long now = System.currentTimeMillis();
        
        // Don't run again if executed within last 2 minutes
        if (now - lastExec < 120000) {
            return false;
        }
        
        return true;
    }

    private void markExecuted(String key) {
        lastExecutionTime.put(key, System.currentTimeMillis());
    }

    /**
     * Execute event - apply commands to all players
     */
    private void executeEvent(String eventName, List<String> commands, String broadcastMessage) {
        Collection<Player> players = getOnlinePlayers();
        
        // Minimum player check
        int minPlayers = config.getSettings().minPlayersRequired;
        if (players.size() < minPlayers) {
            if (config.getSettings().debugMode) {
                LOGGER.info("[EventScheduler] " + eventName + " cancelled: Not enough players (" + players.size() + "/" + minPlayers + ")");
            }
            return;
        }
        
        // Send broadcast message
        if (broadcastMessage != null && !broadcastMessage.isEmpty()) {
            String fullMessage = config.getBroadcastPrefix() + broadcastMessage;
            broadcastToAll(fullMessage);
        }
        
        // Execute commands for each player
        for (Player player : players) {
            for (String command : commands) {
                executeCommandForPlayer(player, command);
            }
        }
        
        if (config.getSettings().debugMode) {
            LOGGER.info("[EventScheduler] Event executed: " + eventName + " (" + players.size() + " players)");
        }
    }

    /**
     * Execute event manually - bypasses minimum player check
     */
    private void executeEventManual(String eventName, List<String> commands, String broadcastMessage) {
        Collection<Player> players = getOnlinePlayers();
        
        // Send broadcast message first (even if no players)
        if (broadcastMessage != null && !broadcastMessage.isEmpty()) {
            String fullMessage = config.getBroadcastPrefix() + broadcastMessage;
            broadcastToAll(fullMessage);
            LOGGER.info("[EventScheduler] Broadcast: " + fullMessage);
        }
        
        // Execute commands for each player
        for (Player player : players) {
            for (String command : commands) {
                executeCommandForPlayer(player, command);
            }
        }
        
        LOGGER.info("[EventScheduler] Event manually triggered: " + eventName + " (" + players.size() + " players)");
    }

    /**
     * Get online players
     */
    private Collection<Player> getOnlinePlayers() {
        return onlinePlayers.values();
    }

    /**
     * Execute command for player
     * Supported command prefixes:
     * - console:<command> : Executes command from server console (full permissions)
     * - cmd:<command> : Executes command as the player
     * - op:<command> : Executes command with OP permissions
     * - server:<command> : Alias for console:
     * 
     * Built-in commands (no prefix needed):
     * - give <itemId> <quantity> : Gives item to player
     * - message <text> : Sends message to player
     * 
     * Placeholders:
     * - {player} : Player's username
     * - {uuid} : Player's UUID
     * - {display_name} : Player's display name
     */
    private void executeCommandForPlayer(Player player, String command) {
        try {
            // Replace placeholders
            String processedCommand = replacePlaceholders(command, player);
            
            // Check for command prefixes
            if (processedCommand.startsWith("console:") || processedCommand.startsWith("server:")) {
                // Console command - execute with full server permissions
                String consoleCmd = processedCommand.substring(processedCommand.indexOf(':') + 1).trim();
                executeConsoleCommand(consoleCmd);
                return;
            }
            
            if (processedCommand.startsWith("cmd:") || processedCommand.startsWith("command:")) {
                // Player command - execute as the player
                String playerCmd = processedCommand.substring(processedCommand.indexOf(':') + 1).trim();
                executePlayerCommand(player, playerCmd);
                return;
            }
            
            if (processedCommand.startsWith("op:") || processedCommand.startsWith("admin:")) {
                // OP command - execute with elevated permissions
                String opCmd = processedCommand.substring(processedCommand.indexOf(':') + 1).trim();
                executeOpCommand(player, opCmd);
                return;
            }
            
            // Built-in commands (legacy support)
            String[] parts = processedCommand.trim().split("\\s+", 3);
            if (parts.length == 0) return;
            
            String cmd = parts[0].toLowerCase();
            
            switch (cmd) {
                case "give":
                    // give <itemId> <quantity>
                    if (parts.length >= 2) {
                        String itemId = parts[1];
                        int quantity = 1;
                        if (parts.length >= 3) {
                            try {
                                quantity = Integer.parseInt(parts[2]);
                            } catch (NumberFormatException e) {
                                quantity = 1;
                            }
                        }
                        giveItem(player, itemId, quantity);
                    }
                    break;
                    
                case "message":
                case "msg":
                    // message <text>
                    if (parts.length >= 2) {
                        String text = processedCommand.substring(cmd.length()).trim();
                        player.sendMessage(Message.raw(text));
                    }
                    break;
                    
                case "broadcast":
                case "bc":
                    // broadcast <text> - send to all players
                    if (parts.length >= 2) {
                        String text = processedCommand.substring(cmd.length()).trim();
                        broadcastToAll(text);
                    }
                    break;
                    
                default:
                    // Try to execute as console command for backward compatibility
                    executeConsoleCommand(processedCommand);
                    break;
            }
        } catch (Exception e) {
            LOGGER.warning("[EventScheduler] Command error: " + command + " - " + e.getMessage());
        }
    }
    
    /**
     * Replace placeholders in command string
     */
    private String replacePlaceholders(String command, Player player) {
        if (player == null) return command;
        
        String result = command;
        PlayerRef ref = player.getPlayerRef();
        
        result = result.replace("{player}", ref.getUsername());
        result = result.replace("{uuid}", ref.getUuid().toString());
        result = result.replace("{display_name}", player.getDisplayName());
        result = result.replace("{name}", ref.getUsername());
        
        return result;
    }
    
    /**
     * Execute command from server console (full permissions)
     */
    private void executeConsoleCommand(String command) {
        try {
            CommandManager commandManager = HytaleServer.get().getCommandManager();
            commandManager.handleCommand(ConsoleSender.INSTANCE, command);
            
            if (config.getSettings().debugMode) {
                LOGGER.info("[EventScheduler] Console command executed: " + command);
            }
        } catch (Exception e) {
            LOGGER.warning("[EventScheduler] Console command error: " + command + " - " + e.getMessage());
        }
    }
    
    /**
     * Execute command as player
     */
    private void executePlayerCommand(Player player, String command) {
        try {
            CommandManager commandManager = HytaleServer.get().getCommandManager();
            commandManager.handleCommand(player.getPlayerRef(), command);
            
            if (config.getSettings().debugMode) {
                LOGGER.info("[EventScheduler] Player command executed: " + player.getDisplayName() + " -> " + command);
            }
        } catch (Exception e) {
            LOGGER.warning("[EventScheduler] Player command error: " + command + " - " + e.getMessage());
        }
    }
    
    /**
     * Execute command with OP permissions (runs as console on behalf of player)
     */
    private void executeOpCommand(Player player, String command) {
        try {
            // For OP commands, we execute as console since console has full permissions
            // The command string may contain {player} placeholder which was already replaced
            CommandManager commandManager = HytaleServer.get().getCommandManager();
            commandManager.handleCommand(ConsoleSender.INSTANCE, command);
            
            if (config.getSettings().debugMode) {
                LOGGER.info("[EventScheduler] OP command executed for " + player.getDisplayName() + ": " + command);
            }
        } catch (Exception e) {
            LOGGER.warning("[EventScheduler] OP command error: " + command + " - " + e.getMessage());
        }
    }
    
    /**
     * Give item to player
     */
    private void giveItem(Player player, String itemId, int quantity) {
        try {
            ItemStack itemStack = new ItemStack(itemId, quantity);
            Inventory inventory = player.getInventory();
            ItemContainer storage = inventory.getStorage();
            storage.addItemStack(itemStack);
            
            if (config.getSettings().debugMode) {
                LOGGER.info("[EventScheduler] Item given: " + player.getDisplayName() + " -> " + quantity + "x " + itemId);
            }
        } catch (Exception e) {
            LOGGER.warning("[EventScheduler] Item give error: " + itemId + " - " + e.getMessage());
        }
    }

    private void broadcastToAll(String message) {
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(Message.raw(message));
        }
    }

    /**
     * Trigger event manually
     */
    public boolean triggerEvent(String eventName) {
        // Interval events
        for (IntervalEvent event : config.getIntervalEvents()) {
            if (event.name.equalsIgnoreCase(eventName)) {
                executeEventManual(event.name, event.commands, event.broadcastMessage);
                return true;
            }
        }
        
        // Scheduled events
        for (ScheduledEvent event : config.getScheduledEvents()) {
            if (event.name.equalsIgnoreCase(eventName)) {
                executeEventManual(event.name, event.commands, event.broadcastMessage);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Toggle event on/off
     */
    public boolean toggleEvent(String eventName, boolean enable) {
        for (IntervalEvent event : config.getIntervalEvents()) {
            if (event.name.equalsIgnoreCase(eventName)) {
                event.enabled = enable;
                if (enable && !runningIntervalTasks.containsKey(event.name)) {
                    scheduleIntervalEvent(event);
                } else if (!enable && runningIntervalTasks.containsKey(event.name)) {
                    runningIntervalTasks.get(event.name).cancel(false);
                    runningIntervalTasks.remove(event.name);
                }
                return true;
            }
        }
        
        for (ScheduledEvent event : config.getScheduledEvents()) {
            if (event.name.equalsIgnoreCase(eventName)) {
                event.enabled = enable;
                return true;
            }
        }
        
        return false;
    }

    /**
     * Reload all events
     */
    public void reload() {
        // Stop current tasks
        for (ScheduledFuture<?> task : runningIntervalTasks.values()) {
            task.cancel(false);
        }
        runningIntervalTasks.clear();
        lastExecutionTime.clear();
        
        // Reload config
        config.reload();
        
        // Restart events
        startIntervalEvents();
        
        LOGGER.info("[EventScheduler] Scheduler reloaded.");
    }

    /**
     * Called on shutdown
     */
    public void shutdown() {
        running = false;
        
        if (timeCheckerTask != null) {
            timeCheckerTask.cancel(false);
        }
        
        for (ScheduledFuture<?> task : runningIntervalTasks.values()) {
            task.cancel(false);
        }
        
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        LOGGER.info("[EventScheduler] Scheduler stopped.");
    }

    // Getters for commands
    public Map<String, ScheduledFuture<?>> getRunningTasks() {
        return runningIntervalTasks;
    }
    
    public EventConfig getEventConfig() {
        return config;
    }
}
