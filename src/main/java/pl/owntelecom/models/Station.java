package pl.owntelecom.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import pl.owntelecom.managers.StationManager;

import java.util.UUID;

public class Station {

    private final String id;
    private final String operatorId;
    private Location location;
    private int level; // 1-3
    private String technology; // np. "2G", "LTE", "5G", "WiFi" itd.
    private boolean active;
    private boolean broken;
    private double damageChance; // Szansa na awarię
    private final long creationDate;
    private UUID createdBy;
    private final StationManager manager; // Referencja do menedżera stacji

    public Station(String id, String operatorId, Location location, String technology,
                   UUID createdBy, StationManager manager) {
        this.id = id;
        this.operatorId = operatorId;
        this.location = location;
        this.level = 1;
        this.technology = technology;
        this.active = true;
        this.broken = false;
        this.damageChance = 15.0; // bazowo dla poziomu 1
        this.creationDate = System.currentTimeMillis();
        this.createdBy = createdBy;
        this.manager = manager;
        updateDamageChance();
    }

    // Gettery i Settery
    public String getId() { return id; }
    public String getOperatorId() { return operatorId; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public int getLevel() { return level; }

    public void setLevel(int level) {
        this.level = Math.min(3, Math.max(1, level));
        updateDamageChance();
    }

    public String getTechnology() { return technology; }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isBroken() { return broken; }

    public void setBroken(boolean broken) {
        this.broken = broken;
        if (broken) {
            this.active = false;
        }
    }

    public double getDamageChance() { return damageChance; }

    private void updateDamageChance() {
        // Można podpiąć pod config, ale zostawiamy proste wartości
        switch (level) {
            case 1: this.damageChance = 15.0; break;
            case 2: this.damageChance = 8.0; break;
            case 3: this.damageChance = 3.0; break;
        }
    }

    public long getCreationDate() { return creationDate; }
    public UUID getCreatedBy() { return createdBy; }

    // ========== DYNAMICZNE ODCZYTY Z KONFIGURACJI ==========

    /**
     * Bazowy zasięg stacji w blokach, wczytany z technologies.yml
     */
    public double getBaseRange() {
        if (manager != null) {
            double base = manager.getTechnologyRange(technology);
            // Bonus za poziom stacji
            switch (level) {
                case 2: return base * 1.3;
                case 3: return base * 1.6;
                default: return base;
            }
        }
        // Fallback, gdyby menedżera nie było
        return 50.0;
    }

    /**
     * Bazowa prędkość internetu w Mb/s, wczytana z technologies.yml
     */
    public double getBaseSpeed() {
        if (manager != null) {
            return manager.getTechnologySpeed(technology);
        }
        return 1.0;
    }

    /**
     * Czy technologia obsługuje internet
     */
    public boolean supportsInternet() {
        if (manager != null) {
            return manager.isInternetSupported(technology);
        }
        return false;
    }

    /**
     * Rzeczywista prędkość w zależności od odległości od stacji
     */
    public double getSpeedAtDistance(double distance) {
        double baseSpeed = getBaseSpeed();
        double range = getBaseRange();

        if (distance > range) return 0.0;
        if (distance < 5) return baseSpeed; // blisko stacji - pełna prędkość

        double ratio = 1.0 - (distance / range);
        double minSpeed = baseSpeed * 0.01; // minimum 1% prędkości bazowej
        return Math.max(minSpeed, baseSpeed * ratio);
    }

    /**
     * Jakość sygnału 0.0 - 1.0
     */
    public double getSignalQuality(Location playerLocation) {
        if (!active || broken) return 0.0;

        if (!location.getWorld().equals(playerLocation.getWorld())) return 0.0;

        double distance = location.distance(playerLocation);
        double range = getBaseRange();

        if (distance > range) return 0.0;
        if (distance < 5) return 1.0;

        double quality = 1.0 - (distance / range);
        return Math.max(0.0, quality);
    }

    /**
     * Czy gracz znajduje się w zasięgu
     */
    public boolean isInRange(Location playerLocation) {
        if (!active || broken) return false;
        if (!location.getWorld().equals(playerLocation.getWorld())) return false;
        return location.distance(playerLocation) <= getBaseRange();
    }

    /**
     * Koszt ulepszenia stacji (można podpiąć pod config, tutaj stałe)
     */
    public double getUpgradeCost() {
        switch (level) {
            case 1: return 5000.0;
            case 2: return 15000.0;
            default: return -1;
        }
    }

    /**
     * Koszt naprawy stacji
     */
    public double getRepairCost() {
        return 1000.0 * level;
    }

    @Override
    public String toString() {
        return "Station{id='" + id + "', tech='" + technology + "', level=" + level + "}";
    }

    // Serializacja lokalizacji
    public String locationToString() {
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    public static Location stringToLocation(String str) {
        String[] parts = str.split(",");
        if (parts.length != 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
