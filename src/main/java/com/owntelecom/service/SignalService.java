package com.owntelecom.service;

import java.util.concurrent.ThreadLocalRandom;

public final class SignalService {

    private SignalService() {}

    public static String glitchMessage(String message, double quality) {
        if (quality >= 0.95 || message.isEmpty()) {
            return message;
        }
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (char c : message.toCharArray()) {
            if (c == ' ') {
                sb.append(' ');
                continue;
            }
            if (rnd.nextDouble() > quality) {
                if (rnd.nextBoolean()) {
                    sb.append('.');
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static long loadingDelayMs(double speedMbps, int baseMbCost) {
        if (speedMbps <= 0) {
            return 5000;
        }
        return (long) Math.min(10000, Math.max(500, (baseMbCost / speedMbps) * 1000));
    }
}
