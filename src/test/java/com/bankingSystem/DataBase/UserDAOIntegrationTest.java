package com.bankingSystem.DataBase;

import com.bankingSystem.Database.DatabaseConnection;
import com.bankingSystem.Database.UserDAO;
import com.bankingSystem.user.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOIntegrationTest {

    private UserDAO dao;
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        dao = new UserDAO();
        conn = DatabaseConnection.getInstance().getConnection();

        // إذا بدك تختبري default users لا تعملي delete هنا
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM Users");
        }
    }

    @Test
    void saveUser_thenLoadUserByUsername_shouldReturn() {
        Customer c = new Customer("u1", "pw", "0991111111");
        c.persist(); // يفترض يستخدم UserDAO.saveUser داخلياً

        User loaded = dao.loadUser("u1");
        assertNotNull(loaded);
        assertEquals("u1", loaded.getUsername());
        assertEquals(Role.CUSTOMER, loaded.getRole());
    }

    @Test
    void saveAndLoadById_shouldReturn() {
        Manager m = new Manager("idM", "m1", "pw", "0992222222");
        dao.saveUser(m);

        User loaded = dao.loadUserById("idM");
        assertNotNull(loaded);
        assertEquals("m1", loaded.getUsername());
        assertEquals(Role.MANAGER, loaded.getRole());
    }

    @Test
    void getAllUsers_shouldReturnAll() {
        dao.saveUser(new Customer("id1", "c1", "pw", "0993000001"));
        dao.saveUser(new Teller("id2", "t1", "pw", "0993000002"));
        dao.saveUser(new Manager("id3", "m1", "pw", "0993000003"));

        List<User> all = dao.getAllUsers();
        assertEquals(3, all.size());
    }

    @Test
    void getAllUsersByRole_shouldFilter() {
        dao.saveUser(new Customer("id1", "c1", "pw", "0994000001"));
        dao.saveUser(new Customer("id2", "c2", "pw", "0994000002"));
        dao.saveUser(new Manager("id3", "m1", "pw", "0994000003"));

        var customers = dao.getAllUsersByRole("Customer");
        assertEquals(2, customers.size());

        var managers = dao.getAllUsersByRole("Manager");
        assertEquals(1, managers.size());
    }

    @Test
    void deleteUser_shouldRemove() {
        dao.saveUser(new Customer("idDel", "cDel", "pw", "0995000001"));
        assertNotNull(dao.loadUser("cDel"));

        dao.deleteUser("idDel");
        assertNull(dao.loadUser("cDel"));
    }

    @Test
    void loadUserByPhone_shouldReturnUser() {
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

    @Test
    void loginByPhoneAndPassword_wrongPassword_shouldReturnNull() {
        Customer c = new Customer("uZ", "correct", "0990000002");
        c.persist();

        User logged = dao.loadUserByPhoneAndPassword("0990000002", "wrong");
        assertNull(logged);
    }

    @Test
    void loadFirstManager_shouldReturnManagerIfExists() {
        dao.saveUser(new Customer("idC", "c1", "pw", "0996000001"));
        dao.saveUser(new Manager("idM", "m1", "pw", "0996000002"));

        User m = dao.loadFirstManager();
        assertNotNull(m);
        assertEquals(Role.MANAGER, m.getRole());
    }

    @Test
    void loadFirstManager_whenNoManager_shouldReturnNull() {
        dao.saveUser(new Customer("idC", "c1", "pw", "0997000001"));
        assertNull(dao.loadFirstManager());
    }
}
