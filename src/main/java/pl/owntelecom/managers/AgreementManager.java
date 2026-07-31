package pl.owntelecom.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Agreement;
import pl.owntelecom.models.Operator;
import pl.owntelecom.models.RoamingZone;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AgreementManager {

    private final OwnTelecom plugin;
    private final Map<String, Agreement> agreements;
    private final Map<String, RoamingZone> zones;
    private final File agreementsFile;
    private final File zonesFile;

    public AgreementManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.agreements = new ConcurrentHashMap<>();
        this.zones = new ConcurrentHashMap<>();
        this.agreementsFile = new File(plugin.getDataFolder(), "agreements.yml");
        this.zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        
        loadZones();
        loadAgreements();
    }

    // ==================== ZARZĄDZANIE STREFAMI ====================

    private void loadZones() {
        // Strefa 0 zawsze istnieje
        if (!zones.containsKey("0")) {
            RoamingZone homeZone = new RoamingZone("0", "Strefa 0 - Home", true);
            zones.put("0", homeZone);
        }

        if (!zonesFile.exists()) {
            // Utwórz domyślne strefy
            zones.put("1", new RoamingZone("1", "Strefa 1", false));
            zones.put("2", new RoamingZone("2", "Strefa 2", false));
            zones.put("3", new RoamingZone("3", "Strefa 3 - Międzynarodowa", false));
            saveZones();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(zonesFile);
        ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection == null) return;

        for (String id : zonesSection.getKeys(false)) {
            String path = "zones." + id;
            String displayName = config.getString(path + ".displayName", "Strefa " + id);
            boolean isHome = config.getBoolean(path + ".isHome", id.equals("0"));
            
            RoamingZone zone = new RoamingZone(id, displayName, isHome);
            zone.setActive(config.getBoolean(path + ".active", true));
            
            // Stawki
            ConfigurationSection ratesSection = config.getConfigurationSection(path + ".rates");
            if (ratesSection != null) {
                for (String rateKey : ratesSection.getKeys(false)) {
                    zone.setRate(rateKey, ratesSection.getDouble(rateKey));
                }
            }
            
            // Operatorzy w strefie
            List<String> operators = config.getStringList(path + ".operators");
            for (String opId : operators) {
                zone.addOperator(opId);
            }
            
            zones.put(id, zone);
        }
    }

    public void saveZones() {
        FileConfiguration config = new YamlConfiguration();
        for (RoamingZone zone : zones.values()) {
            String path = "zones." + zone.getId();
            config.set(path + ".displayName", zone.getDisplayName());
            config.set(path + ".isHome", zone.isHomeZone());
            config.set(path + ".active", zone.isActive());
            config.set(path + ".operators", zone.getOperatorsInZone());
            
            for (Map.Entry<String, Double> entry : zone.getRates().entrySet()) {
                config.set(path + ".rates." + entry.getKey(), entry.getValue());
            }
        }
        try { config.save(zonesFile); } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać zones.yml!");
        }
    }

    public RoamingZone getZone(String zoneId) {
        return zones.getOrDefault(zoneId, zones.get("0"));
    }

    public RoamingZone getOperatorZone(String operatorId) {
        for (RoamingZone zone : zones.values()) {
            if (zone.hasOperator(operatorId)) {
                return zone;
            }
        }
        return zones.get("0");
    }

    public Collection<RoamingZone> getAllZones() {
        return zones.values();
    }

    public void addOperatorToZone(String operatorId, String zoneId) {
        RoamingZone zone = zones.get(zoneId);
        if (zone != null) {
            zone.addOperator(operatorId);
            
            // Aktualizuj operatora
            Operator op = plugin.getOperatorManager().getOperator(operatorId);
            if (op != null) {
                op.setZone(zoneId);
            }
            
            saveZones();
        }
    }

    // ==================== ZARZĄDZANIE UMOWAMI ====================

    // Zaproponuj umowę
    public boolean proposeAgreement(Player proposer, String targetOperatorId, String typeStr, String zoneId) {
        // Znajdź operatora gracza
        List<Operator> playerOps = plugin.getOperatorManager().getOperatorsByOwner(proposer.getUniqueId());
        if (playerOps.isEmpty()) {
            proposer.sendMessage(ChatColor.RED + "Nie posiadasz żadnego operatora!");
            return false;
        }

        Operator proposerOp = playerOps.get(0);
        Operator targetOp = plugin.getOperatorManager().getOperator(targetOperatorId);
        
        if (targetOp == null) {
            proposer.sendMessage(ChatColor.RED + "Nie znaleziono operatora: " + targetOperatorId);
            return false;
        }

        if (proposerOp.getId().equals(targetOperatorId)) {
            proposer.sendMessage(ChatColor.RED + "Nie możesz podpisać umowy z samym sobą!");
            return false;
        }

        // Sprawdź czy już nie ma aktywnej umowy
        for (Agreement existing : agreements.values()) {
            if (existing.isActive() && existing.involves(proposerOp.getId(), targetOperatorId)) {
                proposer.sendMessage(ChatColor.RED + "Już masz aktywną umowę z tym operatorem!");
                return false;
            }
        }

        Agreement.AgreementType type;
        try {
            type = Agreement.AgreementType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            proposer.sendMessage(ChatColor.RED + "Nieprawidłowy typ umowy! Dostępne: CALLS, SMS, ROAMING, FULL");
            return false;
        }

        // Koszt zawarcia umowy
        double agreementCost = 1000.0;
        if (!plugin.getEconomy().has(proposer, agreementCost)) {
            proposer.sendMessage(ChatColor.RED + "Potrzebujesz $" + agreementCost + " na zawarcie umowy!");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(proposer, agreementCost);

        Agreement agreement = new Agreement(proposerOp.getId(), targetOperatorId, type);
        agreement.setZoneId(zoneId != null ? zoneId : "0");
        agreements.put(agreement.getId(), agreement);
        
        saveAgreements();

        // Powiadomienia
        proposer.sendMessage(ChatColor.GREEN + "📝 Zaproponowano umowę " + type + " operatorowi " + 
                            targetOp.getDisplayName());
        proposer.sendMessage(ChatColor.GRAY + "ID umowy: " + agreement.getId());
        proposer.sendMessage(ChatColor.GRAY + "Koszt: $" + agreementCost);

        // Powiadom właściciela drugiego operatora
        Player targetOwner = Bukkit.getPlayer(targetOp.getOwner());
        if (targetOwner != null && targetOwner.isOnline()) {
            targetOwner.sendMessage(ChatColor.GOLD + "📝 " + proposer.getName() + 
                " (Operator: " + proposerOp.getDisplayName() + ") proponuje umowę " + type + "!");
            targetOwner.sendMessage(ChatColor.YELLOW + "Użyj: /umowa akceptuj " + agreement.getId());
            targetOwner.sendMessage(ChatColor.YELLOW + "Lub: /umowa odrzuc " + agreement.getId());
            targetOwner.sendMessage(ChatColor.GRAY + "Strefa: " + getZone(agreement.getZoneId()).getDisplayName());
        }

        return true;
    }

    // Akceptuj umowę
    public boolean acceptAgreement(Player player, String agreementId) {
        Agreement agreement = agreements.get(agreementId);
        if (agreement == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono umowy!");
            return false;
        }

        if (agreement.getStatus() != Agreement.AgreementStatus.PENDING) {
            player.sendMessage(ChatColor.RED + "Ta umowa nie oczekuje na akceptację!");
            return false;
        }

        // Sprawdź czy gracz jest właścicielem operatora B
        Operator targetOp = plugin.getOperatorManager().getOperator(agreement.getOperatorB());
        if (targetOp == null || !targetOp.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie jesteś właścicielem operatora docelowego!");
            return false;
        }

        // Koszt akceptacji (opcjonalny)
        double acceptCost = 500.0;
        if (!plugin.getEconomy().has(player, acceptCost)) {
            player.sendMessage(ChatColor.RED + "Potrzebujesz $" + acceptCost + " na akceptację umowy!");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, acceptCost);
        agreement.setStatus(Agreement.AgreementStatus.ACTIVE);
        
        // Dodaj umowy do operatorów
        Operator opA = plugin.getOperatorManager().getOperator(agreement.getOperatorA());
        Operator opB = plugin.getOperatorManager().getOperator(agreement.getOperatorB());
        
        if (opA != null) opA.addAgreement(opB.getId(), agreement.getType().name().toLowerCase());
        if (opB != null) opB.addAgreement(opA.getId(), agreement.getType().name().toLowerCase());
        
        saveAgreements();
        plugin.getOperatorManager().saveOperators();

        player.sendMessage(ChatColor.GREEN + "✅ Umowa zaakceptowana!");
        
        // Powiadom drugą stronę
        Player otherOwner = Bukkit.getPlayer(opA.getOwner());
        if (otherOwner != null && otherOwner.isOnline()) {
            otherOwner.sendMessage(ChatColor.GREEN + "✅ " + player.getName() + " zaakceptował umowę!");
        }

        return true;
    }

    // Odrzuć umowę
    public boolean rejectAgreement(Player player, String agreementId) {
        Agreement agreement = agreements.get(agreementId);
        if (agreement == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono umowy!");
            return false;
        }

        agreement.setStatus(Agreement.AgreementStatus.REJECTED);
        saveAgreements();

        player.sendMessage(ChatColor.RED + "❌ Umowa odrzucona.");
        return true;
    }

    // Zerwij umowę
    public boolean terminateAgreement(Player player, String agreementId) {
        Agreement agreement = agreements.get(agreementId);
        if (agreement == null || !agreement.isActive()) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono aktywnej umowy!");
            return false;
        }

        // Sprawdź czy gracz jest właścicielem któregoś z operatorów
        Operator opA = plugin.getOperatorManager().getOperator(agreement.getOperatorA());
        Operator opB = plugin.getOperatorManager().getOperator(agreement.getOperatorB());
        
        boolean isOwnerA = opA != null && opA.isOwner(player.getUniqueId());
        boolean isOwnerB = opB != null && opB.isOwner(player.getUniqueId());
        
        if (!isOwnerA && !isOwnerB && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie jesteś stroną tej umowy!");
            return false;
        }

        agreement.setStatus(Agreement.AgreementStatus.TERMINATED);
        
        // Usuń umowy z operatorów
        if (opA != null) opA.removeAgreement(agreement.getOperatorB());
        if (opB != null) opB.removeAgreement(agreement.getOperatorA());
        
        saveAgreements();
        plugin.getOperatorManager().saveOperators();

        player.sendMessage(ChatColor.RED + "🔒 Umowa zerwana.");
        return true;
    }

    // Ustaw stawki w umowie
    public boolean setAgreementRates(Player player, String agreementId, String direction, String serviceType, double rate) {
        Agreement agreement = agreements.get(agreementId);
        if (agreement == null || !agreement.isActive()) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono aktywnej umowy!");
            return false;
        }

        if (rate < 0) {
            player.sendMessage(ChatColor.RED + "Stawka nie może być ujemna!");
            return false;
        }

        switch (direction.toLowerCase()) {
            case "atob":
            case "a->b":
                agreement.setRateAtoB(serviceType, rate);
                break;
            case "btoa":
            case "b->a":
                agreement.setRateBtoA(serviceType, rate);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Kierunek: AtoB lub BtoA");
                return false;
        }

        saveAgreements();
        player.sendMessage(ChatColor.GREEN + "✅ Ustawiono stawkę " + serviceType + " (" + direction + "): $" + rate);
        return true;
    }

    // ==================== SPRAWDZANIE ROAMINGU ====================

    // Sprawdź czy gracz jest w roamingu
    public boolean isPlayerRoaming(Player player, String operatorId) {
        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) return false;

        String homeZone = op.getZone();
        
        // Znajdź strefę w której jest gracz
        for (RoamingZone zone : zones.values()) {
            if (zone.hasOperator(operatorId) && !zone.getId().equals(homeZone)) {
                return true;
            }
        }
        
        return false;
    }

    // Pobierz strefę roamingu dla gracza
    public RoamingZone getPlayerRoamingZone(Player player, String operatorId) {
        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) return zones.get("0");

        String homeZone = op.getZone();
        
        for (RoamingZone zone : zones.values()) {
            if (zone.hasOperator(operatorId) && !zone.getId().equals(homeZone)) {
                return zone;
            }
        }
        
        return zones.get(homeZone);
    }

    // Sprawdź czy połączenie/SMS jest możliwe między operatorami
    public boolean canCommunicate(String operatorA, String operatorB, String serviceType) {
        if (operatorA.equals(operatorB)) return true; // Ten sam operator

        // Sprawdź czy mają umowę
        for (Agreement agreement : agreements.values()) {
            if (agreement.isActive() && agreement.involves(operatorA, operatorB)) {
                switch (serviceType) {
                    case "call":
                    case "minuta":
                        return agreement.allowsCalls();
                    case "sms":
                        return agreement.allowsSMS();
                    case "data":
                    case "mb":
                        return agreement.isRoaming();
                }
            }
        }
        
        return false;
    }

    // Pobierz stawkę za usługę między operatorami
    public double getServiceRate(String fromOperator, String toOperator, String serviceType) {
        if (fromOperator.equals(toOperator)) {
            return plugin.getOperatorManager().getOperator(fromOperator).getRate(serviceType);
        }

        for (Agreement agreement : agreements.values()) {
            if (agreement.isActive() && agreement.involves(fromOperator, toOperator)) {
                return agreement.getRate(fromOperator, toOperator, serviceType);
            }
        }
        
        return 1.0; // Domyślna wysoka stawka
    }

    // Sprawdź roaming i zwróć komunikat
    public String getRoamingNotification(Player player, String operatorId) {
        if (isPlayerRoaming(player, operatorId)) {
            RoamingZone zone = getPlayerRoamingZone(player, operatorId);
            return ChatColor.GOLD + "🌍 Połączono z siecią w roamingu: " + 
                   ChatColor.YELLOW + zone.getDisplayName() + 
                   ChatColor.GRAY + " | /stawki roaming " + operatorId;
        }
        return null;
    }

    // ==================== POMOCNICZE ====================

    public Agreement getAgreement(String id) {
        return agreements.get(id);
    }

    public List<Agreement> getOperatorAgreements(String operatorId) {
        return agreements.values().stream()
            .filter(a -> a.involvesOperator(operatorId))
            .collect(Collectors.toList());
    }

    public List<Agreement> getActiveAgreements() {
        return agreements.values().stream()
            .filter(Agreement::isActive)
            .collect(Collectors.toList());
    }

    public List<Agreement> getPendingAgreements(String operatorId) {
        return agreements.values().stream()
            .filter(a -> a.getStatus() == Agreement.AgreementStatus.PENDING && 
                        a.getOperatorB().equals(operatorId))
            .collect(Collectors.toList());
    }

    // ==================== ZAPIS/ODCZYT ====================

    public void saveAgreements() {
        FileConfiguration config = new YamlConfiguration();
        for (Agreement agreement : agreements.values()) {
            String path = "agreements." + agreement.getId();
            config.set(path + ".operatorA", agreement.getOperatorA());
            config.set(path + ".operatorB", agreement.getOperatorB());
            config.set(path + ".type", agreement.getType().name());
            config.set(path + ".status", agreement.getStatus().name());
            config.set(path + ".zoneId", agreement.getZoneId());
            config.set(path + ".creationDate", agreement.getCreationDate());
            config.set(path + ".expiryDate", agreement.getExpiryDate());
            config.set(path + ".autoRenew", agreement.isAutoRenew());
            
            for (Map.Entry<String, Double> entry : agreement.getRatesAtoB().entrySet()) {
                config.set(path + ".ratesAtoB." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Double> entry : agreement.getRatesBtoA().entrySet()) {
                config.set(path + ".ratesBtoA." + entry.getKey(), entry.getValue());
            }
        }
        try { config.save(agreementsFile); } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać agreements.yml!");
        }
    }

    public void saveAll() {
        saveAgreements();
        saveZones();
    }

    private void loadAgreements() {
        if (!agreementsFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(agreementsFile);
        ConfigurationSection agreementsSection = config.getConfigurationSection("agreements");
        if (agreementsSection == null) return;

        for (String id : agreementsSection.getKeys(false)) {
            String path = "agreements." + id;
            
            String opA = config.getString(path + ".operatorA");
            String opB = config.getString(path + ".operatorB");
            Agreement.AgreementType type = Agreement.AgreementType.valueOf(
                config.getString(path + ".type", "FULL"));
            
            Agreement agreement = new Agreement(opA, opB, type);
            agreement.setStatus(Agreement.AgreementStatus.valueOf(
                config.getString(path + ".status", "PENDING")));
            agreement.setZoneId(config.getString(path + ".zoneId", "0"));
            agreement.setExpiryDate(config.getLong(path + ".expiryDate", 0));
            agreement.setAutoRenew(config.getBoolean(path + ".autoRenew", false));
            
            // Stawki A->B
            ConfigurationSection ratesAB = config.getConfigurationSection(path + ".ratesAtoB");
            if (ratesAB != null) {
                for (String rateKey : ratesAB.getKeys(false)) {
                    agreement.setRateAtoB(rateKey, ratesAB.getDouble(rateKey));
                }
            }
            
            // Stawki B->A
            ConfigurationSection ratesBA = config.getConfigurationSection(path + ".ratesBtoA");
            if (ratesBA != null) {
                for (String rateKey : ratesBA.getKeys(false)) {
                    agreement.setRateBtoA(rateKey, ratesBA.getDouble(rateKey));
                }
            }
            
            agreements.put(id, agreement);
        }
    }
}
