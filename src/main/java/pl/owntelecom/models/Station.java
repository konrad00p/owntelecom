package pl.owntelecom.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class Station {

    private final String id;
    private final String operatorId;
    private Location location;
    private int level; // 1-3
    private String technology; // "2G", "LTE", "5G"
    private boolean active;
    private boolean broken;
    private double damageChance; // Szansa na awarię
    private final long creationDate;
    private UUID createdBy;

    public Station(String id, String operatorId, Location location, String technology, UUID createdBy) {
        this.id = id;
        this.operatorId = operatorId;
        this.location = location;
        this.level = 1;
        this.technology = technology;
        this.active = true;
        this.broken = false;
        this.damageChance = 10.0; // 10% bazowo dla poziomu 1
        this.creationDate = System.currentTimeMillis();
        this.createdBy = createdBy;
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
        // Szansa na awarię maleje z poziomem
        switch (level) {
            case 1: this.damageChance = 15.0; break;
            case 2: this.damageChance = 8.0; break;
            case 3: this.damageChance = 3.0; break;
        }
    }
    
    public long getCreationDate() { return creationDate; }
    public UUID getCreatedBy() { return createdBy; }

    // Zasięg stacji w zależności od poziomu i technologii
    public double getBaseRange() {
        double baseRange;
        switch (technology) {
            case "2G": baseRange = 80.0; break;
            case "LTE": baseRange = 60.0; break;
            case "5G": baseRange = 40.0; break;
            default: baseRange = 50.0;
        }
        
        // Bonus za poziom
        switch (level) {
            case 2: return baseRange * 1.3;
            case 3: return baseRange * 1.6;
            default: return baseRange;
        }
    }

    // Prędkość internetu w Mb/s
    public double getBaseSpeed() {
        switch (technology) {
            case "2G": return 0.5;
            case "LTE": return 100.0;
            case "5G": return 500.0;
            default: return 1.0;
        }
    }

    // Obliczanie rzeczywistej prędkości na podstawie odległości
    public double getSpeedAtDistance(double distance) {
        double baseSpeed = getBaseSpeed();
        double range = getBaseRange();
        
        if (distance > range) return 0.0;
        if (distance < 5) return baseSpeed;
        
        // Spadek prędkości: im dalej, tym wolniej
        double ratio = 1.0 - (distance / range);
        double minSpeed = baseSpeed * 0.01; // Minimum 1% prędkości
        
        return Math.max(minSpeed, baseSpeed * ratio);
    }

    // Jakość połączenia (0.0 - 1.0)
    public double getSignalQuality(Location playerLocation) {
        if (!active || broken) return 0.0;
        
        double distance = location.distance(playerLocation);
        double range = getBaseRange();
        
        if (distance > range) return 0.0;
        if (distance < 5) return 1.0;
        
        double quality = 1.0 - (distance / range);
        return Math.max(0.0, quality);
    }

    // Sprawdź czy gracz jest w zasięgu
    public boolean isInRange(Location playerLocation) {
        if (!active || broken) return false;
        return location.getWorld().equals(playerLocation.getWorld()) && 
               location.distance(playerLocation) <= getBaseRange();
    }

    // Koszt ulepszenia
    public double getUpgradeCost() {
        switch (level) {
            case 1: return 5000.0;  // 1 -> 2
            case 2: return 15000.0; // 2 -> 3
            default: return -1; // Nie można ulepszyć
        }
    }

    // Koszt naprawy
    public double getRepairCost() {
        return 1000.0 * level;
    }

    @Override
    public String toString() {
        return "Station{id='" + id + "', tech='" + technology + "', level=" + level + "}";
    }

    // Serializacja lokalizacji do stringa
    public String locationToString() {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }

    // Deserializacja lokalizacji
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
