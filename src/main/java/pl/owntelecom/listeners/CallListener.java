package pl.owntelecom.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.managers.CallManager;

public class CallListener implements Listener {

    private final OwnTelecom plugin;
    private final CallManager callManager;

    public CallListener(OwnTelecom plugin) {
        this.plugin = plugin;
        this.callManager = plugin.getCallManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Sprawdź czy gracz jest w trakcie połączenia
        if (callManager.isInCall(player)) {
            // Jeśli chat lokalny jest aktywny i gracz jest w połączeniu,
            // przekieruj wiadomość do rozmowy telefonicznej
            event.setCancelled(true);
            
            // Wyślij wiadomość jako część rozmowy telefonicznej
            callManager.sendCallMessage(player, event.getMessage());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        callManager.handlePlayerQuit(player);
    }
}
