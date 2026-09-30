package com.medroute.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(SessionInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        boolean isAuthenticated = false;

        if (session != null) {
            Boolean authAttr = (Boolean) session.getAttribute("AUTHENTICATED");
            Long userId = (Long) session.getAttribute("USER_ID");
            if (Boolean.TRUE.equals(authAttr) && userId != null) {
                isAuthenticated = true;
            }
        }

        if (!isAuthenticated) {
            logger.warn("Unauthenticated access attempt to: {}", request.getRequestURI());
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unauthorized\", \"status\": 401}");
            } else {
                response.sendRedirect(request.getContextPath() + "/auth/login?expired=true");
            }
            return false;
        }

        return true;
    }
}
