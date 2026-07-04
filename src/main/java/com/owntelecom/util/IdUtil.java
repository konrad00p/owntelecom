package com.owntelecom.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class IdUtil {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]{3,16}$");

    private IdUtil() {}

    public static String normalizeId(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    public static String idFromDisplayName(String displayName) {
        return normalizeId(displayName.replace(" ", ""));
    }

    public static boolean isValidId(String id) {
        return ID_PATTERN.matcher(id).matches();
    }
}
