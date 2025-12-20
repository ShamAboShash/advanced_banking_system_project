package com.bankingSystem.Transaction.ChainOfResponsability;

import com.bankingSystem.Transaction.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalChainTest {

    private ApprovalHandler buildChain() {
        ApprovalHandler small = new SmallTransactionHandler();
        ApprovalHandler large = new LargeTransactionHandler();
        ApprovalHandler manager = new ManagerApprovalHandler();

        small.setNext(large);
        large.setNext(manager);

        return small;
    }

    @Test
    void smallTransaction_shouldBeApprovedAutomatically() {
        Transaction tx = new Transaction("TRANSFER", 1000, "A", "B");

        buildChain().handle(tx);

        assertTrue(tx.getStatus().startsWith("APPROVED"));
    }

    @Test
    void largeTransaction_shouldBeApprovedByTeller() {
        Transaction tx = new Transaction("TRANSFER", 20000, "A", "B");

        buildChain().handle(tx);

        assertTrue(tx.getStatus().startsWith("APPROVED"));
    }

    @Test
    void hugeTransaction_shouldBePendingManagerApproval() {
        Transaction tx = new Transaction("TRANSFER", 100000, "A", "B");

        buildChain().handle(tx);

        assertEquals("PENDING_MANAGER_APPROVAL", tx.getStatus());
    }
}
