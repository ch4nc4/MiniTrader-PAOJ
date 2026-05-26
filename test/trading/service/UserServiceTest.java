package trading.service;

import org.junit.jupiter.api.*;
import trading.dao.UserDao;
import trading.dao.UserDaoImpl;
import trading.database.DatabaseConnection;
import trading.exception.TradingException;
import trading.model.Portfolio;
import trading.model.User;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private UserService userService;

    @BeforeEach
    void setUp() {
        UserService.resetInstance();
        MarketService.resetInstance();
        TradingService.resetInstance();
        DatabaseConnection.getInstance("jdbc:sqlite::memory:");
        UserDao userDao = new UserDaoImpl();
        userService = UserService.getInstance(userDao);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.getInstance().close();
        UserService.resetInstance();
    }

    @Test
    void testRegisterUser() {
        User user = userService.registerUser("alice", "alice@test.com");
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        assertEquals("alice@test.com", user.getEmail());
    }

    @Test
    void testRegisterDuplicateUsername() {
        userService.registerUser("bob", "bob@test.com");
        TradingException ex = assertThrows(TradingException.class,
                () -> userService.registerUser("bob", "bob2@test.com"));
        assertTrue(ex.getMessage().contains("already taken"));
    }

    @Test
    void testRegisterEmptyUsername() {
        assertThrows(TradingException.class,
                () -> userService.registerUser("", "empty@test.com"));
    }

    @Test
    void testRegisterInvalidEmail() {
        assertThrows(TradingException.class,
                () -> userService.registerUser("charlie", "invalid-email"));
    }

    @Test
    void testDepositAndGetBalance() {
        User user = userService.registerUser("dave", "dave@test.com");
        userService.deposit(user, "USDT", 10000.0);
        Portfolio portfolio = userService.getPortfolio(user);
        assertEquals(10000.0, portfolio.getBalance("USDT"), 0.001);
    }

    @Test
    void testWithdrawSuccess() {
        User user = userService.registerUser("eve", "eve@test.com");
        userService.deposit(user, "USDT", 5000.0);
        userService.withdraw(user, "USDT", 2000.0);
        Portfolio portfolio = userService.getPortfolio(user);
        assertEquals(3000.0, portfolio.getBalance("USDT"), 0.001);
    }

    @Test
    void testWithdrawInsufficientFunds() {
        User user = userService.registerUser("frank", "frank@test.com");
        userService.deposit(user, "USDT", 100.0);
        assertThrows(TradingException.class,
                () -> userService.withdraw(user, "USDT", 500.0));
    }

    @Test
    void testGetUserByUsername() {
        userService.registerUser("grace", "grace@test.com");
        User found = userService.getUserByUsername("grace");
        assertEquals("grace", found.getUsername());
    }

    @Test
    void testGetNonexistentUser() {
        assertThrows(TradingException.class,
                () -> userService.getUserByUsername("nobody"));
    }
}
