package com.bankingSystem.Account;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckingAccountTest {

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @Test
    void canOverdraft_withBalancePlusLimit_shouldReturnTrue() {
        CheckingAccount acc = new CheckingAccount("CHK-1", "owner");
        acc.deposit(100);

        assertTrue(acc.canOverdraft(500));   // 100 + 500 limit >= 500
        assertTrue(acc.canOverdraft(600));   // 100 + 500 = 600
        assertFalse(acc.canOverdraft(601));  // يتجاوز الحد
    }
}
