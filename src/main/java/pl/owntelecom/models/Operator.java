package pl.owntelecom.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Operator {

    private final String id; // Unikalne ID (np. "phonifyus")
    private String displayName; // Nazwa wyświetlana (np. "Phonify US")
    private UUID owner; // Właściciel operatora
    private final Map<String, Double> rates; // Stawki za usługi
    private final Map<String, Double> roamingRates; // Stawki roamingowe
    private final List<UUID> employees; // Pracownicy z uprawnieniami
    private final Map<String, String> agreements; // Umowy z innymi operatorami (ID operatora -> typ umowy)
    private String zone; // Strefa operatora (domyślnie "0")
    private boolean active; // Czy operator jest aktywny
    private final long creationDate; // Data utworzenia
    private double balance; // Saldo operatora (opcjonalne)

    public Operator(String id, String displayName, UUID owner) {
        this.id = id.toLowerCase();
        this.displayName = displayName;
        this.owner = owner;
        this.rates = new HashMap<>();
        this.roamingRates = new HashMap<>();
        this.employees = new ArrayList<>();
        this.agreements = new HashMap<>();
        this.zone = "0";
        this.active = true;
        this.creationDate = System.currentTimeMillis();
        this.balance = 0.0;
        
        // Domyślne stawki
        this.rates.put("minuta", 0.50);
        this.rates.put("sms", 0.20);
        this.rates.put("mb", 0.10);
        
        // Domyślne stawki roamingowe
        this.roamingRates.put("minuta", 1.50);
        this.roamingRates.put("sms", 0.60);
        this.roamingRates.put("mb", 0.30);
    }

    // Gettery i Settery
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public Map<String, Double> getRates() { return rates; }
    public Map<String, Double> getRoamingRates() { return roamingRates; }
    public List<UUID> getEmployees() { return employees; }
    public Map<String, String> getAgreements() { return agreements; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreationDate() { return creationDate; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getRate(String type) {
        return rates.getOrDefault(type, 0.0);
    }

    public void setRate(String type, double rate) {
        rates.put(type, rate);
    }

    public double getRoamingRate(String type) {
        return roamingRates.getOrDefault(type, 0.0);
    }

    public void setRoamingRate(String type, double rate) {
        roamingRates.put(type, rate);
    }

    public String getOwnerName() {
        Player player = Bukkit.getPlayer(owner);
        return player != null ? player.getName() : "Offline";
    }

    public boolean isOwner(UUID playerId) {
        return owner.equals(playerId);
    }

    public boolean isEmployee(UUID playerId) {
        return employees.contains(playerId);
    }

    public boolean hasAccess(UUID playerId) {
        return isOwner(playerId) || isEmployee(playerId);
    }

    public void addEmployee(UUID playerId) {
        if (!employees.contains(playerId)) {
            employees.add(playerId);
        }
    }

    public void removeEmployee(UUID playerId) {
        employees.remove(playerId);
    }

    public void addAgreement(String operatorId, String type) {
        agreements.put(operatorId.toLowerCase(), type);
    }

    public void removeAgreement(String operatorId) {
        agreements.remove(operatorId.toLowerCase());
    }

    public boolean hasAgreement(String operatorId) {
        return agreements.containsKey(operatorId.toLowerCase());
    }

    public String getAgreementType(String operatorId) {
        return agreements.get(operatorId.toLowerCase());
    }

    @Override
    public String toString() {
        return displayName + " (" + id + ")";
    }
}
