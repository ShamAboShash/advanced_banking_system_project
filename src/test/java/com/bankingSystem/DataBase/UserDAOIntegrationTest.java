package com.bankingSystem.DataBase;

import com.bankingSystem.Database.DatabaseConnection;
import com.bankingSystem.Database.UserDAO;
import com.bankingSystem.user.Customer;
import com.bankingSystem.user.User;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOIntegrationTest {
    private UserDAO dao;

    @BeforeEach
    void setup() { dao = new UserDAO(); }

    @BeforeEach
    void cleanUsersTable() throws Exception {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM Users");
        }
    }

    @Test
    void saveAndLoadByPhone_shouldReturnUser() {
        Customer c = new Customer("uX", "pass123", "0990000000");
        c.persist();

        User loaded = dao.loadUserByPhone("0990000000");
        assertNotNull(loaded);
        assertEquals("uX", loaded.getUsername());
    }

    @Test
    void loginByPhoneAndPassword_shouldWork() {
        Customer c = new Customer("uY", "mypw", "0990000001");
        c.persist();

        User logged = dao.loadUserByPhoneAndPassword("0990000001", "mypw");
        assertNotNull(logged);
        assertEquals("uY", logged.getUsername());
    }
}
