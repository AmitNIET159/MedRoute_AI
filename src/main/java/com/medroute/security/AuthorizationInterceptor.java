package com.medroute.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AuthorizationInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final Map<String, Set<String>> URL_ROLE_MAP = new LinkedHashMap<>();
    static {
        URL_ROLE_MAP.put("/admin/**", Set.of("ADMIN"));
        URL_ROLE_MAP.put("/inventory/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY"));
        URL_ROLE_MAP.put("/demand/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY"));
        URL_ROLE_MAP.put("/ai/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY"));
        URL_ROLE_MAP.put("/location/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY", "NGO", "WAREHOUSE"));
        URL_ROLE_MAP.put("/api/location/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY", "NGO", "WAREHOUSE"));
        URL_ROLE_MAP.put("/requests/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY", "NGO"));
        URL_ROLE_MAP.put("/matching/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY", "NGO"));
        URL_ROLE_MAP.put("/transfers/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY", "NGO"));
        URL_ROLE_MAP.put("/analytics/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY", "NGO"));
        URL_ROLE_MAP.put("/api/analytics/**", Set.of("ADMIN", "HOSPITAL", "CLINIC", "PHARMACY", "NGO"));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String role = (session != null) ? (String) session.getAttribute("ROLE") : null;
        Long userId = (session != null) ? (Long) session.getAttribute("USER_ID") : null;
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String path = requestUri.substring(contextPath.length());

        for (Map.Entry<String, Set<String>> entry : URL_ROLE_MAP.entrySet()) {
            if (matchesPattern(path, entry.getKey())) {
                Set<String> allowedRoles = entry.getValue();
                if (role == null || !allowedRoles.contains(role)) {
                    logger.warn("Access denied: user={}, role={}, path={}", userId, role, path);
                    String acceptHeader = request.getHeader("Accept");
                    if (acceptHeader != null && acceptHeader.contains("application/json")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Access denied\", \"status\": 403}");
                    } else {
                        request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchesPattern(String path, String pattern) {
        return pathMatcher.match(pattern, path);
    }
}
