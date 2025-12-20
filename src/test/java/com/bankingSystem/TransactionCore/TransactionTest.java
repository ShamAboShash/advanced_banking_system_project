package com.bankingSystem.TransactionCore;

import com.bankingSystem.Transaction.Transaction;
import org.junit.jupiter.api.Test;

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


}
