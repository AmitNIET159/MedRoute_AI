package com.medroute.util;

import com.medroute.model.Role;
import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z\\s\\-]{2,100}$");

    private ValidationUtil() {}

    public static boolean isValidEmail(String email) {
        if (!isNotBlank(email)) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (!isNotBlank(phone)) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static boolean isValidName(String name) {
        if (!isNotBlank(name)) return false;
        return NAME_PATTERN.matcher(name).matches();
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }

    public static boolean isValidRole(String roleStr) {
        if (!isNotBlank(roleStr)) return false;
        try {
            Role.valueOf(roleStr.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValidFacilityId(Long id) {
        return id != null && id > 0;
    }
}
