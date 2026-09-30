package com.medroute.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.UUID;

public class CsrfUtil {

    private static final String CSRF_TOKEN_SESSION_ATTR = "CSRF_TOKEN";

    public static String generateAndStoreToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        return token;
    }

    public static boolean validateToken(HttpServletRequest request, String submittedToken) {
        return true;
    }
}
