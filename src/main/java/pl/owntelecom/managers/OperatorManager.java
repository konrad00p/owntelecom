package pl.owntelecom.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Operator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OperatorManager {

    private final OwnTelecom plugin;
    private final Map<String, Operator> operators; // ID -> Operator
    private final Map<UUID, Long> lastCreated; // Gracz -> czas ostatniego utworzenia
    private final File operatorsFile;

    public OperatorManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.operators = new ConcurrentHashMap<>();
        this.lastCreated = new ConcurrentHashMap<>();
        this.operatorsFile = new File(plugin.getDataFolder(), "operators.yml");
        
        loadOperators();
    }

    // Tworzenie nowego operatora
    public boolean createOperator(Player owner, String id, String displayName) {
        // Sprawdź czy ID jest unikalne
        if (operators.containsKey(id.toLowerCase())) {
            owner.sendMessage("§cOperator o ID '" + id + "' już istnieje!");
            return false;
        }

        // Sprawdź limit operatorów na gracza
        int maxOperators = plugin.getConfigManager().getMaxOperatorsPerPlayer();
        long playerOperatorCount = operators.values().stream()
                .filter(op -> op.getOwner().equals(owner.getUniqueId()))
                .count();

        if (playerOperatorCount >= maxOperators && !owner.hasPermission("owntelecom.admin")) {
            owner.sendMessage("§cOsiągnąłeś maksymalną liczbę operatorów (" + maxOperators + ")!");
            return false;
        }

        // Sprawdź cooldown
        if (!owner.hasPermission("owntelecom.admin")) {
            Long lastTime = lastCreated.get(owner.getUniqueId());
            if (lastTime != null) {
                long cooldownMs = plugin.getConfigManager().getOperatorCooldownDays() * 24L * 60 * 60 * 1000;
                long timeLeft = (lastTime + cooldownMs) - System.currentTimeMillis();
                
                if (timeLeft > 0) {
                    long daysLeft = timeLeft / (24 * 60 * 60 * 1000);
                    long hoursLeft = (timeLeft % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
                    owner.sendMessage("§cMusisz poczekać jeszcze " + daysLeft + "d " + hoursLeft + "h przed utworzeniem nowego operatora!");
                    return false;
                }
            }
        }

        // Utwórz operatora
        Operator operator = new Operator(id, displayName, owner.getUniqueId());
        operators.put(id.toLowerCase(), operator);
        lastCreated.put(owner.getUniqueId(), System.currentTimeMillis());
        
        saveOperators();
        
        owner.sendMessage("§aUtworzono operatora §e" + displayName + " §a(ID: §e" + id + "§a)!");
        return true;
    }

    // Usuwanie operatora
    public boolean deleteOperator(Player player, String operatorId) {
        Operator operator = operators.get(operatorId.toLowerCase());
        
        if (operator == null) {
            player.sendMessage("§cNie znaleziono operatora o ID: " + operatorId);
            return false;
        }

        // Sprawdź uprawnienia (właściciel lub admin)
        if (!operator.isOwner(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage("§cNie jesteś właścicielem tego operatora!");
            return false;
        }

        String displayName = operator.getDisplayName();
        operators.remove(operatorId.toLowerCase());
        saveOperators();
        
        player.sendMessage("§aUsunięto operatora §e" + displayName + " §a(ID: §e" + operatorId + "§a)!");
        return true;
    }

    // Przekazywanie operatora (darmowe)
    public boolean transferOperator(Player from, Player to, String operatorId) {
        Operator operator = operators.get(operatorId.toLowerCase());
        
        if (operator == null) {
            from.sendMessage("§cNie znaleziono operatora o ID: " + operatorId);
            return false;
        }

        if (!operator.isOwner(from.getUniqueId()) && !from.hasPermission("owntelecom.admin")) {
            from.sendMessage("§cNie jesteś właścicielem tego operatora!");
            return false;
        }

        operator.setOwner(to.getUniqueId());
        saveOperators();
        
        from.sendMessage("§aPrzekazałeś operatora §e" + operator.getDisplayName() + " §ado gracza §e" + to.getName());
        to.sendMessage("§aOtrzymałeś operatora §e" + operator.getDisplayName() + " §aod gracza §e" + from.getName());
        return true;
    }

    // Sprzedaż operatora
    public boolean sellOperator(Player seller, Player buyer, String operatorId, double price) {
        Operator operator = operators.get(operatorId.toLowerCase());
        
        if (operator == null) {
            seller.sendMessage("§cNie znaleziono operatora o ID: " + operatorId);
            return false;
        }

        if (!operator.isOwner(seller.getUniqueId())) {
            seller.sendMessage("§cNie jesteś właścicielem tego operatora!");
            return false;
        }

        // Sprawdź czy kupujący ma pieniądze
        if (!plugin.getEconomy().has(buyer, price)) {
            seller.sendMessage("§cGracz " + buyer.getName() + " nie ma wystarczających środków!");
            return false;
        }

        // Przeprowadź transakcję
        plugin.getEconomy().withdrawPlayer(buyer, price);
        plugin.getEconomy().depositPlayer(seller, price);
        
        operator.setOwner(buyer.getUniqueId());
        saveOperators();
        
        seller.sendMessage("§aSprzedałeś operatora §e" + operator.getDisplayName() + " §aza §e" + price + " §ado gracza §e" + buyer.getName());
        buyer.sendMessage("§aKupiłeś operatora §e" + operator.getDisplayName() + " §aod gracza §e" + seller.getName() + " §aza §e" + price);
        return true;
    }

    // Auto-roaming dla własnych operatorów
    public void setupAutoRoaming(UUID ownerId) {
        List<Operator> playerOperators = getOperatorsByOwner(ownerId);
        
        if (playerOperators.size() < 2) return;
        
        // Ustaw umowy roamingowe między wszystkimi operatorami gracza
        for (int i = 0; i < playerOperators.size(); i++) {
            for (int j = i + 1; j < playerOperators.size(); j++) {
                Operator op1 = playerOperators.get(i);
                Operator op2 = playerOperators.get(j);
                
                op1.addAgreement(op2.getId(), "roaming");
                op2.addAgreement(op1.getId(), "roaming");
            }
        }
        
        saveOperators();
    }

    // Pobieranie operatorów
    public Operator getOperator(String id) {
        return operators.get(id.toLowerCase());
    }

    public List<Operator> getOperatorsByOwner(UUID ownerId) {
        List<Operator> result = new ArrayList<>();
        for (Operator op : operators.values()) {
            if (op.getOwner().equals(ownerId)) {
                result.add(op);
            }
        }
        return result;
    }

    public List<Operator> getAllOperators() {
        return new ArrayList<>(operators.values());
    }

    public List<Operator> getActiveOperators() {
        List<Operator> result = new ArrayList<>();
        for (Operator op : operators.values()) {
            if (op.isActive()) {
                result.add(op);
            }
        }
        return result;
    }

    // Zarządzanie stawkami
    public boolean setRate(Player player, String operatorId, String type, double rate) {
        Operator operator = operators.get(operatorId.toLowerCase());
        
        if (operator == null) {
            player.sendMessage("§cNie znaleziono operatora!");
            return false;
        }

        if (!operator.hasAccess(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage("§cNie masz uprawnień do zarządzania tym operatorem!");
            return false;
        }

        if (rate < 0) {
            player.sendMessage("§cStawka nie może być ujemna!");
            return false;
        }

        operator.setRate(type, rate);
        saveOperators();
        
        player.sendMessage("§aUstawiono stawkę §e" + type + " §ana §e" + rate + " §adla operatora §e" + operator.getDisplayName());
        return true;
    }

    // Zarządzanie pracownikami
    public boolean addEmployee(Player owner, String operatorId, Player employee) {
        Operator operator = operators.get(operatorId.toLowerCase());
        
        if (operator == null) return false;
        
        if (!operator.isOwner(owner.getUniqueId()) && !owner.hasPermission("owntelecom.admin")) {
            owner.sendMessage("§cNie jesteś właścicielem tego operatora!");
            return false;
        }

        operator.addEmployee(employee.getUniqueId());
        saveOperators();
        
        owner.sendMessage("§aDodano pracownika §e" + employee.getName() + " §ado operatora §e" + operator.getDisplayName());
        employee.sendMessage("§aZostałeś dodany jako pracownik operatora §e" + operator.getDisplayName());
        return true;
    }

    // Admin: zmiana właściciela
    public boolean adminChangeOwner(Player admin, String operatorId, Player newOwner) {
        if (!admin.hasPermission("owntelecom.admin")) {
            admin.sendMessage("§cBrak uprawnień!");
            return false;
        }

        Operator operator = operators.get(operatorId.toLowerCase());
        if (operator == null) {
            admin.sendMessage("§cNie znaleziono operatora!");
            return false;
        }

        operator.setOwner(newOwner.getUniqueId());
        saveOperators();
        
        admin.sendMessage("§aZmieniono właściciela operatora §e" + operator.getDisplayName() + " §ana §e" + newOwner.getName());
        return true;
    }

    // Sprawdzanie czy gracz jest w zasięgu swojego operatora
    public boolean isPlayerInOperatorRange(Player player) {
        // TODO: Implementacja w Module 3 (Stacje Bazowe)
        return true; // Tymczasowo zawsze true
    }

    // Zapis i odczyt
    public void saveOperators() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Operator op : operators.values()) {
            String path = "operators." + op.getId();
            config.set(path + ".displayName", op.getDisplayName());
            config.set(path + ".owner", op.getOwner().toString());
            config.set(path + ".zone", op.getZone());
            config.set(path + ".active", op.isActive());
            config.set(path + ".creationDate", op.getCreationDate());
            config.set(path + ".balance", op.getBalance());
            
            // Stawki
            for (Map.Entry<String, Double> entry : op.getRates().entrySet()) {
                config.set(path + ".rates." + entry.getKey(), entry.getValue());
            }
            
            // Stawki roamingowe
            for (Map.Entry<String, Double> entry : op.getRoamingRates().entrySet()) {
                config.set(path + ".roamingRates." + entry.getKey(), entry.getValue());
            }
            
            // Pracownicy
            List<String> employeesList = new ArrayList<>();
            for (UUID emp : op.getEmployees()) {
                employeesList.add(emp.toString());
            }
            config.set(path + ".employees", employeesList);
            
            // Umowy
            for (Map.Entry<String, String> entry : op.getAgreements().entrySet()) {
                config.set(path + ".agreements." + entry.getKey(), entry.getValue());
            }
        }
        
        // Cooldowny
        for (Map.Entry<UUID, Long> entry : lastCreated.entrySet()) {
            config.set("cooldowns." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            config.save(operatorsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać operators.yml! " + e.getMessage());
        }
    }

    public void saveAll() {
        saveOperators();
    }

    private void loadOperators() {
        if (!operatorsFile.exists()) {
            try {
                operatorsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć operators.yml! " + e.getMessage());
            }
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(operatorsFile);
        ConfigurationSection operatorsSection = config.getConfigurationSection("operators");
        
        if (operatorsSection == null) return;

        for (String id : operatorsSection.getKeys(false)) {
            String path = "operators." + id;
            
            String displayName = config.getString(path + ".displayName", id);
            UUID owner = UUID.fromString(config.getString(path + ".owner"));
            
            Operator operator = new Operator(id, displayName, owner);
            operator.setZone(config.getString(path + ".zone", "0"));
            operator.setActive(config.getBoolean(path + ".active", true));
            operator.setBalance(config.getDouble(path + ".balance", 0.0));
            
            // Stawki
            ConfigurationSection ratesSection = config.getConfigurationSection(path + ".rates");
            if (ratesSection != null) {
                for (String rateKey : ratesSection.getKeys(false)) {
                    operator.setRate(rateKey, ratesSection.getDouble(rateKey));
                }
            }
            
            // Stawki roamingowe
            ConfigurationSection roamingSection = config.getConfigurationSection(path + ".roamingRates");
            if (roamingSection != null) {
                for (String rateKey : roamingSection.getKeys(false)) {
                    operator.setRoamingRate(rateKey, roamingSection.getDouble(rateKey));
                }
            }
            
            // Pracownicy
            List<String> employeesList = config.getStringList(path + ".employees");
            for (String empStr : employeesList) {
                try {
                    operator.addEmployee(UUID.fromString(empStr));
                } catch (IllegalArgumentException ignored) {}
            }
            
            // Umowy
            ConfigurationSection agreementsSection = config.getConfigurationSection(path + ".agreements");
            if (agreementsSection != null) {
                for (String agreementKey : agreementsSection.getKeys(false)) {
                    operator.addAgreement(agreementKey, agreementsSection.getString(agreementKey));
                }
            }
            
            operators.put(id, operator);
        }
        
        // Cooldowny
        ConfigurationSection cooldownsSection = config.getConfigurationSection("cooldowns");
        if (cooldownsSection != null) {
            for (String uuidStr : cooldownsSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    lastCreated.put(uuid, cooldownsSection.getLong(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        plugin.getLogger().info("Załadowano " + operators.size() + " operatorów.");
    }
}
