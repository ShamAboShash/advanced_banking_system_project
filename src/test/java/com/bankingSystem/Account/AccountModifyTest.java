package com.bankingSystem.Account;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountModifyTest {

    @BeforeAll
    static void disableDb() { Account.disablePersistenceForTests(); }

    @Test
    void modify_owner_shouldChangeOwner() {
        SavingsAccount acc = new SavingsAccount("S-500", "oldOwner");
        acc.modify("owner", "newOwner");
        assertEquals("newOwner", acc.getOwnerId());
    }

    @Test
    void updateOwner_empty_shouldThrow() {
        SavingsAccount acc = new SavingsAccount("S-501", "o1");
        assertThrows(IllegalArgumentException.class, () -> acc.updateOwner("   "));
    }

    @Test
    void modify_overdraftLimit_onChecking_shouldUpdate() {
        CheckingAccount acc = new CheckingAccount("C-001", "o1");
        acc.modify("overdraftlimit", "999");
        assertTrue(acc.canOverdraft(900)); // قبلها ممكن ما كان يسمح
    }

    @Test
    void modify_overdraftLimit_onSavings_shouldThrow() {
        SavingsAccount acc = new SavingsAccount("S-502", "o1");
        assertThrows(IllegalStateException.class, () -> acc.modify("overdraftlimit", "999"));
    }
}
