package com.bankingSystem.Notification;

import com.bankingSystem.user.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InAppNotifierTest {

    @Test
    void supports_shouldReturnTrue_whenSameRoleAndSameUserId() {
        InAppNotifier notifier = new InAppNotifier("u1", Role.CUSTOMER);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_COMPLETED,
                "done",
                "tx1",
                Role.CUSTOMER,
                "u1"
        );

        assertTrue(notifier.supports(ev));
    }

    @Test
    void supports_shouldReturnFalse_whenDifferentRole() {
        InAppNotifier notifier = new InAppNotifier("u1", Role.CUSTOMER);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_COMPLETED,
                "done",
                "tx1",
                Role.MANAGER,
                "u1"
        );

        assertFalse(notifier.supports(ev));
    }

    @Test
    void supports_shouldReturnFalse_whenDifferentUserId() {
        InAppNotifier notifier = new InAppNotifier("u1", Role.CUSTOMER);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_COMPLETED,
                "done",
                "tx1",
                Role.CUSTOMER,
                "u2"
        );

        assertFalse(notifier.supports(ev));
    }

    @Test
    void update_shouldNotThrow() {
        InAppNotifier notifier = new InAppNotifier("u1", Role.CUSTOMER);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_COMPLETED,
                "done",
                "tx1",
                Role.CUSTOMER,
                "u1"
        );

        assertDoesNotThrow(() -> notifier.update(ev));
    }
}
