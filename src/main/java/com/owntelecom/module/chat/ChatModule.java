package com.owntelecom.module.chat;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.module.OwnTelecomModule;
import com.owntelecom.service.CoverageResult;
import com.owntelecom.service.MessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatModule implements OwnTelecomModule, Listener {

    private OwnTelecomPlugin plugin;
    private boolean globalOverride;

    @Override
    public void enable(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
        this.globalOverride = plugin.getConfigManager().getConfig().getBoolean("chat.global-override", false);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable(OwnTelecomPlugin plugin) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public String getName() {
        return "Chat";
    }

    public void setGlobalOverride(boolean enabled) {
        this.globalOverride = enabled;
    }

    public boolean isGlobalOverride() {
        return globalOverride;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("chat.enabled", true)) {
            return;
        }
        Player sender = event.getPlayer();
        if (globalOverride || sender.hasPermission("owntelecom.bypass.chat")) {
            return;
        }

        if (plugin.getModuleManager().getCallModule().isInCall(sender.getUniqueId())) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    plugin.getModuleManager().getCallModule().sendCallMessage(sender, message));
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        handleLocalChat(sender, message);
    }

    public void handleLocalChat(Player sender, String message) {
        MessageService messages = plugin.getMessageService();
        double radius = plugin.getConfigManager().getConfig().getDouble("chat.local-radius", 10);
        String format = plugin.getConfigManager().getConfig().getString("chat.format-local",
                "&7[LOCAL] &f{player}&7: &f{message}");

        List<Player> listeners = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.getWorld().equals(sender.getWorld())) {
                continue;
            }
            if (target.equals(sender) || target.getLocation().distance(sender.getLocation()) <= radius) {
                listeners.add(target);
            }
        }

        String formatted = format
                .replace("{player}", sender.getName())
                .replace("{message}", message);

        for (Player target : listeners) {
            target.sendMessage(messages.colorize(formatted));
        }

        if (listeners.size() <= 1) {
            messages.send(sender, "chat.no-listeners");
        }
    }

    public void broadcastInternet(Player sender, String message) {
        CoverageResult senderCoverage = plugin.getModuleManager().getCoverageService().getCoverage(sender);
        if (!senderCoverage.inCoverage() || !senderCoverage.internetEnabled()) {
            plugin.getMessageService().send(sender, "errors.no-coverage");
            return;
        }

        MessageService messages = plugin.getMessageService();
        String format = "&b[NET] &f{player}&7: &f{message}".replace("{player}", sender.getName()).replace("{message}", message);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(sender)) {
                target.sendMessage(messages.colorize(format));
                continue;
            }
            CoverageResult cov = plugin.getModuleManager().getCoverageService().getCoverage(target);
            if (cov.inCoverage() && cov.internetEnabled()) {
                target.sendMessage(messages.colorize(format));
            }
        }
    }
}
