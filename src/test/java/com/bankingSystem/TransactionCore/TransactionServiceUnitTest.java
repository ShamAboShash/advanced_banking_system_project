package com.bankingSystem.TransactionCore;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Notification.NotificationEvent;
import com.bankingSystem.Notification.NotificationEventType;
import com.bankingSystem.Notification.Observer;
import com.bankingSystem.Transaction.Transaction;
import com.bankingSystem.Transaction.TransactionService;
import com.bankingSystem.user.Role;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceUnitTest {

    private TransactionService service;
    private TransactionDAO txDao;
    private AccountDAO accDao;

    @BeforeAll
    static void disableDbPersistence() {
        Account.disablePersistenceForTests();
    }

    @BeforeEach
    void setup() {
        service = TransactionService.getInstance();

        txDao = mock(TransactionDAO.class);
        accDao = mock(AccountDAO.class);

        service.setDaosForTests(txDao, accDao);
    }

    // ===== Helper: mock observer that accepts events for a specific role =====
    private static Observer acceptRole(Role role) {
        Observer obs = mock(Observer.class);
        when(obs.supports(any())).thenAnswer(inv -> {
            NotificationEvent e = inv.getArgument(0);
            return e != null && e.getTargetRole() == role;
        });
        return obs;
    }

    // =========================================================
    // 1) Small deposit (happy path)
    // =========================================================
    @Test
    void processTransaction_smallDeposit_shouldComplete_notifyCustomer_andSaveTx() {
        SavingsAccount to = new SavingsAccount("S-100", "owner1");

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(null, to, 100, "DEPOSIT");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(customerObs, timeout(1000).atLeastOnce()).update(cap.capture());

        boolean hasSubmitted = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_SUBMITTED &&
                        "owner1".equals(e.getTargetUserId())
        );

        boolean hasCompleted = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_COMPLETED &&
                        "owner1".equals(e.getTargetUserId())
        );

        assertTrue(hasSubmitted);
        assertTrue(hasCompleted);
        assertEquals(100, to.getBalance(), 0.001);

        service.removeObserver(customerObs);
    }

    // =========================================================
    // 2) Transfer with null target -> validation + failed
    // =========================================================
    @Test
    void processTransaction_transferWithNullTo_shouldFail_notifyValidation_andFailed() {
        SavingsAccount from = new SavingsAccount("S-200", "ownerX");

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(from, null, 10, "TRANSFER");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(customerObs, timeout(1000).atLeastOnce()).update(cap.capture());

        boolean hasValidationFailed = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_VALIDATION_FAILED &&
                        "ownerX".equals(e.getTargetUserId())
        );

        boolean hasFailed = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_FAILED &&
                        "ownerX".equals(e.getTargetUserId())
        );

        assertTrue(hasValidationFailed);
        assertTrue(hasFailed);

        service.removeObserver(customerObs);
    }

    // =========================================================
    // 3) Withdraw causes low balance warning
    // =========================================================
    @Test
    void processTransaction_withdrawLowBalance_shouldSendLowBalanceWarning() {
        SavingsAccount from = new SavingsAccount("S-300", "ownerL");
        from.deposit(60); // after withdraw 20 -> 40 <= threshold => warning

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(from, null, 20, "WITHDRAW");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(customerObs, timeout(1000).atLeastOnce()).update(cap.capture());

        boolean hasLowBalance = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.LOW_BALANCE_WARNING &&
                        "ownerL".equals(e.getTargetUserId())
        );

        assertTrue(hasLowBalance);
        assertEquals(40, from.getBalance(), 0.001);

        service.removeObserver(customerObs);
    }

    // =========================================================
    // 4) Large amount -> manager alert / pending approval
    // =========================================================
    @Test
    void processTransaction_largeAmount_shouldNotifyManager_andSaveTx() {
        SavingsAccount to = new SavingsAccount("S-400", "ownerBig");

        Observer managerObs = acceptRole(Role.MANAGER);
        service.registerObserver(managerObs);

        service.processTransaction(null, to, 6000, "DEPOSIT");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(managerObs, timeout(1000).atLeastOnce()).update(cap.capture());

        boolean hasLargeAlert = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.LARGE_TRANSACTION_ALERT &&
                        e.getTargetRole() == Role.MANAGER
        );

        boolean hasPending = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_PENDING_MANAGER_APPROVAL &&
                        e.getTargetRole() == Role.MANAGER
        );

        assertTrue(hasLargeAlert || hasPending);

        service.removeObserver(managerObs);
    }

    // =========================================================
    // 5) Unknown transaction type (default branch)
    // =========================================================
    @Test
    void processTransaction_unknownType_shouldFail_andSaveTx() {
        SavingsAccount to = new SavingsAccount("S-500", "ownerU");

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(null, to, 10, "INVALID_TYPE");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        verify(customerObs, timeout(1000).atLeastOnce())
                .update(argThat(e ->
                        e.getType() == NotificationEventType.TRANSACTION_FAILED &&
                                "ownerU".equals(e.getTargetUserId())
                ));

        service.removeObserver(customerObs);
    }

    // =========================================================
    // 6) Withdraw insufficient balance (exception path)
    // =========================================================
    @Test
    void processTransaction_withdrawInsufficientBalance_shouldFail() {
        SavingsAccount from = new SavingsAccount("S-600", "ownerZ");
        from.deposit(5);

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(from, null, 50, "WITHDRAW");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        verify(customerObs, timeout(1000).atLeastOnce())
                .update(argThat(e ->
                        e.getType() == NotificationEventType.TRANSACTION_FAILED &&
                                "ownerZ".equals(e.getTargetUserId())
                ));

        service.removeObserver(customerObs);
    }

    // =========================================================
    // 7) Boundary: amount == LARGE_TX_THRESHOLD (>= branch)
    // =========================================================
    @Test
    void processTransaction_depositAtLargeThreshold_shouldTriggerLargeAlert() {
        SavingsAccount to = new SavingsAccount("S-700", "ownerB");

        Observer managerObs = acceptRole(Role.MANAGER);
        service.registerObserver(managerObs);

        service.processTransaction(null, to, 5000, "DEPOSIT");

        verify(managerObs, timeout(1000).atLeastOnce())
                .update(argThat(e ->
                        e.getType() == NotificationEventType.LARGE_TRANSACTION_ALERT &&
                                e.getTargetRole() == Role.MANAGER
                ));

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        service.removeObserver(managerObs);
    }

    // =========================================================
    // 8) WITHDRAW exact balance (edge case) -> completes, low balance warning likely
    // =========================================================
    @Test
    void processTransaction_withdrawExactBalance_shouldComplete_withoutException() {
        SavingsAccount from = new SavingsAccount("S-800", "ownerE");
        from.deposit(50); // exactly threshold

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(from, null, 50, "WITHDRAW");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(customerObs, timeout(1000).atLeastOnce()).update(cap.capture());

        boolean hasCompleted = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_COMPLETED &&
                        "ownerE".equals(e.getTargetUserId())
        );

        assertTrue(hasCompleted);
        assertEquals(0, from.getBalance(), 0.001);

        service.removeObserver(customerObs);
    }

    // =========================================================
    // 9) Tiny deposit (very small) -> auto approve path
    // =========================================================
    @Test
    void processTransaction_tinyDeposit_shouldAutoApprove() {
        SavingsAccount to = new SavingsAccount("S-801", "ownerTiny");

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(null, to, 1, "DEPOSIT");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        verify(customerObs, timeout(1000).atLeastOnce())
                .update(argThat(e ->
                        e.getType() == NotificationEventType.TRANSACTION_COMPLETED &&
                                "ownerTiny".equals(e.getTargetUserId())
                ));

        assertEquals(1, to.getBalance(), 0.001);

        service.removeObserver(customerObs);
    }

    // =========================================================
    // 10) TRANSFER happy path (covers transfer branch fully)
    // =========================================================
    @Test
    void processTransaction_transferHappyPath_shouldCompleteForBothAccounts() {
        SavingsAccount from = new SavingsAccount("S-900", "ownerFrom");
        SavingsAccount to = new SavingsAccount("S-901", "ownerTo");
        from.deposit(200);

        Observer customerObs = acceptRole(Role.CUSTOMER);
        service.registerObserver(customerObs);

        service.processTransaction(from, to, 50, "TRANSFER");

        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(customerObs, timeout(1000).atLeastOnce()).update(cap.capture());

        boolean senderCompleted = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_COMPLETED &&
                        "ownerFrom".equals(e.getTargetUserId())
        );

        boolean receiverCompleted = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_COMPLETED &&
                        "ownerTo".equals(e.getTargetUserId())
        );

        assertTrue(senderCompleted);
        assertTrue(receiverCompleted);

        assertEquals(150, from.getBalance(), 0.001);
        assertEquals(50, to.getBalance(), 0.001);

        service.removeObserver(customerObs);
    }
}
