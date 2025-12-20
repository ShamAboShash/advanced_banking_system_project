package com.bankingSystem.Notification;

import com.bankingSystem.Database.NotificationDAO;
import com.bankingSystem.user.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationPersistenceObserverTest {

    @Test
    void update_shouldSaveNotificationRecordToDao() {
        NotificationDAO mockDao = mock(NotificationDAO.class);
        NotificationPersistenceObserver obs = new NotificationPersistenceObserver(mockDao);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_COMPLETED,
                "done",
                "tx1",
                Role.CUSTOMER,
                "u1"
        );

        obs.update(ev);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(mockDao, times(1)).save(captor.capture());

        NotificationRecord rec = captor.getValue();
        assertEquals("u1", rec.userId);
        assertEquals("Customer", rec.role); // getDisplayName
        assertEquals("TRANSACTION_COMPLETED", rec.type);
        assertEquals("done", rec.message);
        assertEquals("tx1", rec.transactionId);
        assertFalse(rec.delivered);
        assertFalse(rec.readFlag);
        assertNotNull(rec.createdAt);
    }
}
