package trading.service;

import org.junit.jupiter.api.*;
import trading.dao.*;
import trading.database.DatabaseConnection;
import trading.exception.TradingException;
import trading.model.Market;
import trading.model.SpotAsset;

import static org.junit.jupiter.api.Assertions.*;

public class MarketServiceTest {
    private MarketService marketService;

    @BeforeEach
    void setUp() {
        UserService.resetInstance();
        MarketService.resetInstance();
        TradingService.resetInstance();
        DatabaseConnection.getInstance("jdbc:sqlite::memory:");
        AssetDao assetDao = new AssetDaoImpl();
        MarketDao marketDao = new MarketDaoImpl(assetDao);
        marketService = MarketService.getInstance(assetDao, marketDao);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.getInstance().close();
        MarketService.resetInstance();
    }

    @Test
    void testAddMarket() {
        SpotAsset btc = new SpotAsset("BTC", "Bitcoin", 21_000_000);
        Market market = marketService.addMarket("BTC/USDT", btc, "USDT", 67000.0);
        assertNotNull(market);
        assertEquals("BTC/USDT", market.getSymbol());
        assertEquals(67000.0, market.getLastPrice(), 0.01);
    }

    @Test
    void testGetNonexistentMarket() {
        assertThrows(TradingException.class,
                () -> marketService.getMarket("FAKE/USDT"));
    }

    @Test
    void testAddDuplicateMarket() {
        SpotAsset eth = new SpotAsset("ETH", "Ethereum", 120_000_000);
        marketService.addMarket("ETH/USDT", eth, "USDT", 3500.0);
        assertThrows(TradingException.class,
                () -> marketService.addMarket("ETH/USDT", eth, "USDT", 3500.0));
    }

    @Test
    void testInvalidInitialPrice() {
        SpotAsset sol = new SpotAsset("SOL", "Solana", 500_000_000);
        assertThrows(TradingException.class,
                () -> marketService.addMarket("SOL/USDT", sol, "USDT", -10.0));
    }
}
