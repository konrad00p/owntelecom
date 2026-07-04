package com.owntelecom.module.operator;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Agreement;
import com.owntelecom.database.model.AgreementType;
import com.owntelecom.database.model.Operator;
import com.owntelecom.module.OwnTelecomModule;
import com.owntelecom.util.IdUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OperatorModule implements OwnTelecomModule {

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
        return "Operator";
    }

    public Optional<Operator> createOperator(Player player, String displayName, String id) {
        String normalizedId = IdUtil.normalizeId(id.isEmpty() ? IdUtil.idFromDisplayName(displayName) : id);
        if (!IdUtil.isValidId(normalizedId)) {
            return Optional.empty();
        }

        var db = plugin.getDatabaseManager();
        if (db.operators().findById(normalizedId).isPresent()) {
            return Optional.empty();
        }

        var meta = db.playerMeta().getOrCreate(player.getUniqueId());
        if (meta.hasCreatedOperator() && !player.hasPermission("owntelecom.bypass.cooldown")) {
            return Optional.empty();
        }

        long cooldownDays = plugin.getConfigManager().getConfig().getLong("operator.create-cooldown-days", 7);
        if (!player.hasPermission("owntelecom.bypass.cooldown") && meta.lastOperatorCreate() > 0) {
            long elapsed = System.currentTimeMillis() - meta.lastOperatorCreate();
            if (elapsed < TimeUnit.DAYS.toMillis(cooldownDays)) {
                return Optional.empty();
            }
        }

        double cost = plugin.getConfigManager().getConfig().getDouble("operator.create-cost", 1000);
        if (cost > 0 && !plugin.getEconomyService().withdraw(player, cost)) {
            return Optional.empty();
        }

        Operator operator = new Operator(
                normalizedId,
                displayName,
                player.getUniqueId(),
                System.currentTimeMillis(),
                0,
                1.0, 0.5, 0.1,
                true
        );
        db.operators().create(operator);
        db.playerMeta().markOperatorCreated(player.getUniqueId());
        db.subscribers().setOperator(player.getUniqueId(), normalizedId);

        autoRoamingForSameOwner(player.getUniqueId());
        return Optional.of(operator);
    }

    private void autoRoamingForSameOwner(UUID ownerUuid) {
        if (!plugin.getConfigManager().getConfig().getBoolean("operator.auto-roaming-between-same-owner", true)) {
            return;
        }
        List<Operator> owned = plugin.getDatabaseManager().operators().findByOwner(ownerUuid);
        if (owned.size() < 2) {
            return;
        }
        for (int i = 0; i < owned.size(); i++) {
            for (int j = i + 1; j < owned.size(); j++) {
                createFreeAgreement(owned.get(i).id(), owned.get(j).id(), AgreementType.ROAMING);
                createFreeAgreement(owned.get(i).id(), owned.get(j).id(), AgreementType.CALLS);
            }
        }
    }

    private void createFreeAgreement(String a, String b, AgreementType type) {
        plugin.getDatabaseManager().agreements().create(new Agreement(
                0, a, b, type,
                0, 0, 0,
                0, 0,
                false, false,
                true
        ));
    }

    public boolean deleteOperator(String id, Player actor) {
        var db = plugin.getDatabaseManager();
        Optional<Operator> op = db.operators().findById(id);
        if (op.isEmpty()) {
            return false;
        }
        if (!actor.hasPermission("owntelecom.admin") && !op.get().ownerUuid().equals(actor.getUniqueId())) {
            return false;
        }
        db.stations().deleteByOperator(id);
        db.operators().delete(id);
        plugin.getModuleManager().getStationIndex().rebuild();
        return true;
    }

    public boolean transferOperator(String id, UUID newOwner) {
        var db = plugin.getDatabaseManager();
        if (db.operators().findById(id).isEmpty()) {
            return false;
        }
        db.operators().updateOwner(id, newOwner);
        autoRoamingForSameOwner(newOwner);
        return true;
    }

    public List<Operator> listOperators() {
        return plugin.getDatabaseManager().operators().findAll();
    }
}
