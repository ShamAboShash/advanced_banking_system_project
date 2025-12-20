package com.bankingSystem.DataBase;

import com.bankingSystem.Account.*;
import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Database.DatabaseConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class AccountDAOIntegrationTest {

    private AccountDAO dao;
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        dao = new AccountDAO();
        conn = DatabaseConnection.getInstance().getConnection();
        conn.createStatement().execute("DELETE FROM Accounts");
        conn.createStatement().execute("DELETE FROM Transactions"); // لأن loadAccount يقرأ history
    }

    @Test
    void saveAndLoad_savings_shouldPersistFields() {
        SavingsAccount acc = new SavingsAccount("S-900", "ownerX");
        acc.deposit(150);
        acc.freeze();
        dao.saveAccount(acc);

        Account loaded = dao.loadAccount("S-900");
        assertNotNull(loaded);
        assertEquals("S-900", loaded.getAccountNumber());
        assertEquals("ownerX", loaded.getOwnerId());
        assertEquals(150, loaded.getBalance(), 0.001);
        assertEquals("Frozen", loaded.state.getClass().getSimpleName().replace("State", ""));
        assertTrue(loaded instanceof SavingsAccount);
    }

    @Test
    void saveAndLoad_checking_shouldPersistTypeAndDefaultStateActive() {
        CheckingAccount acc = new CheckingAccount("C-101", "ownerA");
        acc.deposit(30);
        dao.saveAccount(acc);

        Account loaded = dao.loadAccount("C-101");
        assertNotNull(loaded);
        assertTrue(loaded instanceof CheckingAccount);
        // إذا ما غيّرتي state قبل الحفظ غالباً Active
        assertEquals("Active", loaded.state.getClass().getSimpleName().replace("State", ""));
    }

    @Test
    void saveAndLoad_investment_shouldWork() {
        InvestmentAccount inv = new InvestmentAccount("I-77", "ownerInv");
        inv.deposit(999);
        dao.saveAccount(inv);

        Account loaded = dao.loadAccount("I-77");
        assertNotNull(loaded);
        assertTrue(loaded instanceof InvestmentAccount);
        assertEquals(999, loaded.getBalance(), 0.001);
    }

    @Test
    void saveAndLoad_loan_shouldPersistLoanAmount_andStateSuspended() {
        LoanAccount loan = new LoanAccount("L-001", "ownerL", 5000);
        loan.suspend();
        dao.saveAccount(loan);

        Account loaded = dao.loadAccount("L-001");
        assertNotNull(loaded);
        assertTrue(loaded instanceof LoanAccount);
        assertEquals("Suspended", loaded.state.getClass().getSimpleName().replace("State", ""));
        // إذا عندك getter للـ loanAmount زيدي assert عليه
    }

    @Test
    void updateBalance_shouldChangeBalanceInDb() {
        SavingsAccount acc = new SavingsAccount("S-1", "o1");
        acc.deposit(10);
        dao.saveAccount(acc);

        dao.updateBalance("S-1", 777.0);

        Account loaded = dao.loadAccount("S-1");
        assertNotNull(loaded);
        assertEquals(777.0, loaded.getBalance(), 0.001);
    }

    @Test
    void updateAccountState_shouldPersistState() {
        SavingsAccount acc = new SavingsAccount("S-2", "o2");
        dao.saveAccount(acc);

        dao.updateAccountState("S-2", "Frozen");

        Account loaded = dao.loadAccount("S-2");
        assertNotNull(loaded);
        assertEquals("Frozen", loaded.state.getClass().getSimpleName().replace("State", ""));
    }

    @Test
    void deleteAccount_existing_shouldRemoveIt() {
        SavingsAccount acc = new SavingsAccount("S-del", "o3");
        dao.saveAccount(acc);

        assertNotNull(dao.loadAccount("S-del"));

        dao.deleteAccount("S-del");
        assertNull(dao.loadAccount("S-del"));
    }

    @Test
    void deleteAccount_nonExisting_shouldNotThrow() {
        assertDoesNotThrow(() -> dao.deleteAccount("NOPE-123"));
    }

    @Test
    void getAllAccounts_shouldReturnAll() {
        dao.saveAccount(new SavingsAccount("S-a", "o"));
        dao.saveAccount(new CheckingAccount("C-a", "o"));
        dao.saveAccount(new InvestmentAccount("I-a", "o"));

        var all = dao.getAllAccounts();
        assertNotNull(all);
        assertEquals(3, all.size());
    }

    @Test
    void getAccountsByOwner_shouldFilter() {
        dao.saveAccount(new SavingsAccount("S-o1", "owner1"));
        dao.saveAccount(new CheckingAccount("C-o1", "owner1"));
        dao.saveAccount(new SavingsAccount("S-o2", "owner2"));

        var owner1 = dao.getAccountsByOwner("owner1");
        assertEquals(2, owner1.size());

        var owner2 = dao.getAccountsByOwner("owner2");
        assertEquals(1, owner2.size());
    }

    @Test
    void loadAccount_unknownTypeInDb_shouldThrowIllegalArgument() throws Exception {
        // ندخل record يدويًا بنوع غير معروف حتى نغطي default -> throw
        conn.createStatement().executeUpdate("""
            INSERT INTO Accounts(accountId, accountNumber, balance, creationDate, ownerId, state, type, loanAmount)
            VALUES ('x1', 'X-404', 0, CURRENT_TIMESTAMP, 'o', 'Active', 'WeirdAccount', 0)
        """);

        assertThrows(IllegalArgumentException.class, () -> dao.loadAccount("X-404"));
    }

    @Test
    void loadAccount_unknownState_shouldFallbackToActive() throws Exception {
        conn.createStatement().executeUpdate("""
            INSERT INTO Accounts(accountId, accountNumber, balance, creationDate, ownerId, state, type, loanAmount)
            VALUES ('x2', 'S-unknownState', 5, CURRENT_TIMESTAMP, 'o', 'SOME_NEW_STATE', 'SavingsAccount', 0)
        """);

        Account loaded = dao.loadAccount("S-unknownState");
        assertNotNull(loaded);
        assertEquals("Active", loaded.state.getClass().getSimpleName().replace("State", ""));
    }
}
