package com.owntelecom.module.agreement;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Agreement;
import com.owntelecom.database.model.AgreementType;
import com.owntelecom.module.OwnTelecomModule;

public class AgreementModule implements OwnTelecomModule {

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
        return "Agreement";
    }

    public void createAgreement(String operatorA, String operatorB, AgreementType type,
                                double rate1, double rate2, double rate3,
                                boolean passToClient) {
        Agreement agreement = new Agreement(
                0,
                operatorA,
                operatorB,
                type,
                type == AgreementType.ROAMING ? rate1 : 0,
                type == AgreementType.ROAMING ? rate2 : 0,
                type == AgreementType.ROAMING ? rate3 : 0,
                type == AgreementType.CALLS ? rate1 : 0,
                type == AgreementType.CALLS ? rate2 : 0,
                type == AgreementType.ROAMING && passToClient,
                type == AgreementType.CALLS && passToClient,
                true
        );
        plugin.getDatabaseManager().agreements().create(agreement);
    }
}
