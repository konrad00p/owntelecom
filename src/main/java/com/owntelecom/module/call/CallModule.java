package com.owntelecom.module.call;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.AgreementType;
import com.owntelecom.database.model.Operator;
import com.owntelecom.database.model.Subscriber;
import com.owntelecom.module.OwnTelecomModule;
import com.owntelecom.service.CoverageResult;
import com.owntelecom.service.SignalService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class CallModule implements OwnTelecomModule {

    private OwnTelecomPlugin plugin;
    private final Map<UUID, ActiveCall> activeCalls = new HashMap<>();
    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();

    public record ActiveCall(UUID caller, UUID callee, long startedAt) {}
    public record PendingAction(String type, String target, String message) {}

    @Override
    public void enable(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
        org.bukkit.Bukkit.getPluginManager().registerEvents(new CallChatListener(plugin), plugin);
    }

    @Override
    public void disable(OwnTelecomPlugin plugin) {
        activeCalls.clear();
        pendingActions.clear();
    }

    @Override
    public String getName() {
        return "Call";
    }

    public boolean startCall(Player caller, Player callee, boolean confirmed) {
        var db = plugin.getDatabaseManager();
        Optional<Subscriber> callerSub = db.subscribers().findByPlayer(caller.getUniqueId());
        Optional<Subscriber> calleeSub = db.subscribers().findByPlayer(callee.getUniqueId());
        if (callerSub.isEmpty() || calleeSub.isEmpty()) {
            plugin.getMessageService().send(caller, "errors.no-operator");
            return false;
        }

        CoverageResult callerCov = plugin.getModuleManager().getCoverageService().getCoverage(caller);
        CoverageResult calleeCov = plugin.getModuleManager().getCoverageService().getCoverage(callee);
        if (!callerCov.inCoverage() || !calleeCov.inCoverage()) {
            plugin.getMessageService().send(caller, "errors.no-coverage");
            return false;
        }

        String homeA = callerSub.get().operatorId();
        String homeB = calleeSub.get().operatorId();
        if (!homeA.equalsIgnoreCase(homeB)) {
            var agreement = db.agreements().find(homeA, homeB, AgreementType.CALLS);
            if (agreement.isEmpty()) {
                plugin.getMessageService().send(caller, "call.no-agreement");
                return false;
            }
            if (!confirmed && agreement.get().callMinute() > 0) {
                pendingActions.put(caller.getUniqueId(), new PendingAction("call", callee.getName(), null));
                plugin.getMessageService().send(caller, "call.confirm-extra", Map.of(
                        "zone", "miedzynarodowa",
                        "cost", String.valueOf(agreement.get().callMinute()),
                        "player", callee.getName()
                ));
                return false;
            }
        }

        activeCalls.put(caller.getUniqueId(), new ActiveCall(caller.getUniqueId(), callee.getUniqueId(), System.currentTimeMillis()));
        activeCalls.put(callee.getUniqueId(), new ActiveCall(caller.getUniqueId(), callee.getUniqueId(), System.currentTimeMillis()));

        Optional<Operator> op = db.operators().findById(homeA);
        plugin.getMessageService().send(caller, "call.started", Map.of(
                "player", callee.getName(),
                "cost", op.map(o -> String.valueOf(o.prepaidMinute())).orElse("0")
        ));
        callee.sendMessage(plugin.getMessageService().colorize(
                "&a" + caller.getName() + " dzwoni do Ciebie. Odpowiedz przez chat."));
        return true;
    }

    public void endCall(UUID playerUuid) {
        ActiveCall call = activeCalls.remove(playerUuid);
        if (call != null) {
            activeCalls.remove(call.caller());
            activeCalls.remove(call.callee());
            Player p = Bukkit.getPlayer(playerUuid);
            if (p != null) {
                plugin.getMessageService().send(p, "call.ended");
            }
        }
    }

    public boolean isInCall(UUID uuid) {
        return activeCalls.containsKey(uuid);
    }

    public Optional<UUID> getCallPartner(UUID uuid) {
        ActiveCall call = activeCalls.get(uuid);
        if (call == null) {
            return Optional.empty();
        }
        return Optional.of(uuid.equals(call.caller()) ? call.callee() : call.caller());
    }

    public void sendCallMessage(Player sender, String message) {
        Optional<UUID> partnerUuid = getCallPartner(sender.getUniqueId());
        if (partnerUuid.isEmpty()) {
            return;
        }
        Player partner = Bukkit.getPlayer(partnerUuid.get());
        if (partner == null) {
            return;
        }
        CoverageResult cov = plugin.getModuleManager().getCoverageService().getCoverage(sender);
        String glitched = SignalService.glitchMessage(message, cov.voiceQuality());
        String formatted = "&e[TEL] &f" + sender.getName() + "&7: &f" + glitched;
        sender.sendMessage(plugin.getMessageService().colorize(formatted));
        partner.sendMessage(plugin.getMessageService().colorize(formatted));
        plugin.getModuleManager().getPaymentModule().chargeMinute(sender);
    }

    public boolean sendSms(Player sender, Player target, String message, boolean confirmed) {
        var db = plugin.getDatabaseManager();
        Optional<Subscriber> senderSub = db.subscribers().findByPlayer(sender.getUniqueId());
        Optional<Subscriber> targetSub = db.subscribers().findByPlayer(target.getUniqueId());
        if (senderSub.isEmpty()) {
            plugin.getMessageService().send(sender, "errors.no-operator");
            return false;
        }
        CoverageResult cov = plugin.getModuleManager().getCoverageService().getCoverage(sender);
        if (!cov.inCoverage()) {
            plugin.getMessageService().send(sender, "errors.no-coverage");
            return false;
        }
        CoverageResult targetCov = plugin.getModuleManager().getCoverageService().getCoverage(target);
        if (!targetCov.inCoverage()) {
            plugin.getMessageService().send(sender, "errors.no-coverage");
            return false;
        }

        String homeA = senderSub.get().operatorId();
        String homeB = targetSub.map(Subscriber::operatorId).orElse("");
        if (!homeB.isEmpty() && !homeA.equalsIgnoreCase(homeB)) {
            var agreement = db.agreements().find(homeA, homeB, AgreementType.CALLS);
            if (agreement.isEmpty()) {
                plugin.getMessageService().send(sender, "call.no-agreement");
                return false;
            }
            if (!confirmed && agreement.get().callSms() > 0) {
                pendingActions.put(sender.getUniqueId(), new PendingAction("sms", target.getName(), message));
                plugin.getMessageService().send(sender, "sms.confirm-extra", Map.of(
                        "zone", "miedzynarodowa",
                        "player", target.getName()
                ));
                return false;
            }
        }

        int maxLen = plugin.getConfigManager().getConfig().getInt("sms.max-length", 160);
        if (message.length() > maxLen) {
            message = message.substring(0, maxLen);
        }
        String glitched = SignalService.glitchMessage(message, cov.smsQuality());
        plugin.getMessageService().send(sender, "sms.sent", Map.of("player", target.getName()));
        plugin.getMessageService().send(target, "sms.received", Map.of("player", sender.getName(), "message", glitched));
        plugin.getModuleManager().getPaymentModule().chargeSms(sender);
        return true;
    }

    public Optional<PendingAction> consumePending(UUID uuid) {
        return Optional.ofNullable(pendingActions.remove(uuid));
    }

    public void sendEmergency(Player sender, String message) {
        List<com.owntelecom.database.model.Station> nearby =
                plugin.getModuleManager().getStationIndex().getNearby(sender.getLocation());
        boolean anyWorking = nearby.stream().anyMatch(s -> !s.broken());
        if (!anyWorking) {
            plugin.getMessageService().send(sender, "errors.no-coverage");
            return;
        }

        String perm = plugin.getConfigManager().getConfig().getString("calls.emergency-permission", "owntelecom.emergency.receive");
        var loc = sender.getLocation();
        Map<String, String> ph = Map.of(
                "player", sender.getName(),
                "message", message,
                "world", loc.getWorld().getName(),
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ())
        );
        plugin.getMessageService().send(sender, "emergency.sent");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(perm)) {
                plugin.getMessageService().send(staff, "emergency.received", ph);
            }
        }
    }
}
