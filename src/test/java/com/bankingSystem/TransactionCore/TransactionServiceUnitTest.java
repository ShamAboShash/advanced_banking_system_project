package com.bankingSystem.TransactionCore;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Account.SavingsAccount;
import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Notification.*;
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

    @Test
    void processTransaction_smallAmount_shouldComplete_notifyCustomer_saveTx() {
        SavingsAccount to = new SavingsAccount("S-100", "owner1");

        Observer customerObs = mock(Observer.class);
        when(customerObs.supports(argThat(e -> e != null && e.getTargetRole() == Role.CUSTOMER)))
                .thenReturn(true);

        service.registerObserver(customerObs);

        service.processTransaction(null, to, 100, "DEPOSIT");

        // ✅ saved
        verify(txDao, times(1)).saveTransaction(any(Transaction.class));

        // ✅ notified
        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(customerObs, atLeastOnce()).update(cap.capture());

        boolean hasCompleted = cap.getAllValues().stream().anyMatch(e ->
                e.getType() == NotificationEventType.TRANSACTION_COMPLETED &&
                        e.getTargetRole() == Role.CUSTOMER
        );

        assertTrue(hasCompleted);
        assertEquals(100, to.getBalance(), 0.001);
    }
}
