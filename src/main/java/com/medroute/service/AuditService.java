package com.medroute.service;

import com.medroute.dao.AuditDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditDAO auditDAO;

    public void logAction(Long userId, String action, String entityType, String entityId, HttpServletRequest request) {
        logAction(userId, action, entityType, entityId, null, null, request);
    }

    public void logAction(Long userId, String action, String entityType, String entityId,
                          String oldValue, String newValue, HttpServletRequest request) {
        try {
            String ipAddress = null;
            String userAgent = null;

            if (request != null) {
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                userAgent = request.getHeader("User-Agent");
            }

            auditDAO.log(userId, action, entityType, entityId, oldValue, newValue, ipAddress, userAgent);
        } catch (Exception e) {
            logger.error("Failed to log audit action: {}", action, e);
        }
    }
}
