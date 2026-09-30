package com.medroute.service;

import com.medroute.dao.NotificationDAO;
import com.medroute.dao.UserDAO;
import com.medroute.model.Notification;
import com.medroute.model.NotificationType;
import com.medroute.model.Role;
import com.medroute.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock private NotificationDAO notificationDAO;
    @Mock private UserDAO userDAO;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNotifyFacility_createsNotificationsForAllUsers() {
        User u1 = makeUser(1L, 10L);
        User u2 = makeUser(2L, 10L);
        when(userDAO.findActiveByFacilityId(10L)).thenReturn(List.of(u1, u2));
        when(notificationDAO.create(any())).thenReturn(1L);

        notificationService.notifyFacility(10L, NotificationType.TRANSFER_REQUEST,
                "Test Title", "Test Message", "100", "TransferRequest");

        // One notification per user
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDAO, times(2)).create(captor.capture());

        List<Notification> notifications = captor.getAllValues();
        assertEquals(1L, notifications.get(0).getUserId());
        assertEquals(2L, notifications.get(1).getUserId());
        assertEquals(NotificationType.TRANSFER_REQUEST, notifications.get(0).getType());
        assertEquals("Test Title", notifications.get(0).getTitle());
        assertEquals("100", notifications.get(0).getReferenceId());
    }

    @Test
    void testNotifyFacility_failureSilent() {
        when(userDAO.findActiveByFacilityId(10L)).thenThrow(new RuntimeException("DB error"));

        // Should NOT throw — notification failure is silent
        assertDoesNotThrow(() ->
                notificationService.notifyFacility(10L, NotificationType.TRANSFER_UPDATE,
                        "Title", "Msg", "1", "TR"));
    }

    @Test
    void testGetUnreadCount() {
        when(notificationDAO.countUnreadByUserId(1L)).thenReturn(5);

        assertEquals(5, notificationService.getUnreadCount(1L));
    }

    @Test
    void testMarkAsRead() {
        notificationService.markAsRead(10L, 1L);

        verify(notificationDAO).markAsRead(10L, 1L);
    }

    @Test
    void testMarkAllRead() {
        notificationService.markAllRead(1L);

        verify(notificationDAO).markAllReadByUserId(1L);
    }

    private User makeUser(Long id, Long facilityId) {
        User u = new User();
        u.setId(id);
        u.setFacilityId(facilityId);
        u.setRole(Role.HOSPITAL);
        u.setActive(true);
        return u;
    }
}
