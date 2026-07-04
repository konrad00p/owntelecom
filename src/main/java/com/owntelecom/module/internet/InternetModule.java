package com.owntelecom.module.internet;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Website;
import com.owntelecom.module.OwnTelecomModule;
import com.owntelecom.service.CoverageResult;
import com.owntelecom.service.SignalService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;

public class InternetModule implements OwnTelecomModule {

    private OwnTelecomPlugin plugin;

    @Override
    public void enable(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void disable(OwnTelecomPlugin plugin) {
    }

    @Override
    public String getName() {
        return "Internet";
    }

    public List<Website> listAvailableSites() {
        return plugin.getDatabaseManager().websites().findEnabled();
    }

    public void loadPage(Player player, Website website, Runnable onComplete) {
        CoverageResult cov = plugin.getModuleManager().getCoverageService().getCoverage(player);
        if (!cov.inCoverage() || !cov.internetEnabled()) {
            plugin.getMessageService().send(player, "errors.no-internet-tech");
            return;
        }

        int mbCost = plugin.getConfigManager().getConfig().getInt("internet.click-cost-mb", 5);
        long delay = SignalService.loadingDelayMs(cov.speedMbps(), mbCost);
        String loading = plugin.getConfigManager().getConfig().getString("internet.loading-message", "&7Wczytywanie strony...");
        player.sendMessage(plugin.getMessageService().colorize(loading));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!plugin.getModuleManager().getPaymentModule().chargeMb(player, mbCost)) {
                return;
            }
            displayPage(player, website);
            plugin.getMessageService().send(player, "internet.page-loaded");
            if (onComplete != null) {
                onComplete.run();
            }
        }, Math.max(1, delay / 50));
    }

    public void displayPage(Player player, Website website) {
        List<String> lines = plugin.getDatabaseManager().websites().getLines(website.id());
        player.sendMessage(plugin.getMessageService().colorize("&b=== " + website.title() + " ==="));
        for (String line : lines) {
            player.sendMessage(plugin.getMessageService().colorize(line));
        }
    }

    public void postSocialMedia(Player player, String message) {
        if (!plugin.getConfigManager().getInternet().getBoolean("social-media.enabled", true)) {
            return;
        }
        CoverageResult cov = plugin.getModuleManager().getCoverageService().getCoverage(player);
        if (!cov.inCoverage() || !cov.internetEnabled()) {
            plugin.getMessageService().send(player, "errors.no-coverage");
            return;
        }
        int mb = plugin.getConfigManager().getInternet().getInt("social-media.post-cost-mb", 2);
        if (!plugin.getModuleManager().getPaymentModule().chargeMb(player, mb)) {
            return;
        }
        String format = plugin.getConfigManager().getInternet().getString("social-media.format",
                "&b[@{author}] &f{message}");
        String formatted = format.replace("{author}", player.getName()).replace("{message}", message);
        for (Player target : Bukkit.getOnlinePlayers()) {
            CoverageResult tcov = plugin.getModuleManager().getCoverageService().getCoverage(target);
            if (tcov.inCoverage() && tcov.internetEnabled()) {
                target.sendMessage(plugin.getMessageService().colorize(formatted));
            }
        }
    }

    public int createWebsite(Player player, String slug, String title) {
        Website website = new Website(
                0,
                slug,
                player.getUniqueId(),
                plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId())
                        .map(s -> s.operatorId()).orElse(null),
                null,
                title,
                true,
                false,
                "default"
        );
        return plugin.getDatabaseManager().websites().create(website);
    }
}
