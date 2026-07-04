package com.owntelecom.database.model;

import java.util.UUID;

public record Website(
        int id,
        String slug,
        UUID ownerUuid,
        String operatorId,
        Integer serverId,
        String title,
        boolean enabled,
        boolean broken,
        String template
) {
}
