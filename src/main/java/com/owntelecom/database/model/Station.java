package com.owntelecom.database.model;

public record Station(
        int id,
        String operatorId,
        String world,
        int x,
        int y,
        int z,
        String technology,
        int level,
        boolean broken,
        long lastMaintenance
) {
}
