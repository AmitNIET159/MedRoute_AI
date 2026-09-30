package com.medroute.service;

import com.medroute.dao.NotificationDAO;
import com.medroute.dao.UserDAO;
import com.medroute.model.Notification;
import com.medroute.model.NotificationType;
import com.medroute.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Creates and manages in-app notifications for transfer events.
 * No email — in-app database notifications only.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;

    public NotificationService(NotificationDAO notificationDAO, UserDAO userDAO) {
        this.notificationDAO = notificationDAO;
        this.userDAO = userDAO;
    }

    /**
     * Sends a notification to all active users of the specified facility.
     */
    public void notifyFacility(Long facilityId, NotificationType type, String title,
                               String message, String referenceId, String referenceType) {
        try {
            List<User> users = userDAO.findActiveByFacilityId(facilityId);
            for (User user : users) {
                Notification n = new Notification();
                n.setUserId(user.getId());
                n.setFacilityId(facilityId);
                n.setType(type);
                n.setTitle(title);
                n.setMessage(message);
                n.setRead(false);
                n.setReferenceId(referenceId);
                n.setReferenceType(referenceType);
                notificationDAO.create(n);
            }
            logger.debug("Sent {} notification(s) to facility {} users", users.size(), facilityId);
        } catch (Exception e) {
            // Notification failure must never break the transfer operation
            logger.error("Failed to send notification to facility {}: {}", facilityId, e.getMessage());
        }
    }

    /**
     * Sends a notification to a specific user.
     */
    public void notifyUser(Long userId, Long facilityId, NotificationType type, String title,
                           String message, String referenceId, String referenceType) {
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setFacilityId(facilityId);
            n.setType(type);
            n.setTitle(title);
            n.setMessage(message);
            n.setRead(false);
            n.setReferenceId(referenceId);
            n.setReferenceType(referenceType);
            notificationDAO.create(n);
        } catch (Exception e) {
            logger.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }

    public List<Notification> getRecentNotifications(Long userId, int limit) {
        return notificationDAO.findByUserId(userId, limit);
    }

    public List<Notification> getUnreadNotifications(Long userId, int limit) {
        return notificationDAO.findUnreadByUserId(userId, limit);
    }

    public int getUnreadCount(Long userId) {
        return notificationDAO.countUnreadByUserId(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        notificationDAO.markAsRead(notificationId, userId);
    }

    public void markAllRead(Long userId) {
        notificationDAO.markAllReadByUserId(userId);
    }
}
