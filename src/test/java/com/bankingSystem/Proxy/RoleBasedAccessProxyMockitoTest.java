package com.bankingSystem.Proxy;

import com.bankingSystem.Account.SavingsAccount;
import com.bankingSystem.user.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleBasedAccessProxyMockitoTest {

    // ========== Helpers ==========
    private static SavingsAccount accOwnedBy(String accNo, String ownerId) {
        return new SavingsAccount(accNo, ownerId);
    }

    // ========== Existing tests (yours) ==========

    @Test
    void customer_createAccount_shouldThrow_andNotCallRealService() {
        Customer customer = new Customer("c1", "pass", "099");

        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(customer, mockService);

        assertThrows(SecurityException.class, () ->
                proxy.createAccount("savings", customer.getUserId())
        );

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

    // ========== New tests (to raise branch coverage fast) ==========

    // ---- requireRole(TELLER) branches ----

    @Test
    void teller_createAccount_shouldDelegate() {
        Teller teller = new Teller("t1", "pass", "077");
        BankingService mockService = mock(BankingService.class);

        BankingService proxy = new RoleBasedAccessProxy(teller, mockService);

        proxy.createAccount("savings", "ownerX");

        verify(mockService, times(1)).createAccount("savings", "ownerX");
    }

    @Test
    void teller_changeAccountState_shouldDelegate() {
        Teller teller = new Teller("t1", "pass", "077");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(teller, mockService);

        proxy.changeAccountState("ACC-1", "frozen");

        verify(mockService).changeAccountState("ACC-1", "frozen");
    }

    @Test
    void customer_findAccount_shouldThrow_andNotCallRealService() {
        Customer customer = new Customer("c1", "pass", "099");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(customer, mockService);

        assertThrows(SecurityException.class, () -> proxy.findAccountByNumber("ACC-1"));

        verify(mockService, never()).findAccountByNumber(anyString());
    }

    // ---- requireRole(MANAGER) branches ----

    @Test
    void manager_addTeller_shouldDelegate() {
        Manager manager = new Manager("m1", "pass", "088");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(manager, mockService);

        proxy.addTeller("newT", "pw", "0900000000");

        verify(mockService).addTeller("newT", "pw", "0900000000");
    }

    @Test
    void teller_getAllCustomers_shouldThrow_andNotCallRealService() {
        Teller teller = new Teller("t1", "pass", "077");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(teller, mockService);

        assertThrows(SecurityException.class, proxy::getAllCustomers);

        verify(mockService, never()).getAllCustomers();
    }

    // ---- ADMIN bypass branch in requireRole ----

    @Test
    void admin_shouldBypassRequireRole_andDelegateManagerCalls() {
        Admin admin = new Admin("a1", "pass", "066");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(admin, mockService);

        proxy.getAllAccountsReport();
        verify(mockService, times(1)).getAllAccountsReport();

        proxy.createAccount("checking", "ownerZ");
        verify(mockService, times(1)).createAccount("checking", "ownerZ");
    }

    // ---- CUSTOMER ownership checks (branches inside performTransaction/history/modify) ----

    @Test
    void customer_performTransaction_otherOwner_shouldThrow_andNotDelegate() {
        Customer customer = new Customer("c1", "pass", "099");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(customer, mockService);

        var from = accOwnedBy("S-1", "c1");
        var to = accOwnedBy("S-2", "c1");

        assertThrows(SecurityException.class, () ->
                proxy.performTransaction("someone-else", from, to, 50, "TRANSFER")
        );

        verify(mockService, never()).performTransaction(anyString(), any(), any(), anyDouble(), anyString());
    }

    @Test
    void customer_performTransaction_ownOwner_shouldDelegate() {
        Customer customer = new Customer("c1", "pass", "099");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(customer, mockService);

        var from = new SavingsAccount("S-1", customer.getUserId());
        var to = new SavingsAccount("S-2", customer.getUserId());

        proxy.performTransaction(customer.getUserId(), from, to, 50, "TRANSFER");

        verify(mockService, times(1))
                .performTransaction(eq(customer.getUserId()), any(), any(), eq(50.0), eq("TRANSFER"));
    }


    @Test
    void customer_getTransactionHistory_otherOwner_shouldThrow_andNotDelegate() {
        Customer customer = new Customer("c1", "pass", "099");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(customer, mockService);

        assertThrows(SecurityException.class, () ->
                proxy.getTransactionHistory("someone-else", "ACC-1")
        );

        verify(mockService, never()).getTransactionHistory(anyString(), anyString());
    }

    @Test
    void customer_modifyAccountInfo_otherOwner_shouldThrow_andNotDelegate() {
        Customer customer = new Customer("c1", "pass", "099");
        BankingService mockService = mock(BankingService.class);
        BankingService proxy = new RoleBasedAccessProxy(customer, mockService);

        var acc = accOwnedBy("S-1", "c1");

        assertThrows(SecurityException.class, () ->
                proxy.modifyAccountInfo("someone-else", acc, "ownerId", "x")
        );

        verify(mockService, never()).modifyAccountInfo(anyString(), any(), anyString(), anyString());
    }
}
