package trading.service;

import trading.dao.AssetDao;
import trading.dao.AssetDaoImpl;
import trading.dao.MarketDao;
import trading.dao.MarketDaoImpl;
import trading.exception.TradingException;
import trading.model.Asset;
import trading.model.Market;
import trading.model.OrderBook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketService {
    private static MarketService instance;

    private final AssetDao assetDao;
    private final MarketDao marketDao;
    private final Map<String, OrderBook> orderBooks = new HashMap<>();

    private MarketService(AssetDao assetDao, MarketDao marketDao) {
        this.assetDao = assetDao;
        this.marketDao = marketDao;
    }

    public static synchronized MarketService getInstance(AssetDao assetDao, MarketDao marketDao) {
        if (instance == null) {
            instance = new MarketService(assetDao, marketDao);
        }
        return instance;
    }

    public static synchronized MarketService getInstance() {
        if (instance == null) {
            AssetDao ad = new AssetDaoImpl();
            instance = new MarketService(ad, new MarketDaoImpl(ad));
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public AssetDao getAssetDao() {
        return assetDao;
    }

    public MarketDao getMarketDao() {
        return marketDao;
    }

    public Market addMarket(String symbol, Asset baseAsset, String quoteAsset, double initialPrice) {
        if (marketDao.findBySymbol(symbol).isPresent()) {
            throw new TradingException("Market '" + symbol + "' already exists.");
        }
        if (initialPrice <= 0) {
            throw new TradingException("Initial price must be positive.");
        }
        if (assetDao.findBySymbol(baseAsset.getSymbol()).isEmpty()) {
            assetDao.create(baseAsset);
        }
        Market market = new Market(symbol, baseAsset, quoteAsset, initialPrice);
        marketDao.create(market);
        orderBooks.put(symbol, new OrderBook(market));
        AuditService.getInstance().log("ADD_MARKET");
        return market;
    }

    public Market getMarket(String symbol) {
        return marketDao.findBySymbol(symbol)
                .orElseThrow(() -> new TradingException("Market '" + symbol + "' not found."));
    }

    public Map<String, Market> getAllMarkets() {
        Map<String, Market> map = new HashMap<>();
        for (Market m : marketDao.findAll()) {
            map.put(m.getSymbol(), m);
        }
        return map;
    }

    public OrderBook getOrderBook(String symbol) {
        OrderBook ob = orderBooks.get(symbol);
        if (ob == null) {
            Market market = getMarket(symbol);
            ob = new OrderBook(market);
            orderBooks.put(symbol, ob);
        }
        return ob;
    }

    public void updateMarket(Market market) {
        marketDao.update(market);
    }

    public void printMarketSummary() {
        List<Market> markets = marketDao.findAll();
        if (markets.isEmpty()) {
            System.out.println("No markets available.");
            return;
        }
        System.out.println("=== Market Summary ===");
        for (Market m : markets) {
            System.out.println(m.toPrettyString());
        }
        AuditService.getInstance().log("MARKET_SUMMARY");
    }
}
