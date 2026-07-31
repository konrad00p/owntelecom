package pl.owntelecom.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Subscription {

    public enum SubscriptionType {
        DOMESTIC,   // Pakiet krajowy
        ROAMING     // Pakiet roamingowy
    }

    private final String id;
    private final String operatorId;
    private String name;
    private String description;
    private SubscriptionType type;
    private double price;
    private int durationDays; // 0 = bezterminowy (aż do wyczerpania)
    private boolean active;
    
    // Zasoby w pakiecie
    private int minutes;
    private int sms;
    private int megabytes;
    
    // Strefy roamingowe (tylko dla ROAMING)
    private String zoneId;

    public Subscription(String id, String operatorId, String name, SubscriptionType type) {
        this.id = id;
        this.operatorId = operatorId;
        this.name = name;
        this.type = type;
        this.price = 0.0;
        this.durationDays = 30;
        this.active = true;
        this.minutes = 0;
        this.sms = 0;
        this.megabytes = 0;
        this.zoneId = "0";
        this.description = "";
    }

    // Gettery i Settery
    public String getId() { return id; }
    public String getOperatorId() { return operatorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SubscriptionType getType() { return type; }
    public void setType(SubscriptionType type) { this.type = type; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = Math.max(0, price); }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = Math.max(0, durationDays); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = Math.max(0, minutes); }
    public int getSms() { return sms; }
    public void setSms(int sms) { this.sms = Math.max(0, sms); }
    public int getMegabytes() { return megabytes; }
    public void setMegabytes(int megabytes) { this.megabytes = Math.max(0, megabytes); }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }

    // Sprawdź czy pakiet jest nielimitowany
    public boolean isUnlimitedMinutes() { return minutes == -1; }
    public boolean isUnlimitedSms() { return sms == -1; }
    public boolean isUnlimitedData() { return megabytes == -1; }

    // Pobierz opis pakietu
    public String getPackageDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("§e").append(name).append("\n");
        sb.append("§7").append(description).append("\n\n");
        
        if (minutes == -1) sb.append("§a📞 Nielimitowane minuty\n");
        else if (minutes > 0) sb.append("§a📞 ").append(minutes).append(" minut\n");
        
        if (sms == -1) sb.append("§a💬 Nielimitowane SMS\n");
        else if (sms > 0) sb.append("§a💬 ").append(sms).append(" SMS\n");
        
        if (megabytes == -1) sb.append("§a🌐 Nielimitowany internet\n");
        else if (megabytes > 0) sb.append("§a🌐 ").append(megabytes).append(" MB\n");
        
        if (durationDays > 0) {
            sb.append("\n§7Ważność: §f").append(durationDays).append(" dni\n");
        } else {
            sb.append("\n§7Ważność: §fBezterminowa (do wyczerpania)\n");
        }
        
        sb.append("§6Cena: §f$").append(String.format("%.2f", price));
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return name + " (" + type + ") - $" + price;
    }
}
