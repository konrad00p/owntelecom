package pl.owntelecom.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BillingManager {

    private final OwnTelecom plugin;
    private final Map<String, Subscription> subscriptions; // ID pakietu -> Pakiet
    private final Map<UUID, List<ActiveSubscription>> playerSubscriptions; // Gracz -> Aktywne subskrypcje
    private final File subscriptionsFile;
    private final File activeSubsFile;

    public BillingManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.subscriptions = new ConcurrentHashMap<>();
        this.playerSubscriptions = new ConcurrentHashMap<>();
        this.subscriptionsFile = new File(plugin.getDataFolder(), "subscriptions.yml");
        this.activeSubsFile = new File(plugin.getDataFolder(), "active_subscriptions.yml");
        
        loadSubscriptions();
        loadActiveSubscriptions();
    }

    // ==================== ZARZĄDZANIE PAKIETAMI ====================

    // Operator tworzy nowy pakiet
    public boolean createSubscription(Player player, String operatorId, String name, String typeStr, 
                                      double price, int days, int minutes, int sms, int megabytes) {
        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono operatora!");
            return false;
        }

        if (!op.isOwner(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie jesteś właścicielem tego operatora!");
            return false;
        }

        Subscription.SubscriptionType type;
        try {
            type = Subscription.SubscriptionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowy typ! Dostępne: DOMESTIC, ROAMING");
            return false;
        }

        String subId = operatorId + "_" + name.toLowerCase().replace(" ", "_");
        Subscription sub = new Subscription(subId, operatorId, name, type);
        sub.setPrice(price);
        sub.setDurationDays(days);
        sub.setMinutes(minutes);
        sub.setSms(sms);
        sub.setMegabytes(megabytes);
        sub.setDescription("Pakiet " + type.toString().toLowerCase());

        subscriptions.put(subId, sub);
        saveSubscriptions();

        player.sendMessage(ChatColor.GREEN + "✅ Utworzono pakiet: " + name);
        player.sendMessage(ChatColor.GRAY + "ID: " + subId + " | Cena: $" + price);
        return true;
    }

    // Pobierz pakiety operatora
    public List<Subscription> getOperatorSubscriptions(String operatorId) {
        return subscriptions.values().stream()
            .filter(s -> s.getOperatorId().equals(operatorId) && s.isActive())
            .collect(Collectors.toList());
    }

    // Pobierz wszystkie dostępne pakiety
    public List<Subscription> getAvailableSubscriptions(UUID playerId) {
        List<Subscription> available = new ArrayList<>();
        
        for (Subscription sub : subscriptions.values()) {
            if (!sub.isActive()) continue;
            
            // Sprawdź czy gracz jest w zasięgu operatora
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && plugin.getStationManager().isPlayerInRange(player, sub.getOperatorId())) {
                available.add(sub);
            }
        }
        
        return available;
    }

    // ==================== KUPOWANIE I AKTYWACJA ====================

    // Kup pakiet
    public boolean purchaseSubscription(Player player, String subscriptionId) {
        Subscription sub = subscriptions.get(subscriptionId);
        if (sub == null || !sub.isActive()) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono pakietu!");
            return false;
        }

        // Sprawdź czy gracz już ma ten pakiet
        List<ActiveSubscription> playerSubs = playerSubscriptions.getOrDefault(
            player.getUniqueId(), new ArrayList<>());
        
        for (ActiveSubscription active : playerSubs) {
            if (active.getSubscriptionId().equals(subscriptionId) && 
                active.isActive() && !active.isExpired() && !active.isDepleted()) {
                player.sendMessage(ChatColor.RED + "Już posiadasz ten pakiet!");
                return false;
            }
        }

        // Sprawdź pieniądze
        double price = sub.getPrice();
        if (!plugin.getEconomy().has(player, price)) {
            player.sendMessage(ChatColor.RED + "Nie masz wystarczających środków! Potrzebujesz: $" + 
                String.format("%.2f", price));
            return false;
        }

        // Pobierz pieniądze
        plugin.getEconomy().withdrawPlayer(player, price);

        // Przekaż prowizję operatorowi (opcjonalnie)
        Operator op = plugin.getOperatorManager().getOperator(sub.getOperatorId());
        if (op != null) {
            double operatorShare = price * 0.9; // 90% dla operatora
            org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(op.getOwner());
            plugin.getEconomy().depositPlayer(owner, operatorShare);
        }

        // Aktywuj pakiet
        ActiveSubscription activeSub = new ActiveSubscription(player.getUniqueId(), sub);
        playerSubscriptions.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(activeSub);
        
        saveActiveSubscriptions();

        player.sendMessage(ChatColor.GREEN + "✅ Kupiono pakiet: " + sub.getName());
        player.sendMessage(ChatColor.GRAY + "Cena: $" + String.format("%.2f", price));
        player.sendMessage(ChatColor.YELLOW + "Użyj /telefon pakiet aby sprawdzić status");
        
        return true;
    }

    // Sprawdź czy gracz ma aktywny pakiet
    public ActiveSubscription getActiveSubscription(UUID playerId, String operatorId) {
        List<ActiveSubscription> subs = playerSubscriptions.get(playerId);
        if (subs == null) return null;

        for (ActiveSubscription sub : subs) {
            if (sub.isActive() && !sub.isExpired() && !sub.isDepleted() && 
                sub.getOperatorId().equals(operatorId)) {
                return sub;
            }
        }
        return null;
    }

    // Pobierz wszystkie aktywne pakiety gracza
    public List<ActiveSubscription> getPlayerActiveSubscriptions(UUID playerId) {
        List<ActiveSubscription> subs = playerSubscriptions.get(playerId);
        if (subs == null) return new ArrayList<>();
        
        return subs.stream()
            .filter(s -> s.isActive() && !s.isExpired() && !s.isDepleted())
            .collect(Collectors.toList());
    }

    // ==================== ROZLICZANIE USŁUG ====================

    // Rozlicz minutę połączenia
    public boolean chargeForMinute(Player player, String operatorId) {
        return chargeForService(player, operatorId, "minuta", 1);
    }

    // Rozlicz SMS
    public boolean chargeForSms(Player player, String operatorId) {
        return chargeForService(player, operatorId, "sms", 1);
    }

    // Rozlicz dane (MB)
    public boolean chargeForData(Player player, String operatorId, int megabytes) {
        return chargeForService(player, operatorId, "mb", megabytes);
    }

    // Główna metoda rozliczeniowa
    private boolean chargeForService(Player player, String operatorId, String serviceType, int amount) {
        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) return false;

        // 1. Sprawdź czy gracz ma aktywny pakiet
        ActiveSubscription activeSub = getActiveSubscription(player.getUniqueId(), operatorId);
        
        if (activeSub != null) {
            // Spróbuj użyć pakietu
            boolean usedFromPackage = false;
            
            switch (serviceType) {
                case "minuta":
                    usedFromPackage = activeSub.useMinutes(amount);
                    break;
                case "sms":
                    usedFromPackage = activeSub.useSms(amount);
                    break;
                case "mb":
                    usedFromPackage = activeSub.useMegabytes(amount);
                    break;
            }
            
            if (usedFromPackage) {
                // Użyto z pakietu - zapisz stan
                saveActiveSubscriptions();
                return true;
            }
            // Jeśli pakiet nie wystarczył - przejdź do prepaid
        }

        // 2. Prepaid - pobierz z konta bankowego
        double rate = op.getRate(serviceType);
        double cost = rate * amount;

        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage(ChatColor.RED + "❌ Niewystarczające środki! Potrzebujesz: $" + 
                String.format("%.2f", cost));
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        
        // Przekaż część operatorowi
        org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(op.getOwner());
        plugin.getEconomy().depositPlayer(owner, cost * 0.8); // 80% dla operatora
        
        return true;
    }

    // Sprawdź czy gracz może sobie pozwolić na usługę
    public boolean canAffordService(Player player, String operatorId, String serviceType, int amount) {
        // Sprawdź pakiet
        ActiveSubscription activeSub = getActiveSubscription(player.getUniqueId(), operatorId);
        if (activeSub != null) {
            switch (serviceType) {
                case "minuta":
                    if (activeSub.getRemainingMinutes() == -1 || activeSub.getRemainingMinutes() >= amount) 
                        return true;
                    break;
                case "sms":
                    if (activeSub.getRemainingSms() == -1 || activeSub.getRemainingSms() >= amount) 
                        return true;
                    break;
                case "mb":
                    if (activeSub.getRemainingMegabytes() == -1 || activeSub.getRemainingMegabytes() >= amount) 
                        return true;
                    break;
            }
        }

        // Sprawdź środki na koncie
        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) return false;
        
        double rate = op.getRate(serviceType);
        double cost = rate * amount;
        
        return plugin.getEconomy().has(player, cost);
    }

    // Pobierz koszt usługi (uwzględnia pakiety)
    public double getServiceCost(Player player, String operatorId, String serviceType, int amount) {
        ActiveSubscription activeSub = getActiveSubscription(player.getUniqueId(), operatorId);
        if (activeSub != null) {
            switch (serviceType) {
                case "minuta":
                    if (activeSub.getRemainingMinutes() == -1 || activeSub.getRemainingMinutes() >= amount) 
                        return 0.0; // Z pakietu
                    break;
                case "sms":
                    if (activeSub.getRemainingSms() == -1 || activeSub.getRemainingSms() >= amount) 
                        return 0.0;
                    break;
                case "mb":
                    if (activeSub.getRemainingMegabytes() == -1 || activeSub.getRemainingMegabytes() >= amount) 
                        return 0.0;
                    break;
            }
        }

        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) return Double.MAX_VALUE;
        
        return op.getRate(serviceType) * amount;
    }

    // ==================== INFORMACJE O KONCIE ====================

    // Pokaż stan konta telekomunikacyjnego
    public void showAccountInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ 📱 Twoje konto telekomunikacyjne ══════");
        
        double balance = plugin.getEconomy().getBalance(player);
        player.sendMessage(ChatColor.YELLOW + "Stan konta bankowego: " + ChatColor.GREEN + "$" + 
            String.format("%.2f", balance));
        
        List<ActiveSubscription> activeSubs = getPlayerActiveSubscriptions(player.getUniqueId());
        
        if (activeSubs.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "📦 Brak aktywnych pakietów.");
            player.sendMessage(ChatColor.GRAY + "Jesteś na stawce PREPAID (na kartę).");
        } else {
            player.sendMessage(ChatColor.GOLD + "📦 Aktywne pakiety:");
            for (ActiveSubscription sub : activeSubs) {
                Subscription original = subscriptions.get(sub.getSubscriptionId());
                String name = original != null ? original.getName() : "Nieznany";
                player.sendMessage(ChatColor.YELLOW + "  • " + name + ": " + sub.getStatusText());
            }
        }
        
        player.sendMessage(ChatColor.GRAY + "Użyj /telefon pakiety aby zobaczyć dostępne oferty.");
    }

    // Pokaż dostępne pakiety
    public void showAvailablePackages(Player player) {
        List<Subscription> available = getAvailableSubscriptions(player.getUniqueId());
        
        player.sendMessage(ChatColor.GOLD + "══════ 📦 Dostępne pakiety ══════");
        
        if (available.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Brak dostępnych pakietów w Twojej okolicy.");
            player.sendMessage(ChatColor.GRAY + "Sprawdź zasięg operatorów!");
            return;
        }

        // Grupuj według operatora
        Map<String, List<Subscription>> byOperator = new HashMap<>();
        for (Subscription sub : available) {
            byOperator.computeIfAbsent(sub.getOperatorId(), k -> new ArrayList<>()).add(sub);
        }

        for (Map.Entry<String, List<Subscription>> entry : byOperator.entrySet()) {
            Operator op = plugin.getOperatorManager().getOperator(entry.getKey());
            String opName = op != null ? op.getDisplayName() : entry.getKey();
            
            player.sendMessage(ChatColor.YELLOW + "▸ " + opName);
            
            for (Subscription sub : entry.getValue()) {
                String typeIcon = sub.getType() == Subscription.SubscriptionType.ROAMING ? "🌍" : "📱";
                player.sendMessage(typeIcon + " " + ChatColor.GREEN + sub.getName() + 
                    ChatColor.WHITE + " - $" + String.format("%.2f", sub.getPrice()));
                player.sendMessage(ChatColor.GRAY + "  📞 " + 
                    (sub.isUnlimitedMinutes() ? "∞" : sub.getMinutes()) + "min | " +
                    "💬 " + (sub.isUnlimitedSms() ? "∞" : sub.getSms()) + "sms | " +
                    "🌐 " + (sub.isUnlimitedData() ? "∞" : sub.getMegabytes()) + "MB");
                player.sendMessage(ChatColor.GRAY + "  ⏰ " + 
                    (sub.getDurationDays() > 0 ? sub.getDurationDays() + " dni" : "Bezterminowy"));
                player.sendMessage(ChatColor.GRAY + "  ID: " + sub.getId() + 
                    " | /telefon kup " + sub.getId());
            }
        }
    }

    // ==================== CZYSZCZENIE WYGASŁYCH ====================

    // Sprawdź i wyczyść wygasłe pakiety
    public void checkExpiredSubscriptions() {
        int cleaned = 0;
        
        for (List<ActiveSubscription> subs : playerSubscriptions.values()) {
            Iterator<ActiveSubscription> iterator = subs.iterator();
            while (iterator.hasNext()) {
                ActiveSubscription sub = iterator.next();
                if (sub.isExpired() || sub.isDepleted()) {
                    sub.deactivate();
                    cleaned++;
                }
            }
        }
        
        if (cleaned > 0) {
            saveActiveSubscriptions();
            plugin.getLogger().info("Wyczyszczono " + cleaned + " wygasłych pakietów.");
        }
    }

    // ==================== GETTERY ====================

    public Subscription getSubscription(String id) {
        return subscriptions.get(id);
    }

    public Collection<Subscription> getAllSubscriptions() {
        return subscriptions.values();
    }

    // ==================== ZAPIS/ODCZYT ====================

    public void saveSubscriptions() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Subscription sub : subscriptions.values()) {
            String path = "subscriptions." + sub.getId();
            config.set(path + ".operatorId", sub.getOperatorId());
            config.set(path + ".name", sub.getName());
            config.set(path + ".description", sub.getDescription());
            config.set(path + ".type", sub.getType().name());
            config.set(path + ".price", sub.getPrice());
            config.set(path + ".durationDays", sub.getDurationDays());
            config.set(path + ".active", sub.isActive());
            config.set(path + ".minutes", sub.getMinutes());
            config.set(path + ".sms", sub.getSms());
            config.set(path + ".megabytes", sub.getMegabytes());
            config.set(path + ".zoneId", sub.getZoneId());
        }
        
        try { config.save(subscriptionsFile); } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać subscriptions.yml!");
        }
    }

    public void saveActiveSubscriptions() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, List<ActiveSubscription>> entry : playerSubscriptions.entrySet()) {
            String uuidStr = entry.getKey().toString();
            List<ActiveSubscription> subs = entry.getValue();
            
            for (int i = 0; i < subs.size(); i++) {
                ActiveSubscription sub = subs.get(i);
                String path = "active." + uuidStr + "." + i;
                config.set(path + ".id", sub.getId());
                config.set(path + ".playerId", sub.getPlayerId().toString());
                config.set(path + ".subscriptionId", sub.getSubscriptionId());
                config.set(path + ".operatorId", sub.getOperatorId());
                config.set(path + ".purchaseDate", sub.getPurchaseDate());
                config.set(path + ".expiryDate", sub.getExpiryDate());
                config.set(path + ".active", sub.isActive());
                config.set(path + ".remainingMinutes", sub.getRemainingMinutes());
                config.set(path + ".remainingSms", sub.getRemainingSms());
                config.set(path + ".remainingMegabytes", sub.getRemainingMegabytes());
            }
        }
        
        try { config.save(activeSubsFile); } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać active_subscriptions.yml!");
        }
    }

    public void saveAll() {
        saveSubscriptions();
        saveActiveSubscriptions();
    }

    private void loadSubscriptions() {
        if (!subscriptionsFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(subscriptionsFile);
        ConfigurationSection subsSection = config.getConfigurationSection("subscriptions");
        if (subsSection == null) return;

        for (String id : subsSection.getKeys(false)) {
            String path = "subscriptions." + id;
            
            String operatorId = config.getString(path + ".operatorId");
            String name = config.getString(path + ".name", id);
            Subscription.SubscriptionType type = Subscription.SubscriptionType.valueOf(
                config.getString(path + ".type", "DOMESTIC"));
            
            Subscription sub = new Subscription(id, operatorId, name, type);
            sub.setDescription(config.getString(path + ".description", ""));
            sub.setPrice(config.getDouble(path + ".price", 0.0));
            sub.setDurationDays(config.getInt(path + ".durationDays", 30));
            sub.setActive(config.getBoolean(path + ".active", true));
            sub.setMinutes(config.getInt(path + ".minutes", 0));
            sub.setSms(config.getInt(path + ".sms", 0));
            sub.setMegabytes(config.getInt(path + ".megabytes", 0));
            sub.setZoneId(config.getString(path + ".zoneId", "0"));
            
            subscriptions.put(id, sub);
        }
    }

    private void loadActiveSubscriptions() {
        if (!activeSubsFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(activeSubsFile);
        ConfigurationSection activeSection = config.getConfigurationSection("active");
        if (activeSection == null) return;

        for (String uuidStr : activeSection.getKeys(false)) {
            UUID playerId = UUID.fromString(uuidStr);
            ConfigurationSection playerSection = activeSection.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;
            
            List<ActiveSubscription> subs = new ArrayList<>();
            
            for (String indexStr : playerSection.getKeys(false)) {
                String path = "active." + uuidStr + "." + indexStr;
                
                String subId = config.getString(path + ".subscriptionId");
                Subscription original = subscriptions.get(subId);
                if (original == null) continue;
                
                ActiveSubscription sub = new ActiveSubscription(playerId, original);
                // Nadpisz danymi z pliku
                // (uproszczone - w pełnej wersji trzeba by odczytać wszystkie pola)
                
                subs.add(sub);
            }
            
            if (!subs.isEmpty()) {
                playerSubscriptions.put(playerId, subs);
            }
        }
    }
}
