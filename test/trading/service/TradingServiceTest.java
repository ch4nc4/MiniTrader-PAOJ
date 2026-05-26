package trading.service;

import org.junit.jupiter.api.*;
import trading.dao.*;
import trading.database.DatabaseConnection;
import trading.exception.TradingException;
import trading.model.*;
import trading.model.enums.OrderSide;
import trading.model.enums.OrderStatus;
import trading.model.enums.PositionSide;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TradingServiceTest {
    private UserService userService;
    private MarketService marketService;
    private TradingService tradingService;

    @BeforeEach
    void setUp() {
        UserService.resetInstance();
        MarketService.resetInstance();
        TradingService.resetInstance();
        DatabaseConnection.getInstance("jdbc:sqlite::memory:");

        UserDao userDao = new UserDaoImpl();
        AssetDao assetDao = new AssetDaoImpl();
        MarketDao marketDao = new MarketDaoImpl(assetDao);
        OrderDao orderDao = new OrderDaoImpl(userDao, marketDao);
        TradeDao tradeDao = new TradeDaoImpl(userDao, marketDao);

        userService = UserService.getInstance(userDao);
        marketService = MarketService.getInstance(assetDao, marketDao);
        tradingService = TradingService.getInstance(userService, marketService, orderDao, tradeDao);

        SpotAsset btc = new SpotAsset("BTC", "Bitcoin", 21_000_000);
        marketService.addMarket("BTC/USDT", btc, "USDT", 67000.0);

        PerpAsset btcPerp = new PerpAsset("BTC-PERP", "Bitcoin Perpetual", 0.0001, 50);
        marketService.addMarket("BTC-PERP/USDT", btcPerp, "USDT", 67000.0);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.getInstance().close();
        UserService.resetInstance();
        MarketService.resetInstance();
        TradingService.resetInstance();
    }

    @Test
    void testPlaceSpotOrder() {
        User alice = userService.registerUser("alice", "alice@test.com");
        userService.deposit(alice, "USDT", 100000.0);

        SpotOrder order = tradingService.placeSpotOrder(alice, "BTC/USDT", OrderSide.BUY, 67000.0, 1.0);
        assertNotNull(order);
        assertEquals(OrderStatus.OPEN, order.getStatus());
    }

    @Test
    void testSpotOrderMatching() {
        User alice = userService.registerUser("alice", "alice@test.com");
        User bob = userService.registerUser("bob", "bob@test.com");
        userService.deposit(alice, "USDT", 100000.0);
        userService.deposit(bob, "BTC", 2.0);

        tradingService.placeSpotOrder(alice, "BTC/USDT", OrderSide.BUY, 67000.0, 1.0);
        tradingService.placeSpotOrder(bob, "BTC/USDT", OrderSide.SELL, 67000.0, 1.0);

        List<Trade> trades = tradingService.getAllTrades();
        assertEquals(1, trades.size());
        assertEquals(67000.0, trades.get(0).getPrice(), 0.01);
    }

    @Test
    void testCancelOrder() {
        User alice = userService.registerUser("alice", "alice@test.com");
        userService.deposit(alice, "USDT", 100000.0);

        SpotOrder order = tradingService.placeSpotOrder(alice, "BTC/USDT", OrderSide.BUY, 60000.0, 0.5);
        tradingService.cancelOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void testCancelAlreadyFilledOrder() {
        User alice = userService.registerUser("alice", "alice@test.com");
        User bob = userService.registerUser("bob", "bob@test.com");
        userService.deposit(alice, "USDT", 100000.0);
        userService.deposit(bob, "BTC", 2.0);

        SpotOrder buyOrder = tradingService.placeSpotOrder(alice, "BTC/USDT", OrderSide.BUY, 67000.0, 1.0);
        tradingService.placeSpotOrder(bob, "BTC/USDT", OrderSide.SELL, 67000.0, 1.0);

        assertThrows(TradingException.class, () -> tradingService.cancelOrder(buyOrder.getId()));
    }

    @Test
    void testInsufficientBalance() {
        User alice = userService.registerUser("alice", "alice@test.com");
        userService.deposit(alice, "USDT", 100.0);

        assertThrows(TradingException.class,
                () -> tradingService.placeSpotOrder(alice, "BTC/USDT", OrderSide.BUY, 67000.0, 1.0));
    }

    @Test
    void testPerpOrderCreatesPosition() {
        User alice = userService.registerUser("alice", "alice@test.com");
        User bob = userService.registerUser("bob", "bob@test.com");
        userService.deposit(alice, "USDT", 100000.0);
        userService.deposit(bob, "USDT", 100000.0);

        tradingService.placePerpOrder(alice, "BTC-PERP/USDT", OrderSide.BUY, 67000.0, 0.1,
                10, PositionSide.LONG);
        tradingService.placePerpOrder(bob, "BTC-PERP/USDT", OrderSide.SELL, 67000.0, 0.1,
                10, PositionSide.SHORT);

        Portfolio alicePortfolio = userService.getPortfolio(alice);
        assertEquals(1, alicePortfolio.getOpenPositions().size());
        assertEquals(PositionSide.LONG, alicePortfolio.getOpenPositions().get(0).getSide());
    }

    @Test
    void testFilterTradesByMarket() {
        User alice = userService.registerUser("alice", "alice@test.com");
        User bob = userService.registerUser("bob", "bob@test.com");
        userService.deposit(alice, "USDT", 200000.0);
        userService.deposit(bob, "BTC", 5.0);

        tradingService.placeSpotOrder(alice, "BTC/USDT", OrderSide.BUY, 67000.0, 1.0);
        tradingService.placeSpotOrder(bob, "BTC/USDT", OrderSide.SELL, 67000.0, 1.0);

        List<Trade> filtered = tradingService.filterTradesByMarket("BTC/USDT");
        assertEquals(1, filtered.size());

        List<Trade> empty = tradingService.filterTradesByMarket("BTC-PERP/USDT");
        assertTrue(empty.isEmpty());
    }
}
