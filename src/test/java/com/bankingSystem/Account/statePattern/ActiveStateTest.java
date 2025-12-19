package com.bankingSystem.Account.statePattern;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ActiveStateTest {

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @Test
    void deposit_shouldIncreaseBalance() {
        SavingsAccount acc = new SavingsAccount("S-001", "owner1");

        acc.deposit(100);

        assertEquals(100, acc.getBalance(), 0.001);
    }

    @Test
    void withdraw_withInsufficientBalance_shouldThrowException() {
        SavingsAccount acc = new SavingsAccount("S-002", "owner1");

        assertThrows(IllegalStateException.class, () -> acc.withdraw(50));
    }
}
