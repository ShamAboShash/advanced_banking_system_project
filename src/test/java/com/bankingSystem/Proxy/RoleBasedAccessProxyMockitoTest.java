package com.bankingSystem.Proxy;

import com.bankingSystem.user.Customer;
import com.bankingSystem.user.Manager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleBasedAccessProxyMockitoTest {

    @Test
    void customer_createAccount_shouldThrow_andNotCallRealService() {
        Customer customer = new Customer("c1", "pass", "099");

        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(customer, mockService);

        assertThrows(SecurityException.class, () ->
                proxy.createAccount("savings", customer.getUserId())
        );

        // أهم سطر: إثبات إن الخدمة الحقيقية لم تُستدعى
        verify(mockService, never()).createAccount(anyString(), anyString());
    }

    @Test
    void manager_getAllTransactions_shouldDelegateToService() {
        Manager manager = new Manager("m1", "pass", "088");

        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(manager, mockService);

        proxy.getAllTransactionsReport();

        verify(mockService, times(1)).getAllTransactionsReport();
    }
}
