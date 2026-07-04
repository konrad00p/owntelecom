package com.owntelecom.service;

import com.owntelecom.database.model.Station;

public record CoverageResult(
        boolean inCoverage,
        boolean roaming,
        String homeOperatorId,
        String servingOperatorId,
        Station bestStation,
        String technology,
        double distancePercent,
        double speedMbps,
        double voiceQuality,
        double smsQuality,
        boolean internetEnabled
) {
    public static CoverageResult none() {
        return new CoverageResult(false, false, null, null, null, null, 100, 0, 0, 0, false);
    }
}
