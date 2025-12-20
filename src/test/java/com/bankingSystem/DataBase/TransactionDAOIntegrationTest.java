package com.bankingSystem.DataBase;

import com.bankingSystem.Database.DatabaseConnection;
import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Transaction.Transaction;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

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
    void loadTransactions_nullOrBlank_shouldReturnEmpty() {
        assertTrue(dao.loadTransactions(null).isEmpty());
        assertTrue(dao.loadTransactions("").isEmpty());
        assertTrue(dao.loadTransactions("   ").isEmpty());
    }

    @Test
    void save_thenLoadById_shouldReturnSameTransaction_basic() {
        Transaction tx = new Transaction("DEPOSIT", 100, null, "ACC-1", "u1");
        tx.setStatus("COMPLETED");

        dao.saveTransaction(tx);

        Transaction loaded = dao.loadTransactionById(tx.getTransactionId());
        assertNotNull(loaded);
        assertEquals(tx.getTransactionId(), loaded.getTransactionId());
        assertEquals("DEPOSIT", loaded.getType());
        assertEquals(100, loaded.getAmount(), 0.001);
        assertEquals("COMPLETED", loaded.getStatus());
        assertEquals("u1", loaded.getPerformedBy());
        // لا تفترضي performedAt غير null لأن saveTransaction يسمح null
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

    @Test
    void loadPendingTransactions_shouldReturnOnlyPending() {
        Transaction t1 = new Transaction("TRANSFER", 10, "A1", "A2", "u");
        t1.setStatus("PENDING_MANAGER_APPROVAL");
        dao.saveTransaction(t1);

        Transaction t2 = new Transaction("DEPOSIT", 20, null, "A1", "u");
        t2.setStatus("COMPLETED");
        dao.saveTransaction(t2);

        var pending = dao.loadPendingTransactions();
        assertEquals(1, pending.size());
        assertEquals("PENDING_MANAGER_APPROVAL", pending.get(0).getStatus());
    }

    @Test
    void loadAllTransactions_shouldReturnAll() {
        Transaction t1 = new Transaction("DEPOSIT", 1, null, "A1", "u");
        t1.setStatus("COMPLETED");
        dao.saveTransaction(t1);

        Transaction t2 = new Transaction("WITHDRAW", 2, "A1", null, "u");
        t2.setStatus("COMPLETED");
        dao.saveTransaction(t2);

        assertEquals(2, dao.loadAllTransactions().size());
    }

    @Test
    void loadTransactions_forAccount_shouldReturnFromOrTo() {
        Transaction t1 = new Transaction("WITHDRAW", 5, "ACC-X", null, "u");
        t1.setStatus("COMPLETED");
        dao.saveTransaction(t1);

        Transaction t2 = new Transaction("DEPOSIT", 7, null, "ACC-X", "u");
        t2.setStatus("COMPLETED");
        dao.saveTransaction(t2);

        Transaction t3 = new Transaction("DEPOSIT", 9, null, "OTHER", "u");
        t3.setStatus("COMPLETED");
        dao.saveTransaction(t3);

        var list = dao.loadTransactions("ACC-X");
        assertEquals(2, list.size());
    }

    @Test
    void markExecuted_shouldSetPerformedAt_andStatus() {
        Transaction tx = new Transaction("TRANSFER", 33, "A1", "A2", "u1");
        tx.setStatus("PENDING_MANAGER_APPROVAL");
        dao.saveTransaction(tx);

        dao.markExecuted(tx.getTransactionId(), "COMPLETED");

        Transaction loaded = dao.loadTransactionById(tx.getTransactionId());
        assertNotNull(loaded);
        assertEquals("COMPLETED", loaded.getStatus());
        assertNotNull(loaded.getPerformedAt());
    }

    @Test
    void getDailyTransactionsForAccount_shouldReturnTodayTransactions() {
        Transaction tx = new Transaction("DEPOSIT", 10, null, "ACC-D", "u");
        tx.setStatus("COMPLETED");
        dao.saveTransaction(tx);

        List<Transaction> todays = dao.getDailyTransactionsForAccount("ACC-D");
        assertFalse(todays.isEmpty());
        assertTrue(todays.stream().allMatch(t ->
                "ACC-D".equals(t.getFromAccount()) || "ACC-D".equals(t.getToAccount())
        ));
    }

    @Test
    void getWeeklyTransactionsForAccount_shouldReturnLast7Days() throws Exception {
        // نضيف واحدة قديمة يدويًا (أقدم من 7 أيام) حتى نتأكد ما تطلع
        conn.createStatement().executeUpdate("""
            INSERT INTO Transactions(transactionId,type,amount,timestamp,fromAccount,toAccount,status,description,performedBy,performedAt)
            VALUES ('old-1','DEPOSIT',1, TIMESTAMPADD('DAY', -10, CURRENT_TIMESTAMP), NULL, 'ACC-W', 'COMPLETED','old','u', NULL)
        """);

        Transaction recent = new Transaction("DEPOSIT", 2, null, "ACC-W", "u");
        recent.setStatus("COMPLETED");
        dao.saveTransaction(recent);

        var weekly = dao.getWeeklyTransactionsForAccount("ACC-W");
        assertEquals(1, weekly.size());
        assertEquals(recent.getTransactionId(), weekly.get(0).getTransactionId());
    }

    @Test
    void getAccountsWithTransactionsToday_shouldReturnDistinctAccounts() {
        Transaction t1 = new Transaction("DEPOSIT", 1, null, "ACC-T1", "u");
        t1.setStatus("COMPLETED");
        dao.saveTransaction(t1);

        Transaction t2 = new Transaction("WITHDRAW", 1, "ACC-T2", null, "u");
        t2.setStatus("COMPLETED");
        dao.saveTransaction(t2);

        var accounts = dao.getAccountsWithTransactionsToday();
        assertTrue(accounts.contains("ACC-T1"));
        assertTrue(accounts.contains("ACC-T2"));
    }

    @Test
    void getAccountsWithTransactionsInWeek_shouldReturnDistinctAccounts() throws Exception {
        // قديم (خارج الأسبوع) - يدويًا
        conn.createStatement().executeUpdate("""
            INSERT INTO Transactions(transactionId,type,amount,timestamp,fromAccount,toAccount,status,description,performedBy,performedAt)
            VALUES ('old-2','TRANSFER',1, TIMESTAMPADD('DAY', -20, CURRENT_TIMESTAMP), 'A-OLD', 'B-OLD', 'COMPLETED','old','u', NULL)
        """);

        Transaction t = new Transaction("TRANSFER", 5, "A-NEW", "B-NEW", "u");
        t.setStatus("COMPLETED");
        dao.saveTransaction(t);

        var accounts = dao.getAccountsWithTransactionsInWeek();
        assertTrue(accounts.contains("A-NEW"));
        assertTrue(accounts.contains("B-NEW"));
        assertFalse(accounts.contains("A-OLD"));
    }
}
