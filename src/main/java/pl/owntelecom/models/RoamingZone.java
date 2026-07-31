package pl.owntelecom.models;

import java.util.*;

public class RoamingZone {

    private final String id;
    private String displayName;
    private boolean isHomeZone; // Strefa 0 = home
    private final Map<String, Double> rates; // Stawki w tej strefie
    private final List<String> operatorsInZone; // Operatorzy w strefie
    private boolean active;

    public RoamingZone(String id, String displayName, boolean isHomeZone) {
        this.id = id;
        this.displayName = displayName;
        this.isHomeZone = isHomeZone;
        this.rates = new HashMap<>();
        this.operatorsInZone = new ArrayList<>();
        this.active = true;
        
        // Domyślne stawki
        if (!isHomeZone) {
            rates.put("minuta", 1.50);
            rates.put("sms", 0.60);
            rates.put("mb", 0.30);
        } else {
            rates.put("minuta", 0.50);
            rates.put("sms", 0.20);
            rates.put("mb", 0.10);
        }
    }

    // Gettery i Settery
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isHomeZone() { return isHomeZone; }
    public Map<String, Double> getRates() { return rates; }
    public List<String> getOperatorsInZone() { return operatorsInZone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getRate(String type) {
        return rates.getOrDefault(type, isHomeZone ? 0.50 : 1.50);
    }

    public void setRate(String type, double rate) {
        rates.put(type, rate);
    }

    public void addOperator(String operatorId) {
        if (!operatorsInZone.contains(operatorId)) {
            operatorsInZone.add(operatorId);
        }
    }

    public void removeOperator(String operatorId) {
        operatorsInZone.remove(operatorId);
    }

    public boolean hasOperator(String operatorId) {
        return operatorsInZone.contains(operatorId);
    }

    public boolean isRoamingZone() {
        return !isHomeZone;
    }
}
