package com.bankingSystem.DataBase;

import com.bankingSystem.Database.DatabaseConnection;
import com.bankingSystem.Database.NotificationDAO;
import com.bankingSystem.Notification.NotificationRecord;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDAOIntegrationTest {

    private NotificationDAO dao;
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        dao = new NotificationDAO();
        conn = DatabaseConnection.getInstance().getConnection();

        // مهم جداً لتجنب تداخل بيانات الاختبارات
        conn.createStatement().execute("DELETE FROM Notifications");
    }

    @Test
    void save_thenLoadUndelivered_shouldReturnSavedNotification() {
        NotificationRecord rec = new NotificationRecord();
        rec.userId = "u1";
        rec.role = "Customer";
        rec.type = "TRANSACTION_COMPLETED";
        rec.message = "Transaction done";
        rec.transactionId = "tx1";
        rec.createdAt = LocalDateTime.now();
        rec.delivered = false;
        rec.readFlag = false;

        dao.save(rec);

        List<NotificationRecord> undelivered = dao.loadUndelivered("u1");

        assertEquals(1, undelivered.size());
        NotificationRecord loaded = undelivered.get(0);

        assertNotNull(loaded.id);
        assertEquals("u1", loaded.userId);
        assertEquals("Customer", loaded.role);
        assertEquals("TRANSACTION_COMPLETED", loaded.type);
        assertEquals("Transaction done", loaded.message);
        assertEquals("tx1", loaded.transactionId);
        assertFalse(loaded.delivered);
        assertFalse(loaded.readFlag);
        assertNotNull(loaded.createdAt);
    }

    @Test
    void markDelivered_shouldRemoveFromUndelivered() {
        NotificationRecord rec = new NotificationRecord();
        rec.userId = "u2";
        rec.role = "Manager";
        rec.type = "TRANSACTION_PENDING_MANAGER_APPROVAL";
        rec.message = "Needs approval";
        rec.transactionId = "tx99";
        rec.createdAt = LocalDateTime.now();
        rec.delivered = false;
        rec.readFlag = false;

        dao.save(rec);

        List<NotificationRecord> before = dao.loadUndelivered("u2");
        assertEquals(1, before.size());

        String id = before.get(0).id;
        dao.markDelivered(List.of(id));

        List<NotificationRecord> after = dao.loadUndelivered("u2");
        assertEquals(0, after.size(), "After marking delivered, it should not appear in undelivered list");
    }

    @Test
    void loadUnread_thenMarkRead_shouldRemoveFromUnread() {
        NotificationRecord rec = new NotificationRecord();
        rec.userId = "u3";
        rec.role = "Customer";
        rec.type = "TRANSACTION_FAILED";
        rec.message = "Failed";
        rec.transactionId = "tx77";
        rec.createdAt = LocalDateTime.now();
        rec.delivered = true;   // delivered doesn't matter for unread, but ok
        rec.readFlag = false;   // unread

        dao.save(rec);

        List<NotificationRecord> unreadBefore = dao.loadUnread("u3");
        assertEquals(1, unreadBefore.size());

        String id = unreadBefore.get(0).id;
        dao.markRead(id);

        List<NotificationRecord> unreadAfter = dao.loadUnread("u3");
        assertEquals(0, unreadAfter.size(), "After markRead, it should not appear in unread list");
    }
}
