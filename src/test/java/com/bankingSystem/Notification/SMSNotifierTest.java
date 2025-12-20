package com.bankingSystem.Notification;

import com.bankingSystem.user.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SMSNotifierTest {

    @Test
    void supports_shouldMatchRoleAndUserId() {
        SMSNotifier notifier = new SMSNotifier("u1", Role.MANAGER);

        NotificationEvent ok = new NotificationEvent(
                NotificationEventType.TRANSACTION_PENDING_MANAGER_APPROVAL,
                "needs approval",
                "tx99",
                Role.MANAGER,
                "u1"
        );

        NotificationEvent wrongUser = new NotificationEvent(
                NotificationEventType.TRANSACTION_PENDING_MANAGER_APPROVAL,
                "needs approval",
                "tx99",
                Role.MANAGER,
                "u2"
        );

        assertTrue(notifier.supports(ok));
        assertFalse(notifier.supports(wrongUser));
    }

    @Test
    void update_shouldNotThrow() {
        SMSNotifier notifier = new SMSNotifier("u1", Role.MANAGER);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_PENDING_MANAGER_APPROVAL,
                "needs approval",
                "tx99",
                Role.MANAGER,
                "u1"
        );

        assertDoesNotThrow(() -> notifier.update(ev));
    }
}
