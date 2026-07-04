package com.owntelecom.database.model;

public record Agreement(
        int id,
        String operatorA,
        String operatorB,
        AgreementType type,
        double roamingMinute,
        double roamingSms,
        double roamingMb,
        double callMinute,
        double callSms,
        boolean passRoamingToClient,
        boolean passCallToClient,
        boolean active
) {
}
