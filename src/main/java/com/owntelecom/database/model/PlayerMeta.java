package com.owntelecom.database.model;

import java.util.UUID;

public record PlayerMeta(
        UUID playerUuid,
        long lastOperatorCreate,
        boolean hasCreatedOperator
) {
}
