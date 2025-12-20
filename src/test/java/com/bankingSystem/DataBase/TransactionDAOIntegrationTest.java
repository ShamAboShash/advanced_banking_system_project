package com.bankingSystem.DataBase;

import com.bankingSystem.Database.DatabaseConnection;
import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Transaction.Transaction;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDAOIntegrationTest {

    private TransactionDAO dao;
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        dao = new TransactionDAO();
        conn = DatabaseConnection.getInstance().getConnection();
        conn.createStatement().execute("DELETE FROM Transactions");
    }

    @Test
    void save_thenLoadById_shouldReturnSameTransaction() {
        // constructor الموجود عندك: (type, amount, from, to, performedBy)
        Transaction tx = new Transaction("DEPOSIT", 100, null, "ACC-1", "u1");
        tx.setStatus("COMPLETED");

        dao.saveTransaction(tx);

        Transaction loaded = dao.loadTransactionById(tx.getTransactionId());

        assertNotNull(loaded);
        assertEquals(tx.getTransactionId(), loaded.getTransactionId());
        assertEquals("DEPOSIT", loaded.getType());
        assertEquals(100, loaded.getAmount(), 0.001);
        assertEquals("COMPLETED", loaded.getStatus());
        // optional checks:
        assertEquals("u1", loaded.getPerformedBy());
        assertNotNull(loaded.getPerformedAt());
    }

    @Test
    void updateStatus_shouldPersistNewStatus() {
        Transaction tx = new Transaction("WITHDRAW", 50, "ACC-2", null, "u2");
        tx.setStatus("PENDING_MANAGER_APPROVAL");
        dao.saveTransaction(tx);

        dao.updateTransactionStatus(tx.getTransactionId(), "APPROVED_BY_MANAGER");

        Transaction loaded = dao.loadTransactionById(tx.getTransactionId());
        assertNotNull(loaded);
        assertEquals("APPROVED_BY_MANAGER", loaded.getStatus());
    }
}
