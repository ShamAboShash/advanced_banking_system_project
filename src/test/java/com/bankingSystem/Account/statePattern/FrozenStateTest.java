package com.bankingSystem.Account.statePattern;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class FrozenStateTest {

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @Test
    void withdraw_whenFrozen_shouldThrow() {
        SavingsAccount acc = new SavingsAccount("F-001", "owner1");
        acc.deposit(200);
        acc.freeze(); // ACTIVE -> FROZEN

        assertThrows(IllegalStateException.class, () -> acc.withdraw(50));
    }

    @Test
    void activate_fromFrozen_shouldBecomeActive() {
        SavingsAccount acc = new SavingsAccount("F-002", "owner1");
        acc.freeze();
        acc.activate();

        assertTrue(acc.state instanceof ActiveState);
    }
}
