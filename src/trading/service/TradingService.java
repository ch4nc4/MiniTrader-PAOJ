package trading.service;

import trading.dao.OrderDao;
import trading.dao.TradeDao;
import trading.exception.TradingException;
import trading.model.*;
import trading.model.enums.OrderSide;
import trading.model.enums.OrderStatus;
import trading.model.enums.PositionSide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TradingService {
    private static TradingService instance;

    private final UserService userService;
    private final MarketService marketService;
    private final OrderDao orderDao;
    private final TradeDao tradeDao;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Trade> allTrades = new ArrayList<>();

    private TradingService(UserService userService, MarketService marketService,
                           OrderDao orderDao, TradeDao tradeDao) {
        this.userService = userService;
        this.marketService = marketService;
        this.orderDao = orderDao;
        this.tradeDao = tradeDao;
    }

    public static synchronized TradingService getInstance(UserService userService,
                                                          MarketService marketService,
                                                          OrderDao orderDao,
                                                          TradeDao tradeDao) {
        if (instance == null) {
            instance = new TradingService(userService, marketService, orderDao, tradeDao);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public void loadFromDatabase() {
        List<Order> savedOrders = orderDao.findAll();
        allOrders.addAll(savedOrders);
        for (Order order : savedOrders) {
            if (order.getStatus() == OrderStatus.OPEN) {
                OrderBook ob = marketService.getOrderBook(order.getMarket().getSymbol());
                ob.addOrder(order);
            }
        }

        List<Trade> savedTrades = tradeDao.findAll();
        allTrades.addAll(savedTrades);
    }

    public SpotOrder placeSpotOrder(User user, String marketSymbol, OrderSide side,
                                    double price, double quantity) {
        if (price <= 0 || quantity <= 0) {
            throw new TradingException("Price and quantity must be positive.");
        }
        Market market = marketService.getMarket(marketSymbol);
        Portfolio portfolio = userService.getPortfolio(user);

        if (side == OrderSide.BUY) {
            double cost = price * quantity;
            if (portfolio.getBalance(market.getQuoteAsset()) < cost) {
                throw new TradingException("Insufficient " + market.getQuoteAsset() + " balance for this order.");
            }
        } else {
            if (portfolio.getBalance(market.getBaseAsset().getSymbol()) < quantity) {
                throw new TradingException("Insufficient " + market.getBaseAsset().getSymbol() + " balance for this order.");
            }
        }

        SpotOrder order = new SpotOrder(user, market, side, price, quantity);
        allOrders.add(order);
        orderDao.create(order);

        OrderBook ob = marketService.getOrderBook(marketSymbol);
        tryMatchOrder(order, ob);

        AuditService.getInstance().log("PLACE_SPOT_ORDER");
        return order;
    }

    public PerpOrder placePerpOrder(User user, String marketSymbol, OrderSide side,
                                    double price, double quantity, int leverage,
                                    PositionSide positionSide) {
        if (price <= 0 || quantity <= 0) {
            throw new TradingException("Price and quantity must be positive.");
        }
        if (leverage < 1 || leverage > 100) {
            throw new TradingException("Leverage must be between 1 and 100.");
        }
        Market market = marketService.getMarket(marketSymbol);
        PerpAsset perpAsset = (PerpAsset) market.getBaseAsset();
        if (leverage > perpAsset.getMaxLeverage()) {
            throw new TradingException("Leverage exceeds max allowed (" + perpAsset.getMaxLeverage() + "x) for " + marketSymbol);
        }

        Portfolio portfolio = userService.getPortfolio(user);
        double margin = (price * quantity) / leverage;
        if (portfolio.getBalance(market.getQuoteAsset()) < margin) {
            throw new TradingException("Insufficient margin. Need " + String.format("%.2f", margin) + " " + market.getQuoteAsset());
        }

        PerpOrder order = new PerpOrder(user, market, side, price, quantity, leverage, positionSide);
        allOrders.add(order);
        orderDao.create(order);

        OrderBook ob = marketService.getOrderBook(marketSymbol);
        tryMatchOrder(order, ob);

        AuditService.getInstance().log("PLACE_PERP_ORDER");
        return order;
    }

    private void tryMatchOrder(Order incoming, OrderBook ob) {
        if (incoming.getSide() == OrderSide.BUY) {
            Order bestSell = ob.getBestSell();
            if (bestSell != null && bestSell.getPrice() <= incoming.getPrice()) {
                executeTrade(incoming, bestSell);
                ob.removeOrder(bestSell);
                return;
            }
        } else {
            Order bestBuy = ob.getBestBuy();
            if (bestBuy != null && bestBuy.getPrice() >= incoming.getPrice()) {
                executeTrade(bestBuy, incoming);
                ob.removeOrder(bestBuy);
                return;
            }
        }
        ob.addOrder(incoming);
    }

    private void executeTrade(Order buyOrder, Order sellOrder) {
        double tradePrice = sellOrder.getPrice();
        double tradeQty = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());

        Market market = buyOrder.getMarket();
        Trade trade = new Trade(buyOrder.getUser(), sellOrder.getUser(), market, tradePrice, tradeQty);
        allTrades.add(trade);
        tradeDao.create(trade);

        buyOrder.setStatus(OrderStatus.FILLED);
        sellOrder.setStatus(OrderStatus.FILLED);
        orderDao.updateStatus(buyOrder.getId(), OrderStatus.FILLED.name());
        orderDao.updateStatus(sellOrder.getId(), OrderStatus.FILLED.name());

        market.setLastPrice(tradePrice);
        market.addVolume(tradeQty * tradePrice);
        marketService.updateMarket(market);

        Portfolio buyerPortfolio = userService.getPortfolio(buyOrder.getUser());
        Portfolio sellerPortfolio = userService.getPortfolio(sellOrder.getUser());

        if (buyOrder instanceof SpotOrder) {
            buyerPortfolio.withdraw(market.getQuoteAsset(), tradePrice * tradeQty);
            buyerPortfolio.deposit(market.getBaseAsset().getSymbol(), tradeQty);
            sellerPortfolio.withdraw(market.getBaseAsset().getSymbol(), tradeQty);
            sellerPortfolio.deposit(market.getQuoteAsset(), tradePrice * tradeQty);
        } else if (buyOrder instanceof PerpOrder) {
            PerpOrder perpBuy = (PerpOrder) buyOrder;
            PerpOrder perpSell = (PerpOrder) sellOrder;
            double buyMargin = (tradePrice * tradeQty) / perpBuy.getLeverage();
            double sellMargin = (tradePrice * tradeQty) / perpSell.getLeverage();
            buyerPortfolio.withdraw(market.getQuoteAsset(), buyMargin);
            sellerPortfolio.withdraw(market.getQuoteAsset(), sellMargin);

            Position longPos = new Position(buyOrder.getUser(), market, PositionSide.LONG,
                    tradePrice, tradeQty, perpBuy.getLeverage());
            Position shortPos = new Position(sellOrder.getUser(), market, PositionSide.SHORT,
                    tradePrice, tradeQty, perpSell.getLeverage());
            buyerPortfolio.addPosition(longPos);
            sellerPortfolio.addPosition(shortPos);
        }

        userService.persistBalances(buyOrder.getUser());
        userService.persistBalances(sellOrder.getUser());

        System.out.println("Trade executed: " + trade.toPrettyString());
    }

    public void cancelOrder(int orderId) {
        for (Order order : allOrders) {
            if (order.getId() == orderId) {
                if (order.getStatus() != OrderStatus.OPEN) {
                    throw new TradingException("Order " + orderId + " is not open (status: " + order.getStatus() + ").");
                }
                order.setStatus(OrderStatus.CANCELLED);
                orderDao.updateStatus(orderId, OrderStatus.CANCELLED.name());
                OrderBook ob = marketService.getOrderBook(order.getMarket().getSymbol());
                ob.removeOrder(order);
                System.out.println("Order " + orderId + " cancelled.");
                AuditService.getInstance().log("CANCEL_ORDER");
                return;
            }
        }
        throw new TradingException("Order " + orderId + " not found.");
    }

    public List<Order> getOpenOrders(User user) {
        List<Order> result = new ArrayList<>();
        for (Order o : allOrders) {
            if (o.getUser().equals(user) && o.getStatus() == OrderStatus.OPEN) {
                result.add(o);
            }
        }
        return result;
    }

    public List<Trade> getTradeHistory(User user) {
        List<Trade> result = new ArrayList<>();
        for (Trade t : allTrades) {
            if (t.getBuyer().equals(user) || t.getSeller().equals(user)) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Trade> filterTradesByMarket(String marketSymbol) {
        List<Trade> result = new ArrayList<>();
        for (Trade t : allTrades) {
            if (t.getMarket().getSymbol().equalsIgnoreCase(marketSymbol)) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Order> getOrdersSortedByPrice(User user) {
        List<Order> userOrders = new ArrayList<>();
        for (Order o : allOrders) {
            if (o.getUser().equals(user)) {
                userOrders.add(o);
            }
        }
        userOrders.sort(Comparator.comparingDouble(Order::getPrice));
        return userOrders;
    }

    public List<Order> getAllOrders() {
        return allOrders;
    }

    public List<Trade> getAllTrades() {
        return allTrades;
    }
}