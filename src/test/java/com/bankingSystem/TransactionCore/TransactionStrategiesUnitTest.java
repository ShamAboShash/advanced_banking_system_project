package com.bankingSystem.TransactionCore;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import com.bankingSystem.Transaction.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStrategiesUnitTest {

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @Test
    void depositStrategy_shouldIncreaseBalance_setCompleted_andAddTransaction() {
        SavingsAccount to = new SavingsAccount("S-10", "o1");

        Transaction tx = new Transaction("DEPOSIT", 50, null, "S-10");
        tx.setStatus("PENDING");

        new DepositStrategy().execute(null, to, 50, tx);

        assertEquals(50, to.getBalance(), 0.001);
        assertEquals("COMPLETED", tx.getStatus());
        assertFalse(to.getTransactionHistory().isEmpty());
        assertTrue(to.getTransactionHistory().contains(tx), "Deposit should add the same tx to history");
    }

    @Test
    void withdrawStrategy_shouldDecreaseBalance_setCompleted_andAddTransaction() {
        SavingsAccount from = new SavingsAccount("S-11", "o1");
        from.deposit(100);

        Transaction tx = new Transaction("WITHDRAW", 40, "S-11", null);
        tx.setStatus("PENDING");

        new WithdrawStrategy().execute(from, null, 40, tx);

        assertEquals(60, from.getBalance(), 0.001);
        assertEquals("COMPLETED", tx.getStatus());
        assertFalse(from.getTransactionHistory().isEmpty());
        assertTrue(from.getTransactionHistory().contains(tx), "Withdraw should add the same tx to history");
    }

    @Test
    void transferStrategy_shouldMoveMoney_andCreateInOutTransactions() {
        SavingsAccount from = new SavingsAccount("S-12", "o1");
        SavingsAccount to = new SavingsAccount("S-13", "o2");
        from.deposit(200);

        Transaction main = new Transaction("TRANSFER", 70, "S-12", "S-13");
        main.setStatus("PENDING");

        new TransferStrategy().execute(from, to, 70, main);

        assertEquals(130, from.getBalance(), 0.001);
        assertEquals(70, to.getBalance(), 0.001);
        assertEquals("COMPLETED", main.getStatus());

        // TransferStrategy يعمل معاملات منفصلة TRANSFER_OUT و TRANSFER_IN
        assertTrue(from.getTransactionHistory().stream().anyMatch(t -> "TRANSFER_OUT".equals(t.getType())));
        assertTrue(to.getTransactionHistory().stream().anyMatch(t -> "TRANSFER_IN".equals(t.getType())));
    }

    @Test
    void transferStrategy_insufficientFunds_shouldThrow_andSetRejectedStatus() {
        SavingsAccount from = new SavingsAccount("S-14", "o1");
        SavingsAccount to = new SavingsAccount("S-15", "o2");
        from.deposit(10);

        Transaction main = new Transaction("TRANSFER", 50, "S-14", "S-15");
        main.setStatus("PENDING");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new TransferStrategy().execute(from, to, 50, main)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
        assertTrue(main.getStatus().startsWith("REJECTED"), "Status should start with REJECTED when insufficient funds");
        // وتأكد أنه ما صار تغيير رصيد
        assertEquals(10, from.getBalance(), 0.001);
        assertEquals(0, to.getBalance(), 0.001);
    }
}
