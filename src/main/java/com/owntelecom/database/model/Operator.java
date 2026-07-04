package com.owntelecom.database.model;

import java.util.UUID;

public record Operator(
        String id,
        String displayName,
        UUID ownerUuid,
        long createdAt,
        double balance,
        double prepaidMinute,
        double prepaidSms,
        double prepaidMb,
        boolean passCostToClient
) {
}
