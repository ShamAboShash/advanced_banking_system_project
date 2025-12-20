package com.bankingSystem.TransactionCore;

import com.bankingSystem.Transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void isPendingManagerApproval_whenStatusPending_shouldReturnTrue() {
        Transaction tx = new Transaction("TRANSFER", 100000, "A", "B");
        tx.setStatus("PENDING_MANAGER_APPROVAL");

        assertTrue(tx.isPendingManagerApproval());
        assertFalse(tx.isApproved());
    }

    @Test
    void isApproved_whenStatusStartsWithApproved_shouldReturnTrue() {
        Transaction tx = new Transaction("TRANSFER", 2000, "A", "B");
        tx.setStatus("APPROVED - Small transaction");

        assertTrue(tx.isApproved());
        assertFalse(tx.isPendingManagerApproval());
    }

    @Test
    void isApproved_whenCompleted_shouldReturnTrue() {
        Transaction tx = new Transaction("DEPOSIT", 50, null, "B");
        tx.setStatus("COMPLETED");

        assertTrue(tx.isApproved());
    }

    @Test
    void transaction_basicConstructorAndGetters_shouldWork() {
        Transaction tx = new Transaction(
                "tx-1",
                "DEPOSIT",
                100,
                LocalDateTime.now(),
                null,
                "ACC-1",
                "COMPLETED",
                "test",
                "user1",
                LocalDateTime.now()
        );

        assertEquals("tx-1", tx.getTransactionId());
        assertEquals("DEPOSIT", tx.getType());
        assertEquals(100, tx.getAmount());
        assertEquals("ACC-1", tx.getToAccount());
        assertEquals("COMPLETED", tx.getStatus());
    }

}
