package com.owntelecom.module.payment;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Operator;
import com.owntelecom.database.model.Subscriber;
import com.owntelecom.module.OwnTelecomModule;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class PaymentModule implements OwnTelecomModule {

    private OwnTelecomPlugin plugin;

    @Override
    public void enable(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void disable(OwnTelecomPlugin plugin) {
    }

    @Override
    public String getName() {
        return "Payment";
    }

    public boolean chargeMinute(Player player) {
        return chargeUsage(player, UsageType.MINUTE, 1);
    }

    public boolean chargeSms(Player player) {
        return chargeUsage(player, UsageType.SMS, 1);
    }

    public boolean chargeMb(Player player, double mb) {
        return chargeUsage(player, UsageType.MB, mb);
    }

    private boolean chargeUsage(Player player, UsageType type, double amount) {
        var db = plugin.getDatabaseManager();
        Optional<Subscriber> subOpt = db.subscribers().findByPlayer(player.getUniqueId());
        if (subOpt.isEmpty()) {
            plugin.getMessageService().send(player, "errors.no-operator");
            return false;
        }
        Subscriber sub = subOpt.get();
        long now = System.currentTimeMillis();

        if (sub.hasActivePackage(now)) {
            Subscriber updated = deductFromPackage(sub, type, amount);
            if (updated != null) {
                db.subscribers().updatePackage(updated);
                return true;
            }
        }

        Optional<Operator> opOpt = db.operators().findById(sub.operatorId());
        if (opOpt.isEmpty()) {
            return false;
        }
        Operator op = opOpt.get();
        double cost = switch (type) {
            case MINUTE -> op.prepaidMinute() * amount;
            case SMS -> op.prepaidSms() * amount;
            case MB -> op.prepaidMb() * amount;
        };

        if (cost <= 0) {
            return true;
        }
        if (!plugin.getEconomyService().isAvailable()) {
            plugin.getMessageService().send(player, "errors.vault-missing");
            return false;
        }
        if (!plugin.getEconomyService().has(player, cost)) {
            plugin.getMessageService().send(player, "errors.insufficient-funds", Map.of("cost", String.valueOf(cost)));
            return false;
        }
        plugin.getEconomyService().withdraw(player, cost);
        return true;
    }

    private Subscriber deductFromPackage(Subscriber sub, UsageType type, double amount) {
        double minutes = sub.packageMinutesLeft();
        double sms = sub.packageSmsLeft();
        double mb = sub.packageMbLeft();

        switch (type) {
            case MINUTE -> {
                if (minutes >= amount) {
                    minutes -= amount;
                } else {
                    return null;
                }
            }
            case SMS -> {
                if (sms >= amount) {
                    sms -= amount;
                } else {
                    return null;
                }
            }
            case MB -> {
                if (mb >= amount) {
                    mb -= amount;
                } else {
                    return null;
                }
            }
        }
        return new Subscriber(sub.playerUuid(), sub.operatorId(), sub.packageId(),
                minutes, sms, mb, sub.packageExpiresAt());
    }

    private enum UsageType {
        MINUTE, SMS, MB
    }
}
