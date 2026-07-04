package com.owntelecom.database.model;

import java.util.UUID;

public record Subscriber(
        UUID playerUuid,
        String operatorId,
        Integer packageId,
        double packageMinutesLeft,
        double packageSmsLeft,
        double packageMbLeft,
        long packageExpiresAt
) {
    public boolean hasActivePackage(long now) {
        if (packageId == null) {
            return false;
        }
        if (packageExpiresAt > 0 && packageExpiresAt < now) {
            return false;
        }
        return packageMinutesLeft > 0 || packageSmsLeft > 0 || packageMbLeft > 0;
    }
}
