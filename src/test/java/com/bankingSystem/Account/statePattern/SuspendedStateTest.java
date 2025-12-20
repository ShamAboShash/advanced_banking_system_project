package com.bankingSystem.Account.statePattern;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SuspendedStateTest {

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @Test
    void deposit_whenSuspended_shouldThrow() {
        SavingsAccount acc = new SavingsAccount("SU-001", "owner1");
        acc.suspend();

        assertThrows(IllegalStateException.class, () -> acc.deposit(10));
    }

    @Test
    void activate_fromSuspended_shouldBecomeActive() {
        SavingsAccount acc = new SavingsAccount("SU-002", "owner1");
        acc.suspend();
        acc.activate();

        assertTrue(acc.state instanceof ActiveState);
    }
}
