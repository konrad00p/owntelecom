package com.owntelecom.service;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.config.ConfigManager;
import com.owntelecom.database.model.Station;
import com.owntelecom.database.model.Subscriber;
import com.owntelecom.database.repository.AgreementRepository;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class CoverageService {

    private final OwnTelecomPlugin plugin;
    private final ConfigManager configManager;
    private final StationIndex stationIndex;
    private final Map<UUID, CachedCoverage> cache = new HashMap<>();

    private record CachedCoverage(CoverageResult result, long timestamp, Location location) {}

    public CoverageService(OwnTelecomPlugin plugin, StationIndex stationIndex) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.stationIndex = stationIndex;
    }

    public CoverageResult getCoverage(Player player) {
        long cacheMs = configManager.getConfig().getLong("performance.coverage-cache-ms", 500);
        int moveThreshold = configManager.getConfig().getInt("performance.coverage-move-threshold", 2);
        CachedCoverage cached = cache.get(player.getUniqueId());
        Location loc = player.getLocation();
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < cacheMs) {
            if (cached.location().getWorld().equals(loc.getWorld())
                    && cached.location().distanceSquared(loc) < moveThreshold * moveThreshold) {
                return cached.result();
            }
        }
        CoverageResult result = computeCoverage(player);
        cache.put(player.getUniqueId(), new CachedCoverage(result, System.currentTimeMillis(), loc.clone()));
        return result;
    }

    public void invalidate(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    public void rebuildIndex() {
        stationIndex.rebuild();
        cache.clear();
    }

    private CoverageResult computeCoverage(Player player) {
        Optional<Subscriber> subOpt = plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId());
        if (subOpt.isEmpty()) {
            return CoverageResult.none();
        }
        String homeOperator = subOpt.get().operatorId();
        Location loc = player.getLocation();
        List<Station> nearby = stationIndex.getNearby(loc);

        Station best = null;
        double bestScore = -1;
        String servingOperator = null;

        for (Station station : nearby) {
            if (station.broken()) {
                continue;
            }
            double range = getEffectiveRange(station);
            double dist = loc.distance(new Location(loc.getWorld(), station.x() + 0.5, station.y(), station.z() + 0.5));
            if (dist > range) {
                continue;
            }
            double score = (range - dist) / range;
            if (score > bestScore) {
                bestScore = score;
                best = station;
                servingOperator = station.operatorId();
            }
        }

        if (best == null) {
            return CoverageResult.none();
        }

        boolean roaming = !homeOperator.equalsIgnoreCase(servingOperator);
        if (roaming) {
            AgreementRepository agreements = plugin.getDatabaseManager().agreements();
            var roamingAgreement = agreements.find(homeOperator, servingOperator, com.owntelecom.database.model.AgreementType.ROAMING);
            if (roamingAgreement.isEmpty()) {
                return CoverageResult.none();
            }
        }

        double range = getEffectiveRange(best);
        double dist = loc.distance(new Location(loc.getWorld(), best.x() + 0.5, best.y(), best.z() + 0.5));
        double distPercent = Math.min(100, (dist / range) * 100);
        double speed = getSpeedMbps(best, distPercent);
        ConfigurationSection tech = getTechnology(best.technology());

        return new CoverageResult(
                true,
                roaming,
                homeOperator,
                servingOperator,
                best,
                best.technology(),
                distPercent,
                speed,
                tech != null ? tech.getDouble("voice-quality", 1.0) : 1.0,
                tech != null ? tech.getDouble("sms-quality", 1.0) : 1.0,
                tech != null && tech.getBoolean("internet-enabled", true)
        );
    }

    public double getEffectiveRange(Station station) {
        ConfigurationSection tech = getTechnology(station.technology());
        if (tech == null) {
            return 50;
        }
        double base = tech.getDouble("base-range", 50);
        ConfigurationSection level = tech.getConfigurationSection("level-bonuses." + station.level());
        double mult = level != null ? level.getDouble("range-multiplier", 1.0) : 1.0;
        return base * mult;
    }

    public double getSpeedMbps(Station station, double distancePercent) {
        ConfigurationSection tech = getTechnology(station.technology());
        if (tech == null) {
            return 1.0;
        }
        ConfigurationSection profile = tech.getConfigurationSection("speed-profile");
        double speed = tech.getDouble("base-speed-mbps", 1.0);
        if (profile != null) {
            speed = interpolateSpeed(profile, distancePercent);
        }
        ConfigurationSection level = tech.getConfigurationSection("level-bonuses." + station.level());
        double mult = level != null ? level.getDouble("speed-multiplier", 1.0) : 1.0;
        return speed * mult;
    }

    private double interpolateSpeed(ConfigurationSection profile, double distancePercent) {
        NavigableMap<Integer, Double> map = new TreeMap<>();
        for (String key : profile.getKeys(false)) {
            map.put(Integer.parseInt(key), profile.getDouble(key));
        }
        if (map.isEmpty()) {
            return 1.0;
        }
        Map.Entry<Integer, Double> floor = map.floorEntry((int) distancePercent);
        Map.Entry<Integer, Double> ceiling = map.ceilingEntry((int) distancePercent);
        if (floor == null) {
            return ceiling.getValue();
        }
        if (ceiling == null || floor.getKey().equals(ceiling.getKey())) {
            return floor.getValue();
        }
        double ratio = (distancePercent - floor.getKey()) / (ceiling.getKey() - floor.getKey());
        return floor.getValue() + (ceiling.getValue() - floor.getValue()) * ratio;
    }

    public ConfigurationSection getTechnology(String id) {
        return configManager.getTechnologies().getConfigurationSection("technologies." + id);
    }
}
