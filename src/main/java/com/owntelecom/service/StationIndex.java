package com.owntelecom.service;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Station;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StationIndex {

    private final OwnTelecomPlugin plugin;
    private final int chunkSize;
    private final Map<String, Map<Long, List<Station>>> worldIndex = new ConcurrentHashMap<>();

    public StationIndex(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
        this.chunkSize = plugin.getConfigManager().getConfig().getInt("performance.spatial-chunk-size", 16);
    }

    public void rebuild() {
        worldIndex.clear();
        for (String worldName : Bukkit.getWorlds().stream().map(w -> w.getName()).toList()) {
            for (Station station : plugin.getDatabaseManager().stations().findByWorld(worldName)) {
                addToIndex(station);
            }
        }
    }

    public void addStation(Station station) {
        addToIndex(station);
    }

    public void removeStation(Station station) {
        long key = chunkKey(station.x(), station.z());
        Map<Long, List<Station>> index = worldIndex.get(station.world());
        if (index == null) {
            return;
        }
        List<Station> list = index.get(key);
        if (list != null) {
            list.removeIf(s -> s.id() == station.id());
        }
    }

    public List<Station> getNearby(Location location) {
        Map<Long, List<Station>> index = worldIndex.get(location.getWorld().getName());
        if (index == null) {
            return List.of();
        }
        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;
        Set<Station> result = new LinkedHashSet<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int bx = (cx + dx) * 16;
                int bz = (cz + dz) * 16;
                List<Station> chunk = index.get(chunkKey(bx, bz));
                if (chunk != null) {
                    result.addAll(chunk);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private void addToIndex(Station station) {
        long key = chunkKey(station.x(), station.z());
        worldIndex.computeIfAbsent(station.world(), w -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(station);
    }

    private long chunkKey(int x, int z) {
        int cx = Math.floorDiv(x, chunkSize);
        int cz = Math.floorDiv(z, chunkSize);
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
