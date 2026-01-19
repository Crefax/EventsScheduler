package net.crefax.hytale.events.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import net.crefax.hytale.events.EventSchedulerMod;
import net.crefax.hytale.events.manager.SchedulerManager;
import net.crefax.hytale.events.i18n.I18nManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * /eventsreload Command
 * 
 * Reloads the configuration.
 * 
 * @author Crefax
 */
public class EventsReloadCommand extends AbstractCommand {

    private final EventSchedulerMod plugin;

    public EventsReloadCommand(EventSchedulerMod plugin) {
        super("eventsreload", "Reload event config");
        this.plugin = plugin;
        // Only OP can use
        setPermissionGroups("OP");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        SchedulerManager scheduler = plugin.getSchedulerManager();
        I18nManager i18n = plugin.getI18n();
        
        scheduler.reload();
        i18n.reload();
        i18n.setLanguage(plugin.getConfig().getSettings().language);
        
        context.sendMessage(Message.raw(i18n.getPrefix() + i18n.getMessage("commands.reload.success")));
        
        return CompletableFuture.completedFuture(null);
    }
}
