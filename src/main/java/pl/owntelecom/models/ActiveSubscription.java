package pl.owntelecom.models;

import java.util.UUID;

public class ActiveSubscription {

    private final String id;
    private final UUID playerId;
    private final String subscriptionId; // ID pakietu
    private final String operatorId;
    private long purchaseDate;
    private long expiryDate;
    private boolean active;
    
    // Pozostałe zasoby
    private int remainingMinutes;
    private int remainingSms;
    private int remainingMegabytes;

    public ActiveSubscription(UUID playerId, Subscription subscription) {
        this.id = "sub_" + playerId.toString().substring(0, 8) + "_" + System.currentTimeMillis();
        this.playerId = playerId;
        this.subscriptionId = subscription.getId();
        this.operatorId = subscription.getOperatorId();
        this.purchaseDate = System.currentTimeMillis();
        this.active = true;
        
        // Ustaw expiry
        if (subscription.getDurationDays() > 0) {
            this.expiryDate = System.currentTimeMillis() + 
                (subscription.getDurationDays() * 24L * 60 * 60 * 1000);
        } else {
            this.expiryDate = 0; // Bezterminowy
        }
        
        // Przepisz zasoby
        this.remainingMinutes = subscription.getMinutes();
        this.remainingSms = subscription.getSms();
        this.remainingMegabytes = subscription.getMegabytes();
    }

    // Gettery
    public String getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public String getSubscriptionId() { return subscriptionId; }
    public String getOperatorId() { return operatorId; }
    public long getPurchaseDate() { return purchaseDate; }
    public long getExpiryDate() { return expiryDate; }
    public boolean isActive() { return active; }
    public int getRemainingMinutes() { return remainingMinutes; }
    public int getRemainingSms() { return remainingSms; }
    public int getRemainingMegabytes() { return remainingMegabytes; }

    // Sprawdź czy subskrypcja wygasła
    public boolean isExpired() {
        if (expiryDate == 0) return false; // Bezterminowa
        return System.currentTimeMillis() > expiryDate;
    }

    // Sprawdź czy zasoby się skończyły
    public boolean isDepleted() {
        boolean minutesDepleted = remainingMinutes == 0 && remainingMinutes != -1;
        boolean smsDepleted = remainingSms == 0 && remainingSms != -1;
        boolean dataDepleted = remainingMegabytes == 0 && remainingMegabytes != -1;
        return minutesDepleted && smsDepleted && dataDepleted;
    }

    // Użyj minut
    public boolean useMinutes(int amount) {
        if (remainingMinutes == -1) return true; // Nielimitowane
        if (remainingMinutes >= amount) {
            remainingMinutes -= amount;
            return true;
        }
        return false;
    }

    // Użyj SMS
    public boolean useSms(int amount) {
        if (remainingSms == -1) return true; // Nielimitowane
        if (remainingSms >= amount) {
            remainingSms -= amount;
            return true;
        }
        return false;
    }

    // Użyj danych
    public boolean useMegabytes(int amount) {
        if (remainingMegabytes == -1) return true; // Nielimitowane
        if (remainingMegabytes >= amount) {
            remainingMegabytes -= amount;
            return true;
        }
        return false;
    }

    // Sprawdź czy ma jeszcze jakieś zasoby
    public boolean hasResources() {
        return remainingMinutes != 0 || remainingSms != 0 || remainingMegabytes != 0 ||
               remainingMinutes == -1 || remainingSms == -1 || remainingMegabytes == -1;
    }

    // Dezaktywuj
    public void deactivate() {
        this.active = false;
    }

    // Pobierz dni do wygaśnięcia
    public long getDaysUntilExpiry() {
        if (expiryDate == 0) return -1;
        long remaining = expiryDate - System.currentTimeMillis();
        return Math.max(0, remaining / (24 * 60 * 60 * 1000));
    }

    // Pobierz status w formie tekstu
    public String getStatusText() {
        if (!active) return "§cNieaktywna";
        if (isExpired()) return "§cWygasła";
        
        StringBuilder sb = new StringBuilder();
        sb.append("§aAktywna");
        
        if (remainingMinutes == -1) sb.append(" | §b📞 ∞");
        else if (remainingMinutes > 0) sb.append(" | §b📞 ").append(remainingMinutes).append("min");
        
        if (remainingSms == -1) sb.append(" §b💬 ∞");
        else if (remainingSms > 0) sb.append(" §b💬 ").append(remainingSms).append("sms");
        
        if (remainingMegabytes == -1) sb.append(" §b🌐 ∞");
        else if (remainingMegabytes > 0) sb.append(" §b🌐 ").append(remainingMegabytes).append("MB");
        
        if (expiryDate > 0) {
            sb.append(" | §e⏰ ").append(getDaysUntilExpiry()).append("d");
        }
        
        return sb.toString();
    }
}
