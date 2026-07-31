package pl.owntelecom.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class ServerRoom {

    private final String id;
    private UUID owner;
    private Location location;
    private int level; // 1-3
    private int maxSlots; // Maksymalna liczba stron
    private int usedSlots; // Zajęte sloty
    private int maxVisitors; // Maksymalna liczba równoczesnych odwiedzin
    private boolean active;
    private double failureChance;
    private final List<String> hostedSites; // Lista ID hostowanych stron
    private final Map<UUID, Long> rentedSlots; // Najemcy -> ID strony
    private double rentalPrice; // Cena wynajmu slotu

    public ServerRoom(String id, UUID owner, Location location) {
        this.id = id;
        this.owner = owner;
        this.location = location;
        this.level = 1;
        this.maxSlots = 5;
        this.usedSlots = 0;
        this.maxVisitors = 10;
        this.active = true;
        this.failureChance = 10.0;
        this.hostedSites = new ArrayList<>();
        this.rentedSlots = new HashMap<>();
        this.rentalPrice = 100.0;
    }

    // Gettery i Settery
    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public Location getLocation() { return location; }
    public int getLevel() { return level; }
    
    public void setLevel(int level) {
        this.level = Math.min(3, Math.max(1, level));
        updateStats();
    }
    
    public int getMaxSlots() { return maxSlots; }
    public int getUsedSlots() { return usedSlots; }
    public int getFreeSlots() { return maxSlots - usedSlots; }
    public int getMaxVisitors() { return maxVisitors; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public double getFailureChance() { return failureChance; }
    public List<String> getHostedSites() { return hostedSites; }
    public Map<UUID, Long> getRentedSlots() { return rentedSlots; }
    public double getRentalPrice() { return rentalPrice; }
    public void setRentalPrice(double price) { this.rentalPrice = price; }

    private void updateStats() {
        switch (level) {
            case 1:
                this.maxSlots = 5;
                this.maxVisitors = 10;
                this.failureChance = 10.0;
                break;
            case 2:
                this.maxSlots = 15;
                this.maxVisitors = 30;
                this.failureChance = 5.0;
                break;
            case 3:
                this.maxSlots = 50;
                this.maxVisitors = 100;
                this.failureChance = 2.0;
                break;
        }
    }

    public boolean addSite(String siteId) {
        if (usedSlots >= maxSlots) return false;
        hostedSites.add(siteId);
        usedSlots++;
        return true;
    }

    public boolean removeSite(String siteId) {
        if (hostedSites.remove(siteId)) {
            usedSlots--;
            return true;
        }
        return false;
    }

    public boolean rentSlot(UUID tenant, String siteId) {
        if (getFreeSlots() <= 0) return false;
        rentedSlots.put(tenant, System.currentTimeMillis());
        return addSite(siteId);
    }

    public double getUpgradeCost() {
        switch (level) {
            case 1: return 10000.0;
            case 2: return 50000.0;
            default: return -1;
        }
    }

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
            return new Location(world, 
                Integer.parseInt(parts[1]), 
                Integer.parseInt(parts[2]), 
                Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
