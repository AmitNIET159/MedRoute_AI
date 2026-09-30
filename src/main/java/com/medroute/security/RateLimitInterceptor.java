package com.medroute.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    private static final int AUTH_MAX_REQUESTS = 20;
    private static final int AUTH_WINDOW_SECONDS = 60;
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final int LOGIN_WINDOW_SECONDS = 900; // 15 min

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String requestURI = request.getRequestURI();
        
        String endpointCategory = "AUTH";
        int maxRequests = AUTH_MAX_REQUESTS;
        int windowSeconds = AUTH_WINDOW_SECONDS;

        if (requestURI.contains("/auth/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            endpointCategory = "LOGIN";
            maxRequests = LOGIN_MAX_ATTEMPTS;
            windowSeconds = LOGIN_WINDOW_SECONDS;
        }

        String key = clientIp + ":" + endpointCategory;
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Cleanup old timestamps
        long windowStart = now - (windowSeconds * 1000L);
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            logger.warn("Rate limit exceeded for IP: {} on category: {}", clientIp, endpointCategory);
            
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                response.setStatus(429); // 429 Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\", \"status\": 429}");
            } else {
                response.sendError(429, "Too many requests. Please try again later.");
            }
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
