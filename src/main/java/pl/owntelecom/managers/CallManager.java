package pl.owntelecom.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Call;
import pl.owntelecom.models.Operator;
import pl.owntelecom.models.Station;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CallManager {

    private final OwnTelecom plugin;
    private final Map<UUID, Call> activeCalls; // callId -> Call
    private final Map<Player, Call> playerCalls; // Gracz -> Jego aktywne połączenie
    private final Map<Player, Player> pendingCalls; // Odbiorca -> Dzwoniący (oczekujące)
    private final Random random;

    public CallManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.activeCalls = new ConcurrentHashMap<>();
        this.playerCalls = new ConcurrentHashMap<>();
        this.pendingCalls = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    // ==================== POŁĄCZENIA ====================

    // Inicjowanie połączenia
    public boolean initiateCall(Player caller, Player receiver) {
        // Sprawdź czy dzwoniący nie jest już w połączeniu
        if (isInCall(caller)) {
            caller.sendMessage(ChatColor.RED + "Już jesteś w trakcie połączenia! Użyj /rozlacz aby zakończyć.");
            return false;
        }

        // Sprawdź czy odbiorca nie jest w połączeniu
        if (isInCall(receiver)) {
            caller.sendMessage(ChatColor.RED + "Gracz " + receiver.getName() + " jest obecnie zajęty.");
            return false;
        }

        // Sprawdź czy nie dzwoni do siebie
        if (caller.equals(receiver)) {
            caller.sendMessage(ChatColor.RED + "Nie możesz zadzwonić do siebie!");
            return false;
        }

        // Sprawdź zasięg dzwoniącego
        String callerOperatorId = findPlayerOperator(caller);
        if (callerOperatorId == null) {
            caller.sendMessage(ChatColor.RED + "Nie jesteś w zasięgu żadnej sieci! Nie możesz wykonać połączenia.");
            return false;
        }

        // Sprawdź zasięg odbiorcy
        String receiverOperatorId = findPlayerOperator(receiver);
        if (receiverOperatorId == null) {
            caller.sendMessage(ChatColor.RED + "Gracz " + receiver.getName() + " jest poza zasięgiem sieci!");
            return false;
        }

        // Sprawdź umowę między operatorami
        if (!callerOperatorId.equals(receiverOperatorId)) {
            Operator callerOp = plugin.getOperatorManager().getOperator(callerOperatorId);
            Operator receiverOp = plugin.getOperatorManager().getOperator(receiverOperatorId);
            
            if (callerOp == null || receiverOp == null) {
                caller.sendMessage(ChatColor.RED + "Błąd systemu operatorskiego!");
                return false;
            }

            if (!callerOp.hasAgreement(receiverOperatorId) && !receiverOp.hasAgreement(callerOperatorId)) {
                caller.sendMessage(ChatColor.RED + "Brak umowy między operatorami! Połączenie niemożliwe.");
                return false;
            }
        }

        // Utwórz połączenie
        Call call = new Call(caller, receiver);
        call.setCallerOperatorId(callerOperatorId);
        call.setReceiverOperatorId(receiverOperatorId);

        // Oblicz jakość sygnału
        Station callerStation = plugin.getStationManager().findBestStation(caller, callerOperatorId);
        Station receiverStation = plugin.getStationManager().findBestStation(receiver, receiverOperatorId);
        
        double callerQuality = callerStation != null ? callerStation.getSignalQuality(caller.getLocation()) : 0.5;
        double receiverQuality = receiverStation != null ? receiverStation.getSignalQuality(receiver.getLocation()) : 0.5;
        double avgQuality = (callerQuality + receiverQuality) / 2.0;
        call.setSignalQuality(avgQuality);

        // Dodaj do oczekujących
        pendingCalls.put(receiver, caller);
        activeCalls.put(call.getCallId(), call);
        playerCalls.put(caller, call);

        // Powiadomienia
        caller.sendMessage(ChatColor.GREEN + "📞 Dzwonisz do " + ChatColor.YELLOW + receiver.getName() + ChatColor.GREEN + "...");
        
        String qualityText = getQualityText(avgQuality);
        receiver.sendMessage(ChatColor.GREEN + "📞 Połączenie przychodzące od " + ChatColor.YELLOW + caller.getName());
        receiver.sendMessage(ChatColor.GRAY + "Jakość połączenia: " + qualityText);
        receiver.sendMessage(ChatColor.YELLOW + "Użyj /odbierz aby odebrać lub /rozlacz aby odrzucić.");

        // Timer na nieodebrane połączenie (30 sekund)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (call.getState() == Call.CallState.RINGING) {
                endCall(call, "Nieodebrane");
                caller.sendMessage(ChatColor.RED + "📞 " + receiver.getName() + " nie odbiera.");
                receiver.sendMessage(ChatColor.GRAY + "📞 Nieodebrane połączenie od " + caller.getName());
            }
        }, 20 * 30);

        return true;
    }

    // Odbieranie połączenia
    public boolean acceptCall(Player receiver) {
        Player caller = pendingCalls.get(receiver);
        if (caller == null) {
            receiver.sendMessage(ChatColor.RED + "Nie masz żadnego oczekującego połączenia!");
            return false;
        }

        Call call = playerCalls.get(caller);
        if (call == null || call.getState() != Call.CallState.RINGING) {
            receiver.sendMessage(ChatColor.RED + "To połączenie już wygasło.");
            pendingCalls.remove(receiver);
            return false;
        }

        call.setState(Call.CallState.ACTIVE);
        playerCalls.put(receiver, call);
        pendingCalls.remove(receiver);

        caller.sendMessage(ChatColor.GREEN + "📞 Rozpoczęto rozmowę z " + ChatColor.YELLOW + receiver.getName());
        caller.sendMessage(ChatColor.GRAY + "Pisz na chacie aby rozmawiać. /rozlacz aby zakończyć.");
        
        receiver.sendMessage(ChatColor.GREEN + "📞 Rozpoczęto rozmowę z " + ChatColor.YELLOW + caller.getName());
        receiver.sendMessage(ChatColor.GRAY + "Pisz na chacie aby rozmawiać. /rozlacz aby zakończyć.");

        return true;
    }

    // Odrzucanie połączenia
    public boolean rejectCall(Player receiver) {
        Player caller = pendingCalls.get(receiver);
        if (caller == null) {
            return false;
        }

        Call call = playerCalls.get(caller);
        if (call != null) {
            endCall(call, "Odrzucone");
        }

        pendingCalls.remove(receiver);
        caller.sendMessage(ChatColor.RED + "📞 " + receiver.getName() + " odrzucił połączenie.");
        receiver.sendMessage(ChatColor.GRAY + "📞 Odrzucono połączenie od " + caller.getName());

        return true;
    }

    // Zakończenie połączenia
    public boolean endCall(Player player) {
        Call call = playerCalls.get(player);
        if (call == null) {
            player.sendMessage(ChatColor.RED + "Nie jesteś w trakcie połączenia!");
            return false;
        }

        endCall(call, "Zakończone");
        return true;
    }

    // Wewnętrzne zakończenie połączenia
    private void endCall(Call call, String reason) {
        call.endCall();
        
        Player caller = call.getCaller();
        Player receiver = call.getReceiver();

        // Rozliczenie kosztów
        if (call.getState() != Call.CallState.RINGING && call.getMessagesCount() > 0) {
            chargeForCall(call);
        }

        // Powiadomienia
        String duration = formatDuration(call.getDurationSeconds());
        caller.sendMessage(ChatColor.RED + "📞 Połączenie zakończone. (" + reason + ")");
        caller.sendMessage(ChatColor.GRAY + "Czas trwania: " + duration + " | Wiadomości: " + call.getMessagesCount());
        
        receiver.sendMessage(ChatColor.RED + "📞 Połączenie zakończone. (" + reason + ")");
        receiver.sendMessage(ChatColor.GRAY + "Czas trwania: " + duration + " | Wiadomości: " + call.getMessagesCount());

        // Czyszczenie
        playerCalls.remove(caller);
        playerCalls.remove(receiver);
        activeCalls.remove(call.getCallId());
        pendingCalls.remove(receiver);
    }

    // Wysyłanie wiadomości podczas połączenia
    public boolean sendCallMessage(Player sender, String message) {
        Call call = playerCalls.get(sender);
        if (call == null || call.getState() != Call.CallState.ACTIVE) {
            return false;
        }

        Player receiver = call.getOtherParty(sender);
        call.incrementMessages();

        // Zniekształcanie wiadomości przy słabym zasięgu
        String finalMessage = message;
        if (plugin.getConfigManager().isDistortionEnabled() && call.getSignalQuality() < 0.5) {
            finalMessage = distortMessage(message, call.getSignalQuality());
        }

        // Formatowanie wiadomości
        String callerFormat = ChatColor.DARK_GREEN + "[📞→ " + receiver.getName() + "] " + ChatColor.WHITE + finalMessage;
        String receiverFormat = ChatColor.DARK_GREEN + "[📞← " + sender.getName() + "] " + ChatColor.WHITE + finalMessage;

        sender.sendMessage(callerFormat);
        receiver.sendMessage(receiverFormat);

        return true;
    }

    // Zniekształcanie wiadomości
    private String distortMessage(String message, double quality) {
        if (quality >= 0.8) return message;
        
        StringBuilder distorted = new StringBuilder();
        double distortionChance = (1.0 - quality) * 0.7; // 70% szansy na zniekształcenie przy zerowej jakości
        
        for (char c : message.toCharArray()) {
            if (c == ' ') {
                distorted.append(' ');
            } else if (random.nextDouble() < distortionChance) {
                distorted.append('.');
            } else {
                distorted.append(c);
            }
        }
        
        return distorted.toString();
    }

    // Rozliczanie połączenia
    private void chargeForCall(Call call) {
        int messagesPerMinute = plugin.getConfigManager().getMessagesPerMinute();
        long minutes = Math.max(1, call.getMessagesCount() / messagesPerMinute);
        
        String callerOpId = call.getCallerOperatorId();
        String receiverOpId = call.getReceiverOperatorId();
        
        Operator callerOp = plugin.getOperatorManager().getOperator(callerOpId);
        Operator receiverOp = plugin.getOperatorManager().getOperator(receiverOpId);
        
        if (callerOp == null || receiverOp == null) return;

        // Pobierz stawki
        double rate = callerOp.getRate("minuta");
        double cost = minutes * rate;

        Player caller = call.getCaller();
        
        // Sprawdź czy gracz ma pieniądze
        if (plugin.getEconomy().has(caller, cost)) {
            plugin.getEconomy().withdrawPlayer(caller, cost);
            
            // Jeśli różni operatorzy, przekaż część do operatora odbiorcy
            if (!callerOpId.equals(receiverOpId)) {
                double roamingFee = receiverOp.getRate("minuta") * minutes * 0.5;
                // Tutaj można dodać logikę przekazywania pieniędzy między operatorami
            }
            
            caller.sendMessage(ChatColor.GRAY + "Koszt połączenia: $" + String.format("%.2f", cost) + 
                " (" + minutes + " min)");
        }
    }

    // ==================== SMS ====================

    public boolean sendSMS(Player sender, Player receiver, String message) {
        // Sprawdź czy nie wysyła do siebie
        if (sender.equals(receiver)) {
            sender.sendMessage(ChatColor.RED + "Nie możesz wysłać SMS do siebie!");
            return false;
        }

        // Sprawdź zasięg nadawcy
        String senderOperatorId = findPlayerOperator(sender);
        if (senderOperatorId == null) {
            sender.sendMessage(ChatColor.RED + "Nie jesteś w zasięgu sieci! Nie możesz wysłać SMS.");
            return false;
        }

        // Sprawdź zasięg odbiorcy
        String receiverOperatorId = findPlayerOperator(receiver);
        if (receiverOperatorId == null) {
            sender.sendMessage(ChatColor.RED + "Gracz " + receiver.getName() + " jest poza zasięgiem sieci!");
            return false;
        }

        // Sprawdź umowę między operatorami
        if (!senderOperatorId.equals(receiverOperatorId)) {
            Operator senderOp = plugin.getOperatorManager().getOperator(senderOperatorId);
            Operator receiverOp = plugin.getOperatorManager().getOperator(receiverOperatorId);
            
            if (senderOp == null || receiverOp == null) {
                sender.sendMessage(ChatColor.RED + "Błąd systemu operatorskiego!");
                return false;
            }

            if (!senderOp.hasAgreement(receiverOperatorId) && !receiverOp.hasAgreement(senderOperatorId)) {
                sender.sendMessage(ChatColor.RED + "Brak umowy między operatorami! SMS niemożliwy.");
                return false;
            }
        }

        // Pobierz stawkę za SMS
        Operator senderOp = plugin.getOperatorManager().getOperator(senderOperatorId);
        double smsCost = senderOp.getRate("sms");

        // Sprawdź czy gracz ma pieniądze
        if (!plugin.getEconomy().has(sender, smsCost)) {
            sender.sendMessage(ChatColor.RED + "Nie masz wystarczających środków! Koszt SMS: $" + smsCost);
            return false;
        }

        // Pobierz pieniądze
        plugin.getEconomy().withdrawPlayer(sender, smsCost);

        // Wyślij SMS
        String senderMsg = ChatColor.GOLD + "[SMS→ " + receiver.getName() + "] " + ChatColor.WHITE + message;
        String receiverMsg = ChatColor.GOLD + "[SMS← " + sender.getName() + "] " + ChatColor.WHITE + message;
        
        sender.sendMessage(senderMsg);
        sender.sendMessage(ChatColor.GRAY + "Koszt SMS: $" + String.format("%.2f", smsCost));
        receiver.sendMessage(receiverMsg);

        return true;
    }

    // ==================== NUMER ALARMOWY ====================

    public boolean sendAlarm(Player sender, String message) {
        // Sprawdź czy gracz jest w zasięgu JAKIEJKOLWIEK sieci
        boolean inRange = false;
        for (Operator op : plugin.getOperatorManager().getActiveOperators()) {
            if (plugin.getStationManager().isPlayerInRange(sender, op.getId())) {
                inRange = true;
                break;
            }
        }

        if (!inRange) {
            sender.sendMessage(ChatColor.RED + "⚠ Nie jesteś w zasięgu żadnej sieci! Numer alarmowy niedostępny.");
            return false;
        }

        // Wyślij alert do administracji i służb
        String alarmMsg = ChatColor.RED + "🚨 [ALARM 112] " + ChatColor.YELLOW + sender.getName() + 
                         ChatColor.WHITE + ": " + message;
        
        // Powiadom wszystkich z uprawnieniem owntelecom.admin
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("owntelecom.admin")) {
                player.sendMessage(alarmMsg);
                player.sendMessage(ChatColor.RED + "Lokalizacja: " + 
                    formatLocation(sender.getLocation()));
            }
        }

        // Log do konsoli
        plugin.getLogger().warning("[ALARM 112] " + sender.getName() + ": " + message);
        plugin.getLogger().warning("Lokalizacja: " + formatLocation(sender.getLocation()));

        // Potwierdzenie dla nadawcy
        sender.sendMessage(ChatColor.GREEN + "🚑 Zgłoszenie alarmowe wysłane! Służby zostały powiadomione.");
        sender.sendMessage(ChatColor.GRAY + "Treść: " + message);

        return true;
    }

    // ==================== POMOCNICZE ====================

    // Znajdź operatora gracza (na podstawie zasięgu)
    private String findPlayerOperator(Player player) {
        for (Operator op : plugin.getOperatorManager().getActiveOperators()) {
            if (plugin.getStationManager().isPlayerInRange(player, op.getId())) {
                return op.getId();
            }
        }
        return null;
    }

    // Sprawdź czy gracz jest w trakcie połączenia
    public boolean isInCall(Player player) {
        return playerCalls.containsKey(player);
    }

    // Pobierz aktywne połączenie gracza
    public Call getPlayerCall(Player player) {
        return playerCalls.get(player);
    }

    // Tekst jakości połączenia
    private String getQualityText(double quality) {
        if (quality >= 0.8) return ChatColor.GREEN + "★★★★★ Doskonała";
        if (quality >= 0.6) return ChatColor.DARK_GREEN + "★★★★☆ Dobra";
        if (quality >= 0.4) return ChatColor.YELLOW + "★★★☆☆ Średnia";
        if (quality >= 0.2) return ChatColor.GOLD + "★★☆☆☆ Słaba";
        return ChatColor.RED + "★☆☆☆☆ Bardzo słaba";
    }

    // Formatowanie czasu
    private String formatDuration(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    // Formatowanie lokalizacji
    private String formatLocation(org.bukkit.Location loc) {
        return String.format("Świat: %s X: %d Y: %d Z: %d", 
            loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // Czyszczenie przy wyjściu gracza
    public void handlePlayerQuit(Player player) {
        Call call = playerCalls.get(player);
        if (call != null) {
            endCall(player);
        }
        pendingCalls.remove(player);
    }
}
