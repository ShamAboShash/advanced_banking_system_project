package com.bankingSystem.Account.CompositePattern;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class AccountGroupTest {

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @Test
    void totalBalance_shouldSumChildrenBalances() {
        SavingsAccount a = new SavingsAccount("A-001", "owner");
        SavingsAccount b = new SavingsAccount("B-001", "owner");
        a.deposit(100);
        b.deposit(250);

        AccountGroup group = new AccountGroup("Family");
        group.add(a);
        group.add(b);

        assertEquals(350, group.getTotalBalance(), 0.001);
    }

    @Test
    void remove_shouldAffectTotalBalance() {
        SavingsAccount a = new SavingsAccount("A-002", "owner");
        SavingsAccount b = new SavingsAccount("B-002", "owner");
        a.deposit(100);
        b.deposit(200);

        AccountGroup group = new AccountGroup("Business");
        group.add(a);
        group.add(b);

        group.remove(b);

        assertEquals(100, group.getTotalBalance(), 0.001);
    }
}
