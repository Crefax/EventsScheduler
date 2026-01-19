package net.crefax.hytale.events.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import net.crefax.hytale.events.EventSchedulerMod;
import net.crefax.hytale.events.manager.SchedulerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * /eventstrigger --eventName <eventName> Command
 * 
 * Triggers an event manually.
 * 
 * @author Crefax
 */
public class EventsTriggerCommand extends AbstractCommand {

    private final EventSchedulerMod plugin;
    private final OptionalArg<String> eventNameArg;

    public EventsTriggerCommand(EventSchedulerMod plugin) {
        super("eventstrigger", "Trigger event manually");
        this.plugin = plugin;
        this.eventNameArg = withOptionalArg("eventName", "Event name", ArgTypes.STRING);
        // Only OP can use
        setPermissionGroups("OP");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String eventName = context.get(eventNameArg);
        
        if (eventName == null || eventName.isEmpty()) {
            context.sendMessage(Message.raw("[Events] Usage: /eventstrigger --eventName <eventName>"));
            return CompletableFuture.completedFuture(null);
        }
        
        SchedulerManager scheduler = plugin.getSchedulerManager();
        boolean found = scheduler.triggerEvent(eventName);
        
        if (found) {
            context.sendMessage(Message.raw("[Events] '" + eventName + "' event triggered!"));
        } else {
            context.sendMessage(Message.raw("[Events] No event found with name '" + eventName + "'!"));
        }
        
        return CompletableFuture.completedFuture(null);
    }
}
