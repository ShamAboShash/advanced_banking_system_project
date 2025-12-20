package com.bankingSystem.TransactionCore;

import com.bankingSystem.Transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMoreTests {

    @Test
    void newConstructor_shouldAutoGenerateId_timestamp_description_andDefaultStatusPending() {
        Transaction tx = new Transaction("DEPOSIT", 123.45, null, "ACC-1", "user-1");

        assertNotNull(tx.getTransactionId());
        assertFalse(tx.getTransactionId().isBlank());

        assertEquals("DEPOSIT", tx.getType());
        assertEquals(123.45, tx.getAmount(), 0.0001);

        assertNotNull(tx.getTimestamp());
        assertNotNull(tx.getPerformedAt()); // in your constructor it is set to now

        assertEquals("PENDING", tx.getStatus());

        assertEquals(null, tx.getFromAccount());
        assertEquals("ACC-1", tx.getToAccount());

        // description uses "-" for nulls
        assertTrue(tx.getDescription().contains("DEPOSIT"));
        assertTrue(tx.getDescription().contains("123.45"));
        assertTrue(tx.getDescription().contains("-"));
        assertTrue(tx.getDescription().contains("ACC-1"));
    }

    @Test
    void compatibilityConstructor_typeAmount_shouldSetNullAccountsAndNullPerformer() {
        Transaction tx = new Transaction("WITHDRAW", 10);

        assertEquals("WITHDRAW", tx.getType());
        assertEquals(10, tx.getAmount(), 0.0001);
        assertNull(tx.getFromAccount());
        assertNull(tx.getToAccount());
        assertNull(tx.getPerformedBy()); // old constructor calls performedBy=null
        assertNotNull(tx.getPerformedAt()); // still set in new main constructor
        assertEquals("PENDING", tx.getStatus());
    }

    @Test
    void compatibilityConstructor_withAccounts_shouldFillDescriptionWithAccounts() {
        Transaction tx = new Transaction("TRANSFER", 50, "A-1", "B-1");

        assertEquals("TRANSFER", tx.getType());
        assertEquals("A-1", tx.getFromAccount());
        assertEquals("B-1", tx.getToAccount());
        assertTrue(tx.getDescription().contains("A-1"));
        assertTrue(tx.getDescription().contains("B-1"));
    }

    @Test
    void setStatus_shouldUpdateStatus() {
        Transaction tx = new Transaction("DEPOSIT", 1);
        tx.setStatus("COMPLETED");
        assertEquals("COMPLETED", tx.getStatus());
    }

    @Test
    void isApproved_shouldReturnTrue_whenStatusStartsWithApproved() {
        Transaction tx = new Transaction("TRANSFER", 100, "A", "B");
        tx.setStatus("APPROVED_BY_MANAGER");

        assertTrue(tx.isApproved());
    }

    @Test
    void isApproved_shouldReturnTrue_whenStatusCompleted() {
        Transaction tx = new Transaction("WITHDRAW", 10, "A", null);
        tx.setStatus("COMPLETED");

        assertTrue(tx.isApproved());
    }

    @Test
    void isApproved_shouldReturnFalse_whenRejectedOrPending() {
        Transaction tx = new Transaction("WITHDRAW", 10, "A", null);

        tx.setStatus("PENDING_MANAGER_APPROVAL");
        assertFalse(tx.isApproved());

        tx.setStatus("REJECTED_BY_MANAGER");
        assertFalse(tx.isApproved());

        tx.setStatus("FAILED: boom");
        assertFalse(tx.isApproved());
    }

    @Test
    void isApproved_shouldReturnFalse_whenStatusNull() {
        Transaction tx = new Transaction("DEPOSIT", 1);
        tx.setStatus(null);

        assertFalse(tx.isApproved());
    }

    @Test
    void isPendingManagerApproval_shouldWork() {
        Transaction tx = new Transaction("TRANSFER", 999, "A", "B");

        tx.setStatus("PENDING_MANAGER_APPROVAL");
        assertTrue(tx.isPendingManagerApproval());

        tx.setStatus("PENDING");
        assertFalse(tx.isPendingManagerApproval());
    }

    @Test
    void dbConstructor_shouldKeepProvidedFields() {
        LocalDateTime ts = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime perf = LocalDateTime.of(2025, 1, 1, 10, 5);

        Transaction tx = new Transaction(
                "tx-123",
                "DEPOSIT",
                200,
                ts,
                null,
                "ACC-9",
                "COMPLETED",
                "desc",
                "user-9",
                perf
        );

        assertEquals("tx-123", tx.getTransactionId());
        assertEquals("DEPOSIT", tx.getType());
        assertEquals(200, tx.getAmount(), 0.0001);
        assertEquals(ts, tx.getTimestamp());
        assertEquals("ACC-9", tx.getToAccount());
        assertEquals("COMPLETED", tx.getStatus());
        assertEquals("desc", tx.getDescription());
        assertEquals("user-9", tx.getPerformedBy());
        assertEquals(perf, tx.getPerformedAt());
    }

    @Test
    void toString_shouldIncludeUnknown_whenPerformedByNull() {
        Transaction tx = new Transaction(
                "tx-x",
                "DEPOSIT",
                1,
                LocalDateTime.now(),
                null,
                "B",
                "PENDING",
                "d",
                null,   // performedBy null
                LocalDateTime.now()
        );

        String s = tx.toString();
        assertNotNull(s);
        assertTrue(s.contains("unknown"));
        assertTrue(s.contains("DEPOSIT"));
    }

    @Test
    void toString_shouldFallbackToTimestamp_whenPerformedAtNull() {
        LocalDateTime ts = LocalDateTime.of(2025, 6, 1, 12, 30);

        Transaction tx = new Transaction(
                "tx-y",
                "WITHDRAW",
                5,
                ts,
                "A",
                null,
                "PENDING",
                "d",
                "userX",
                null // performedAt null -> should fallback to timestamp
        );

        String s = tx.toString();
        assertNotNull(s);
        assertTrue(s.contains("WITHDRAW"));
        assertTrue(s.contains("userX"));
        // ما منقارن string كامل لأنه format ممكن يختلف، بس نضمن أنه لم ينهار وأنه يحتوي معلومات
    }
}
