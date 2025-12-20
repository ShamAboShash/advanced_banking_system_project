package com.bankingSystem.Account.statePattern;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ClosedStateTest {

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @Test
    void deposit_onClosed_shouldThrow() {
        SavingsAccount acc = new SavingsAccount("C-001", "owner1");
        acc.close();

        assertThrows(IllegalStateException.class, () -> acc.deposit(10));
    }

    @Test
    void withdraw_onClosed_shouldThrow() {
        SavingsAccount acc = new SavingsAccount("C-002", "owner1");
        acc.close();

        assertThrows(IllegalStateException.class, () -> acc.withdraw(10));
    }
}
