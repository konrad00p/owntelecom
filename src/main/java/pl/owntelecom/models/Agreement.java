package pl.owntelecom.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Agreement {

    public enum AgreementType {
        CALLS,      // Umowa o połączenia
        SMS,        // Umowa o SMS
        ROAMING,    // Umowa roamingowa
        FULL        // Pełna umowa (połączenia + SMS + roaming)
    }

    public enum AgreementStatus {
        PENDING,    // Oczekuje na akceptację
        ACTIVE,     // Aktywna
        REJECTED,   // Odrzucona
        EXPIRED,    // Wygasła
        TERMINATED  // Zerwana
    }

    private final String id;
    private final String operatorA; // Operator inicjujący
    private final String operatorB; // Operator docelowy
    private AgreementType type;
    private AgreementStatus status;
    private final Map<String, Double> ratesAtoB; // Stawki A -> B
    private final Map<String, Double> ratesBtoA; // Stawki B -> A
    private String zoneId; // Strefa roamingowa (dla umów roamingowych)
    private final long creationDate;
    private long expiryDate;
    private boolean autoRenew;

    public Agreement(String operatorA, String operatorB, AgreementType type) {
        this.id = "agreement_" + operatorA + "_" + operatorB + "_" + System.currentTimeMillis();
        this.operatorA = operatorA;
        this.operatorB = operatorB;
        this.type = type;
        this.status = AgreementStatus.PENDING;
        this.ratesAtoB = new HashMap<>();
        this.ratesBtoA = new HashMap<>();
        this.zoneId = "0";
        this.creationDate = System.currentTimeMillis();
        this.expiryDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000); // 30 dni
        this.autoRenew = false;
        
        // Domyślne stawki
        setDefaultRates();
    }

    private void setDefaultRates() {
        // Domyślne stawki A -> B
        ratesAtoB.put("minuta", 0.50);
        ratesAtoB.put("sms", 0.20);
        ratesAtoB.put("mb", 0.10);
        
        // Domyślne stawki B -> A (symetryczne)
        ratesBtoA.put("minuta", 0.50);
        ratesBtoA.put("sms", 0.20);
        ratesBtoA.put("mb", 0.10);
    }

    // Gettery
    public String getId() { return id; }
    public String getOperatorA() { return operatorA; }
    public String getOperatorB() { return operatorB; }
    public AgreementType getType() { return type; }
    public void setType(AgreementType type) { this.type = type; }
    public AgreementStatus getStatus() { return status; }
    public void setStatus(AgreementStatus status) { this.status = status; }
    public Map<String, Double> getRatesAtoB() { return ratesAtoB; }
    public Map<String, Double> getRatesBtoA() { return ratesBtoA; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public long getCreationDate() { return creationDate; }
    public long getExpiryDate() { return expiryDate; }
    public void setExpiryDate(long expiryDate) { this.expiryDate = expiryDate; }
    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }

    // Ustawianie stawek
    public void setRateAtoB(String type, double rate) {
        ratesAtoB.put(type, Math.max(0, rate));
    }

    public void setRateBtoA(String type, double rate) {
        ratesBtoA.put(type, Math.max(0, rate));
    }

    public double getRateAtoB(String type) {
        return ratesAtoB.getOrDefault(type, 0.50);
    }

    public double getRateBtoA(String type) {
        return ratesBtoA.getOrDefault(type, 0.50);
    }

    // Sprawdź czy umowa jest aktywna
    public boolean isActive() {
        return status == AgreementStatus.ACTIVE && 
               (expiryDate > System.currentTimeMillis() || expiryDate == 0);
    }

    // Sprawdź czy umowa wygasła
    public boolean isExpired() {
        return expiryDate > 0 && expiryDate < System.currentTimeMillis();
    }

    // Sprawdź czy umowa dotyczy danych operatorów
    public boolean involves(String op1, String op2) {
        return (operatorA.equals(op1) && operatorB.equals(op2)) ||
               (operatorA.equals(op2) && operatorB.equals(op1));
    }

    // Sprawdź czy umowa dotyczy operatora
    public boolean involvesOperator(String operatorId) {
        return operatorA.equals(operatorId) || operatorB.equals(operatorId);
    }

    // Pobierz stawkę dla kierunku
    public double getRate(String fromOperator, String toOperator, String serviceType) {
        if (fromOperator.equals(operatorA) && toOperator.equals(operatorB)) {
            return getRateAtoB(serviceType);
        } else if (fromOperator.equals(operatorB) && toOperator.equals(operatorA)) {
            return getRateBtoA(serviceType);
        }
        return 0.50; // Domyślna stawka
    }

    // Sprawdź czy to roaming
    public boolean isRoaming() {
        return type == AgreementType.ROAMING || type == AgreementType.FULL;
    }

    // Sprawdź czy to połączenia
    public boolean allowsCalls() {
        return type == AgreementType.CALLS || type == AgreementType.FULL;
    }

    // Sprawdź czy to SMS
    public boolean allowsSMS() {
        return type == AgreementType.SMS || type == AgreementType.FULL;
    }

    public String getOtherOperator(String operatorId) {
        return operatorA.equals(operatorId) ? operatorB : operatorA;
    }
}
