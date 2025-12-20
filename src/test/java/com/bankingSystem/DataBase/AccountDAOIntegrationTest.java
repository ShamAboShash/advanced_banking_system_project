package com.bankingSystem.DataBase;

import com.bankingSystem.Account.*;
import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Database.DatabaseConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class AccountDAOIntegrationTest {

    private AccountDAO dao;
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        dao = new AccountDAO();
        conn = DatabaseConnection.getInstance().getConnection();

        // تنظيف حتى ما تتداخل النتائج
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM Accounts");
            st.executeUpdate("DELETE FROM Transactions"); // لأن loadAccount بيجيب history
        }
    }

    @Test
    void loadAccount_nonExisting_shouldReturnNull() {
        assertNull(dao.loadAccount("NO-ACC"));
    }

    @Test
    void getAccountsByOwner_whenNone_shouldReturnEmptyList() {
        assertTrue(dao.getAccountsByOwner("no-owner").isEmpty());
    }

    @Test
    void getAllAccounts_shouldReturnSavedOnes() {
        dao.saveAccount(new SavingsAccount("S-1", "o1"));
        dao.saveAccount(new CheckingAccount("C-1", "o2"));

        var all = dao.getAllAccounts();
        assertTrue(all.size() >= 2);
    }

    @Test
    void updateBalance_existingAccount_shouldPersist() {
        SavingsAccount acc = new SavingsAccount("S-200", "owner");
        acc.deposit(50);
        dao.saveAccount(acc);

        dao.updateBalance("S-200", 999);

        Account loaded = dao.loadAccount("S-200");
        assertNotNull(loaded);
        assertEquals(999, loaded.getBalance(), 0.001);
    }

    @Test
    void updateBalance_nonExisting_shouldNotThrow() {
        assertDoesNotThrow(() -> dao.updateBalance("NO-ACC", 123));
    }

    @Test
    void updateAccountState_existing_shouldPersist() {
        SavingsAccount acc = new SavingsAccount("S-300", "owner");
        dao.saveAccount(acc);

        dao.updateAccountState("S-300", "Frozen");

        Account loaded = dao.loadAccount("S-300");
        assertNotNull(loaded);
        assertEquals("Frozen", loaded.state.getClass().getSimpleName().replace("State", ""));
    }

    @Test
    void updateAccountState_nonExisting_shouldNotThrow() {
        assertDoesNotThrow(() -> dao.updateAccountState("NO-ACC", "Suspended"));
    }

    @Test
    void deleteAccount_existing_shouldRemove() {
        SavingsAccount acc = new SavingsAccount("S-400", "owner");
        dao.saveAccount(acc);

        dao.deleteAccount("S-400");

        assertNull(dao.loadAccount("S-400"));
    }

    @Test
    void deleteAccount_nonExisting_shouldNotThrow() {
        assertDoesNotThrow(() -> dao.deleteAccount("NO-ACC"));
    }

    @Test
    void loadAccount_unknownType_shouldThrowIllegalArgumentException() throws Exception {
        // ندخل row يدوي بالـ type = "WeirdAccount" لنضرب default branch
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                INSERT INTO Accounts(accountId, accountNumber, balance, creationDate, ownerId, state, type, loanAmount)
                VALUES('x1','X-1',10,CURRENT_TIMESTAMP,'owner','Active','WeirdAccount',0)
            """);
        }

        assertThrows(IllegalArgumentException.class, () -> dao.loadAccount("X-1"));
    }
}
