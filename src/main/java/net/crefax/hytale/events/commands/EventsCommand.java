package net.crefax.hytale.events.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;

import net.crefax.hytale.events.EventSchedulerMod;
import net.crefax.hytale.events.config.EventConfig;
import net.crefax.hytale.events.config.EventConfig.IntervalEvent;
import net.crefax.hytale.events.config.EventConfig.ScheduledEvent;
import net.crefax.hytale.events.manager.SchedulerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * /events Command
 * 
 * Event scheduler management command.
 * Usage: /events <list|reload|help>
 * 
 * @author Crefax
 */
public class EventsCommand extends AbstractCommand {

    private final EventSchedulerMod plugin;

    public EventsCommand(EventSchedulerMod plugin) {
        super("events", "Event scheduler management command");
        this.plugin = plugin;
        // Only OP can use
        setPermissionGroups("OP");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        // list, reload veya help
        showEventList(context);
        return CompletableFuture.completedFuture(null);
    }

    private void showEventList(CommandContext context) {
        EventConfig config = plugin.getConfig();
        
        context.sendMessage(Message.raw("========== Events Scheduler =========="));
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("--- Interval Events ---"));
        
        for (IntervalEvent event : config.getIntervalEvents()) {
            String status = event.enabled ? "[ACTIVE]" : "[DISABLED]";
            context.sendMessage(Message.raw(status + " " + event.name + " - Every " + event.interval + " seconds"));
            context.sendMessage(Message.raw("  Commands: " + String.join(", ", event.commands)));
        }
        
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("--- Scheduled Events ---"));
        
        for (ScheduledEvent event : config.getScheduledEvents()) {
            String status = event.enabled ? "[ACTIVE]" : "[DISABLED]";
            context.sendMessage(Message.raw(status + " " + event.name + " - Times: " + String.join(", ", event.times)));
            context.sendMessage(Message.raw("  Commands: " + String.join(", ", event.commands)));
        }
        
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("Commands:"));
        context.sendMessage(Message.raw("  /eventstrigger --eventName <name> - Trigger event manually"));
        context.sendMessage(Message.raw("  /eventsreload - Reload config"));
        context.sendMessage(Message.raw("  /events - Show event list"));
        context.sendMessage(Message.raw("======================================="));
    }
}
