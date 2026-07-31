package pl.owntelecom.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Operator;
import pl.owntelecom.models.Station;

import java.util.*;

public class NetworkManager {

    private final OwnTelecom plugin;
    // Który operator obecnie obsługuje gracza
    private final Map<UUID, String> connectedOperators;
    // Czy gracz zezwala na automatyczny roaming
    private final Map<UUID, Boolean> autoRoaming;

    public NetworkManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.connectedOperators = new HashMap<>();
        this.autoRoaming = new HashMap<>();
    }

    // ==================== POŁĄCZENIE Z SIECIĄ ====================

    // Pobierz operatora, do którego podłączony jest gracz
    public String getConnectedOperator(Player player) {
        String operatorId = connectedOperators.get(player.getUniqueId());
        
        // Jeśli gracz nie ma wybranego operatora, znajdź domyślny
        if (operatorId == null) {
            operatorId = findDefaultOperator(player);
            if (operatorId != null) {
                connectedOperators.put(player.getUniqueId(), operatorId);
            }
        }
        
        // Sprawdź czy nadal ma zasięg
        if (operatorId != null && !plugin.getStationManager().isPlayerInRange(player, operatorId)) {
            // Spróbuj automatycznego roamingu
            if (isAutoRoamingEnabled(player)) {
                String newOp = findBestRoamingOperator(player);
                if (newOp != null) {
                    connectToOperator(player, newOp);
                    return newOp;
                }
            }
            // Brak zasięgu
            return null;
        }
        
        return operatorId;
    }

    // Ręczne przełączenie do operatora
    public boolean switchOperator(Player player, String targetOperatorId) {
        Operator targetOp = plugin.getOperatorManager().getOperator(targetOperatorId);
        if (targetOp == null || !targetOp.isActive()) {
            player.sendMessage(ChatColor.RED + "Ten operator nie istnieje lub jest nieaktywny!");
            return false;
        }

        // Sprawdź zasięg
        if (!plugin.getStationManager().isPlayerInRange(player, targetOperatorId)) {
            player.sendMessage(ChatColor.RED + "Brak zasięgu operatora " + targetOp.getDisplayName() + "!");
            return false;
        }

        String currentOp = getConnectedOperator(player);
        
        // Jeśli to ten sam operator
        if (currentOp != null && currentOp.equals(targetOperatorId)) {
            player.sendMessage(ChatColor.YELLOW + "Już jesteś połączony z " + targetOp.getDisplayName());
            return false;
        }

        // Sprawdź umowę roamingową (jeśli zmieniamy operatora)
        if (currentOp != null && !currentOp.equals(targetOperatorId)) {
            Operator currentOperator = plugin.getOperatorManager().getOperator(currentOp);
            
            if (currentOperator != null && !currentOperator.hasAgreement(targetOperatorId)) {
                // Brak umowy - roaming awaryjny (drogo!)
                player.sendMessage(ChatColor.GOLD + "⚠ Brak umowy roamingowej z " + targetOp.getDisplayName());
                player.sendMessage(ChatColor.RED + "Roaming awaryjny: stawki x3!");
                player.sendMessage(ChatColor.YELLOW + "Użyj /przelacz " + targetOperatorId + " potwierdz aby kontynuować.");
                return false;
            }
        }

        return connectToOperator(player, targetOperatorId);
    }

    // Wymuś przełączenie (nawet bez umowy)
    public boolean forceSwitchOperator(Player player, String targetOperatorId) {
        Operator targetOp = plugin.getOperatorManager().getOperator(targetOperatorId);
        if (targetOp == null) return false;

        if (!plugin.getStationManager().isPlayerInRange(player, targetOperatorId)) {
            player.sendMessage(ChatColor.RED + "Brak zasięgu!");
            return false;
        }

        return connectToOperator(player, targetOperatorId);
    }

    // Połącz z operatorem
    private boolean connectToOperator(Player player, String operatorId) {
        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) return false;

        String oldOperator = connectedOperators.get(player.getUniqueId());
        connectedOperators.put(player.getUniqueId(), operatorId);

        // Sprawdź roaming
        boolean isRoaming = plugin.getAgreementManager().isPlayerRoaming(player, operatorId);
        var zone = plugin.getAgreementManager().getPlayerRoamingZone(player, operatorId);

        player.sendMessage(ChatColor.GREEN + "📡 Połączono z siecią: " + ChatColor.YELLOW + op.getDisplayName());
        
        if (isRoaming) {
            player.sendMessage(ChatColor.GOLD + "🌍 Roaming w strefie: " + zone.getDisplayName());
            player.sendMessage(ChatColor.GRAY + "Stawki roamingowe: minuta $" + 
                String.format("%.2f", op.getRoamingRate("minuta")) + 
                " | SMS $" + String.format("%.2f", op.getRoamingRate("sms")) + 
                " | MB $" + String.format("%.2f", op.getRoamingRate("mb")));
            player.sendMessage(ChatColor.YELLOW + "Użyj /zasieg aby sprawdzić szczegóły.");
        }

        return true;
    }

    // Znajdź domyślnego operatora
    private String findDefaultOperator(Player player) {
        // Szukamy najlepszego operatora w zasięgu (preferujemy własnego)
        List<Operator> playerOperators = plugin.getOperatorManager().getOperatorsByOwner(player.getUniqueId());
        
        // Najpierw sprawdź własnych operatorów
        for (Operator op : playerOperators) {
            if (op.isActive() && plugin.getStationManager().isPlayerInRange(player, op.getId())) {
                return op.getId();
            }
        }
        
        // Potem dowolny operator w zasięgu
        for (Operator op : plugin.getOperatorManager().getActiveOperators()) {
            if (plugin.getStationManager().isPlayerInRange(player, op.getId())) {
                return op.getId();
            }
        }
        
        return null;
    }

    // Znajdź najlepszy roaming
    private String findBestRoamingOperator(Player player) {
        String currentOp = connectedOperators.get(player.getUniqueId());
        Operator currentOperator = currentOp != null ? 
            plugin.getOperatorManager().getOperator(currentOp) : null;
        
        Station bestStation = null;
        String bestOperator = null;
        double bestQuality = -1;

        for (Operator op : plugin.getOperatorManager().getActiveOperators()) {
            if (op.getId().equals(currentOp)) continue;
            
            // Sprawdź umowę
            if (currentOperator != null && !currentOperator.hasAgreement(op.getId())) {
                continue; // Pomiń operatorów bez umowy przy auto-roamingu
            }
            
            Station station = plugin.getStationManager().findBestStation(player, op.getId());
            if (station != null) {
                double quality = station.getSignalQuality(player.getLocation());
                if (quality > bestQuality) {
                    bestQuality = quality;
                    bestStation = station;
                    bestOperator = op.getId();
                }
            }
        }

        return bestOperator;
    }

    // ==================== AUTO-ROAMING ====================

    public boolean isAutoRoamingEnabled(Player player) {
        return autoRoaming.getOrDefault(player.getUniqueId(), true);
    }

    public void setAutoRoaming(Player player, boolean enabled) {
        autoRoaming.put(player.getUniqueId(), enabled);
        
        if (enabled) {
            player.sendMessage(ChatColor.GREEN + "✅ Automatyczny roaming WŁĄCZONY");
        } else {
            player.sendMessage(ChatColor.RED + "⛔ Automatyczny roaming WYŁĄCZONY");
            player.sendMessage(ChatColor.GRAY + "Pozostaniesz przy obecnym operatorze nawet bez zasięgu.");
        }
    }

    // ==================== INFO O SIECI ====================

    // Pokaż szczegółowe info o obecnym operatorze
    public void showCurrentNetwork(Player player) {
        String operatorId = getConnectedOperator(player);
        
        if (operatorId == null) {
            player.sendMessage(ChatColor.RED + "📵 BRAK SYGNAŁU - Nie jesteś podłączony do żadnej sieci!");
            player.sendMessage(ChatColor.GRAY + "Użyj /zasiegall aby zobaczyć dostępne sieci.");
            return;
        }

        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        Station bestStation = plugin.getStationManager().findBestStation(player, operatorId);
        
        if (op == null || bestStation == null) {
            player.sendMessage(ChatColor.RED + "Błąd: Nie można pobrać informacji o sieci.");
            return;
        }

        double quality = bestStation.getSignalQuality(player.getLocation());
        double distance = bestStation.getLocation().distance(player.getLocation());
        boolean isRoaming = plugin.getAgreementManager().isPlayerRoaming(player, operatorId);
        
        player.sendMessage(ChatColor.GOLD + "══════ 📱 " + op.getDisplayName() + " ══════");
        
        // Pasek sygnału
        player.sendMessage(ChatColor.GRAY + "Sygnał: " + getSignalBar(quality) + " " + 
            getQualityText(quality) + String.format(" (%.0f%%)", quality * 100));
        
        // Technologia
        player.sendMessage(ChatColor.GRAY + "Technologia: " + ChatColor.WHITE + bestStation.getTechnology() + 
            ChatColor.GRAY + " | Stacja Lvl." + bestStation.getLevel());
        
        // Odległość
        player.sendMessage(ChatColor.GRAY + "Odległość od stacji: " + ChatColor.WHITE + 
            String.format("%.1f bloków", distance));
        
        // Internet
        if (plugin.getStationManager().supportsInternet(bestStation.getTechnology())) {
            double speed = bestStation.getSpeedAtDistance(distance);
            String speedColor = speed > 10 ? ChatColor.GREEN.toString() : 
                               speed > 1 ? ChatColor.YELLOW.toString() : ChatColor.RED.toString();
            player.sendMessage(ChatColor.GRAY + "Internet: " + speedColor + 
                String.format("%.1f Mb/s", speed));
        } else {
            player.sendMessage(ChatColor.GRAY + "Internet: " + ChatColor.RED + "Niedostępny");
        }
        
        // Roaming
        if (isRoaming) {
            var zone = plugin.getAgreementManager().getPlayerRoamingZone(player, operatorId);
            player.sendMessage(ChatColor.GOLD + "🌍 ROAMING: " + zone.getDisplayName());
            
            // Stawki roamingowe
            player.sendMessage(ChatColor.GRAY + "Stawki:");
            player.sendMessage(ChatColor.GRAY + "  📞 Minuta: " + ChatColor.YELLOW + 
                "$" + String.format("%.2f", op.getRoamingRate("minuta")));
            player.sendMessage(ChatColor.GRAY + "  💬 SMS: " + ChatColor.YELLOW + 
                "$" + String.format("%.2f", op.getRoamingRate("sms")));
            player.sendMessage(ChatColor.GRAY + "  🌐 MB: " + ChatColor.YELLOW + 
                "$" + String.format("%.2f", op.getRoamingRate("mb")));
        } else {
            player.sendMessage(ChatColor.GREEN + "🏠 HOME - Stawki krajowe");
            
            // Stawki krajowe
            player.sendMessage(ChatColor.GRAY + "Stawki:");
            player.sendMessage(ChatColor.GRAY + "  📞 Minuta: " + ChatColor.WHITE + 
                "$" + String.format("%.2f", op.getRate("minuta")));
            player.sendMessage(ChatColor.GRAY + "  💬 SMS: " + ChatColor.WHITE + 
                "$" + String.format("%.2f", op.getRate("sms")));
            player.sendMessage(ChatColor.GRAY + "  🌐 MB: " + ChatColor.WHITE + 
                "$" + String.format("%.2f", op.getRate("mb")));
        }
        
        // Auto-roaming status
        boolean autoRoam = isAutoRoamingEnabled(player);
        player.sendMessage(ChatColor.GRAY + "Auto-roaming: " + 
            (autoRoam ? ChatColor.GREEN + "WŁ." : ChatColor.RED + "WYŁ."));
        
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
    }

    // Pokaż wszystkie dostępne sieci
    public void showAllNetworks(Player player) {
        String currentOp = getConnectedOperator(player);
        Operator currentOperator = currentOp != null ? 
            plugin.getOperatorManager().getOperator(currentOp) : null;
        
        player.sendMessage(ChatColor.GOLD + "══════ 📡 Dostępne sieci ══════");
        
        List<Operator> available = new ArrayList<>();
        List<Operator> roamingAvailable = new ArrayList<>();
        List<Operator> emergencyOnly = new ArrayList<>();
        
        for (Operator op : plugin.getOperatorManager().getActiveOperators()) {
            Station bestStation = plugin.getStationManager().findBestStation(player, op.getId());
            if (bestStation == null) continue; // POMIJAMY operatorów bez zasięgu!
            
            if (currentOperator != null && op.getId().equals(currentOperator.getId())) {
                // Obecny operator - na samej górze
                available.add(0, op);
            } else if (currentOperator != null && currentOperator.hasAgreement(op.getId())) {
                // Ma umowę roamingową
                roamingAvailable.add(op);
            } else {
                // Tylko roaming awaryjny (bez umowy)
                emergencyOnly.add(op);
            }
        }
        
        int count = 0;
        
        // Sekcja 1: Obecny operator
        if (!available.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "▸ OBECNA SIEĆ:");
            for (Operator op : available) {
                displayOperatorInfo(player, op, "obecny", currentOp);
                count++;
            }
        }
        
        // Sekcja 2: Roaming z umową
        if (!roamingAvailable.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "▸ ROAMING (z umową):");
            for (Operator op : roamingAvailable) {
                displayOperatorInfo(player, op, "roaming", currentOp);
                count++;
            }
        }
        
        // Sekcja 3: Tylko awaryjny
        if (!emergencyOnly.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "▸ TYLKO ALARMOWY (brak umowy):");
            for (Operator op : emergencyOnly) {
                displayOperatorInfo(player, op, "emergency", currentOp);
                count++;
            }
        }
        
        if (count == 0) {
            player.sendMessage(ChatColor.RED + "📵 Brak jakichkolwiek sieci w zasięgu!");
        }
        
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.GRAY + "💡 /przelacz <operator> - zmień sieć");
        player.sendMessage(ChatColor.GRAY + "💡 /roaming wlacz/wylacz - auto-roaming");
    }

    private void displayOperatorInfo(Player player, Operator op, String type, String currentOpId) {
        Station bestStation = plugin.getStationManager().findBestStation(player, op.getId());
        if (bestStation == null) return;
        
        double quality = bestStation.getSignalQuality(player.getLocation());
        String signalBar = getSignalBar(quality);
        String qualityText = getQualityText(quality);
        
        String prefix;
        switch (type) {
            case "obecny":
                prefix = ChatColor.GREEN + "  ▶ ";
                break;
            case "roaming":
                prefix = ChatColor.YELLOW + "  • ";
                break;
            case "emergency":
                prefix = ChatColor.RED + "  ⚠ ";
                break;
            default:
                prefix = "  ";
        }
        
        player.sendMessage(prefix + ChatColor.YELLOW + op.getDisplayName() + 
            ChatColor.GRAY + " " + signalBar + " " + qualityText);
        player.sendMessage(ChatColor.GRAY + "     " + bestStation.getTechnology() + 
            " | Stacja Lvl." + bestStation.getLevel());
        
        // Stawki
        if (type.equals("roaming")) {
            double roamingRate = op.getRoamingRate("minuta");
            player.sendMessage(ChatColor.GRAY + "     📞 $" + String.format("%.2f", roamingRate) + "/min");
        } else if (type.equals("emergency")) {
            double emergencyRate = op.getRate("minuta") * 3; // x3 stawka
            player.sendMessage(ChatColor.RED + "     📞 $" + String.format("%.2f", emergencyRate) + 
                "/min (awaryjna)");
        }
        
        // ID do przełączenia
        player.sendMessage(ChatColor.DARK_GRAY + "     /przelacz " + op.getId());
    }

    // Pasek sygnału
    private String getSignalBar(double quality) {
        int bars = (int) Math.ceil(quality * 5);
        bars = Math.min(5, Math.max(0, bars));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= bars) {
                if (bars >= 4) sb.append(ChatColor.GREEN);
                else if (bars >= 2) sb.append(ChatColor.YELLOW);
                else sb.append(ChatColor.RED);
                sb.append("█");
            } else {
                sb.append(ChatColor.DARK_GRAY).append("█");
            }
        }
        return sb.toString();
    }

    private String getQualityText(double quality) {
        if (quality >= 0.8) return ChatColor.GREEN + "Doskonały";
        if (quality >= 0.6) return ChatColor.DARK_GREEN + "Dobry";
        if (quality >= 0.4) return ChatColor.YELLOW + "Średni";
        if (quality >= 0.2) return ChatColor.GOLD + "Słaby";
        return ChatColor.RED + "B. słaby";
    }
}
