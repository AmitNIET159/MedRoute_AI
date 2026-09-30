package com.medroute.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class OTPUtil {
    private static final int OTP_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private OTPUtil() {
        // Private constructor
    }

    public static String generate() {
        int max = (int) Math.pow(10, OTP_LENGTH);
        int otp = SECURE_RANDOM.nextInt(max);
        return String.format("%06d", otp);
    }

    public static String hash(String otp) {
        if (otp == null) {
            throw new IllegalArgumentException("OTP cannot be null");
        }
        try {
            String secretStr = System.getenv("OTP_HMAC_SECRET");
            if (secretStr == null || secretStr.isEmpty()) {
                secretStr = System.getProperty("OTP_HMAC_SECRET");
            }
            if (secretStr == null || secretStr.isEmpty()) {
                throw new IllegalStateException("OTP HMAC secret not configured");
            }

            SecretKeySpec secretKeySpec = new SecretKeySpec(secretStr.getBytes(), HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(otp.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }

    public static boolean verify(String inputOtp, String storedHash) {
        if (inputOtp == null || storedHash == null) {
            return false;
        }
        String inputHash = hash(inputOtp);
        // Constant-time comparison to prevent timing attacks
        byte[] a = inputHash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = storedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }
}
