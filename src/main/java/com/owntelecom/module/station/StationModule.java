package com.owntelecom.module.station;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Operator;
import com.owntelecom.database.model.Station;
import com.owntelecom.module.OwnTelecomModule;
import com.owntelecom.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class StationModule implements OwnTelecomModule {

    private OwnTelecomPlugin plugin;
    private BukkitTask failureTask;

    @Override
    public void enable(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
        startFailureScheduler();
    }

    @Override
    public void disable(OwnTelecomPlugin plugin) {
        if (failureTask != null) {
            failureTask.cancel();
        }
    }

    @Override
    public String getName() {
        return "Station";
    }

    private void startFailureScheduler() {
        if (!plugin.getConfigManager().getStations().getBoolean("failure.enabled", true)) {
            return;
        }
        long interval = plugin.getConfigManager().getConfig().getLong("performance.failure-check-interval-minutes", 30);
        failureTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkFailures,
                interval * 60 * 20L, interval * 60 * 20L);
    }

    private void checkFailures() {
        var stations = plugin.getDatabaseManager().stations();
        var coverage = plugin.getModuleManager().getCoverageService();
        for (var world : Bukkit.getWorlds()) {
            for (Station station : stations.findByWorld(world.getName())) {
                if (station.broken()) {
                    continue;
                }
                ConfigurationSection tech = coverage.getTechnology(station.technology());
                if (tech == null) {
                    continue;
                }
                double baseChance = tech.getDouble("failure-chance-base", 0.05);
                ConfigurationSection level = tech.getConfigurationSection("level-bonuses." + station.level());
                double reduction = level != null ? level.getDouble("failure-reduction", 0) : 0;
                double chance = baseChance * (1 - reduction);
                if (ThreadLocalRandom.current().nextDouble() < chance) {
                    plugin.getDatabaseManager().stations().setBroken(station.id(), true);
                    notifyOwner(station);
                }
            }
        }
    }

    private void notifyOwner(Station station) {
        if (!plugin.getConfigManager().getStations().getBoolean("failure.notify-owner", true)) {
            return;
        }
        Optional<Operator> op = plugin.getDatabaseManager().operators().findById(station.operatorId());
        if (op.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player owner = Bukkit.getPlayer(op.get().ownerUuid());
            if (owner != null) {
                plugin.getMessageService().send(owner, "station.broken", Map.of("id", String.valueOf(station.id())));
            }
        });
    }

    public Optional<Station> createStation(Player player, Operator operator, String technologyId) {
        ConfigurationSection tech = plugin.getModuleManager().getCoverageService().getTechnology(technologyId);
        if (tech == null) {
            return Optional.empty();
        }

        Material required = Material.matchMaterial(tech.getString("block", "IRON_BLOCK"));
        if (required == null) {
            required = Material.IRON_BLOCK;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != required) {
            return Optional.empty();
        }

        double cost = tech.getDouble("create-cost", 0);
        EconomyService economy = plugin.getEconomyService();
        if (cost > 0 && !economy.withdraw(player, cost)) {
            return Optional.empty();
        }

        Station station = new Station(
                0,
                operator.id(),
                target.getWorld().getName(),
                target.getX(),
                target.getY(),
                target.getZ(),
                technologyId,
                1,
                false,
                System.currentTimeMillis()
        );
        int id = plugin.getDatabaseManager().stations().create(station);
        Station created = new Station(id, station.operatorId(), station.world(), station.x(), station.y(),
                station.z(), station.technology(), station.level(), false, station.lastMaintenance());
        plugin.getModuleManager().getStationIndex().addStation(created);
        return Optional.of(created);
    }

    public boolean upgradeStation(Player player, Station station) {
        ConfigurationSection tech = plugin.getModuleManager().getCoverageService().getTechnology(station.technology());
        if (tech == null) {
            return false;
        }
        int maxLevel = tech.getInt("max-level", 3);
        if (station.level() >= maxLevel) {
            return false;
        }
        int nextLevel = station.level() + 1;
        ConfigurationSection levelCfg = tech.getConfigurationSection("level-bonuses." + nextLevel);
        if (levelCfg == null) {
            return false;
        }
        double cost = levelCfg.getDouble("upgrade-cost", 0);
        if (cost > 0 && !plugin.getEconomyService().withdraw(player, cost)) {
            return false;
        }
        plugin.getDatabaseManager().stations().setLevel(station.id(), nextLevel);
        plugin.getModuleManager().getStationIndex().rebuild();
        return true;
    }

    public boolean repairStation(Player player, Station station) {
        ConfigurationSection tech = plugin.getModuleManager().getCoverageService().getTechnology(station.technology());
        if (tech == null) {
            return false;
        }
        double cost = tech.getConfigurationSection("maintenance") != null
                ? tech.getConfigurationSection("maintenance").getDouble("repair-cost", 0) : 0;
        if (cost > 0 && !plugin.getEconomyService().withdraw(player, cost)) {
            return false;
        }
        plugin.getDatabaseManager().stations().setBroken(station.id(), false);
        return true;
    }
}
