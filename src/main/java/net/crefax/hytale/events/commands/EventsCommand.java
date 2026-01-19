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
import net.crefax.hytale.events.i18n.I18nManager;

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
        showEventList(context);
        return CompletableFuture.completedFuture(null);
    }

    private void showEventList(CommandContext context) {
        EventConfig config = plugin.getConfig();
        I18nManager i18n = plugin.getI18n();
        
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.title")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.empty")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.interval_title")));
        
        for (IntervalEvent event : config.getIntervalEvents()) {
            String status = event.enabled 
                ? i18n.getMessage("commands.events.status_active") 
                : i18n.getMessage("commands.events.status_disabled");
            context.sendMessage(Message.raw(i18n.getMessage("commands.events.interval_format", 
                status, event.name, String.valueOf(event.interval))));
            context.sendMessage(Message.raw(i18n.getMessage("commands.events.commands_format", 
                String.join(", ", event.commands))));
        }
        
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.empty")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.scheduled_title")));
        
        for (ScheduledEvent event : config.getScheduledEvents()) {
            String status = event.enabled 
                ? i18n.getMessage("commands.events.status_active") 
                : i18n.getMessage("commands.events.status_disabled");
            context.sendMessage(Message.raw(i18n.getMessage("commands.events.scheduled_format", 
                status, event.name, String.join(", ", event.times))));
            context.sendMessage(Message.raw(i18n.getMessage("commands.events.commands_format", 
                String.join(", ", event.commands))));
        }
        
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.empty")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.commands_title")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.help_trigger")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.help_reload")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.help_list")));
        context.sendMessage(Message.raw(i18n.getMessage("commands.events.footer")));
    }
}
