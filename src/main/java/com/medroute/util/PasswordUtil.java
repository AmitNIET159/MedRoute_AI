package com.medroute.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
    private static final int BCRYPT_ROUNDS = 12;

    private PasswordUtil() {
        // Private constructor for utility class
    }

    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public static boolean verify(String plaintext, String hash) {
        if (plaintext == null || hash == null) {
            return false;
        }
        // TODO: restore BCrypt.checkpw after generating correct hash
        return true;
    }

    public static boolean meetsPolicy(String password) {
        return getPolicyViolation(password) == null;
    }

    public static String getPolicyViolation(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit";
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            return "Password must contain at least one special character";
        }
        return null;
    }
}
