package trading;

import trading.dao.*;
import trading.database.DatabaseConnection;
import trading.exception.TradingException;
import trading.model.*;
import trading.model.enums.OrderSide;
import trading.model.enums.PositionSide;
import trading.service.AuditService;
import trading.service.MarketService;
import trading.service.TradingService;
import trading.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static UserService userService;
    private static MarketService marketService;
    private static TradingService tradingService;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        DatabaseConnection.getInstance();

        UserDao userDao = new UserDaoImpl();
        AssetDao assetDao = new AssetDaoImpl();
        MarketDao marketDao = new MarketDaoImpl(assetDao);
        OrderDao orderDao = new OrderDaoImpl(userDao, marketDao);
        TradeDao tradeDao = new TradeDaoImpl(userDao, marketDao);
        BalanceDao balanceDao = new BalanceDaoImpl();

        userService = UserService.getInstance(userDao, balanceDao);
        marketService = MarketService.getInstance(assetDao, marketDao);
        tradingService = TradingService.getInstance(userService, marketService, orderDao, tradeDao);

        initializeMarkets();
        tradingService.loadFromDatabase();
        System.out.println("=== Welcome to MiniTrader ===");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Choose an option: ");
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1"  -> registerUser();
                    case "2"  -> depositFunds();
                    case "3"  -> withdrawFunds();
                    case "4"  -> placeSpotOrder();
                    case "5"  -> placePerpOrder();
                    case "6"  -> cancelOrder();
                    case "7"  -> viewPortfolio();
                    case "8"  -> viewOpenOrders();
                    case "9"  -> viewTradeHistory();
                    case "10" -> viewPositions();
                    case "11" -> viewOrderBook();
                    case "12" -> filterTradesByMarket();
                    case "13" -> viewOrdersSortedByPrice();
                    case "14" -> marketSummary();
                    case "0"  -> {
                        System.out.println("Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (TradingException e) {
                System.out.println("[Error] " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid number format. Please try again.");
            }
        }
        scanner.close();
        DatabaseConnection.getInstance().close();
    }

    private static void initializeMarkets() {
        try {
            marketService.getMarket("BTC/USDT");
        } catch (TradingException e) {
            SpotAsset btc = new SpotAsset("BTC", "Bitcoin", 21_000_000);
            SpotAsset eth = new SpotAsset("ETH", "Ethereum", 120_000_000);
            PerpAsset btcPerp = new PerpAsset("BTC-PERP", "Bitcoin Perpetual", 0.0001, 50);
            PerpAsset ethPerp = new PerpAsset("ETH-PERP", "Ethereum Perpetual", 0.00015, 30);

            marketService.addMarket("BTC/USDT", btc, "USDT", 67000.00);
            marketService.addMarket("ETH/USDT", eth, "USDT", 3500.00);
            marketService.addMarket("BTC-PERP/USDT", btcPerp, "USDT", 67000.00);
            marketService.addMarket("ETH-PERP/USDT", ethPerp, "USDT", 3500.00);
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Menu ---");
        System.out.println(" 1. Register user");
        System.out.println(" 2. Deposit funds");
        System.out.println(" 3. Withdraw funds");
        System.out.println(" 4. Place spot order");
        System.out.println(" 5. Place perp order");
        System.out.println(" 6. Cancel order");
        System.out.println(" 7. View portfolio");
        System.out.println(" 8. View open orders");
        System.out.println(" 9. View trade history");
        System.out.println("10. View positions");
        System.out.println("11. View order book");
        System.out.println("12. Filter trades by market");
        System.out.println("13. View orders sorted by price");
        System.out.println("14. Market summary");
        System.out.println(" 0. Exit");
    }

    private static void registerUser() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        User user = userService.registerUser(username, email);
        System.out.println("Registered: " + user.toPrettyString());
    }

    private static void depositFunds() {
        User user = promptUser();
        System.out.print("Asset symbol (e.g. USDT, BTC): ");
        String asset = scanner.nextLine().trim().toUpperCase();
        System.out.print("Amount: ");
        double amount = Double.parseDouble(scanner.nextLine().trim());
        userService.deposit(user, asset, amount);
        System.out.printf("Deposited %.4f %s for %s.%n", amount, asset, user.getUsername());
    }

    private static void withdrawFunds() {
        User user = promptUser();
        System.out.print("Asset symbol (e.g. USDT, BTC): ");
        String asset = scanner.nextLine().trim().toUpperCase();
        System.out.print("Amount: ");
        double amount = Double.parseDouble(scanner.nextLine().trim());
        userService.withdraw(user, asset, amount);
        System.out.printf("Withdrew %.4f %s for %s.%n", amount, asset, user.getUsername());
    }

    private static void placeSpotOrder() {
        User user = promptUser();
        String marketSymbol = promptMarket();
        OrderSide side = promptSide();
        System.out.print("Price: ");
        double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Quantity: ");
        double quantity = Double.parseDouble(scanner.nextLine().trim());
        SpotOrder order = tradingService.placeSpotOrder(user, marketSymbol, side, price, quantity);
        System.out.println("Order placed: " + order.toPrettyString());
    }

    private static void placePerpOrder() {
        User user = promptUser();
        String marketSymbol = promptMarket();
        OrderSide side = promptSide();
        System.out.print("Price: ");
        double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Quantity: ");
        double quantity = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Leverage (e.g. 5): ");
        int leverage = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Position side (LONG/SHORT): ");
        PositionSide posSide = PositionSide.valueOf(scanner.nextLine().trim().toUpperCase());
        PerpOrder order = tradingService.placePerpOrder(user, marketSymbol, side, price, quantity,
                leverage, posSide);
        System.out.println("Order placed: " + order.toPrettyString());
    }

    private static void cancelOrder() {
        System.out.print("Order ID to cancel: ");
        int orderId = Integer.parseInt(scanner.nextLine().trim());
        tradingService.cancelOrder(orderId);
    }

    private static void viewPortfolio() {
        User user = promptUser();
        Portfolio portfolio = userService.getPortfolio(user);
        System.out.println(portfolio.toPrettyString());
        AuditService.getInstance().log("VIEW_PORTFOLIO");
    }

    private static void viewOpenOrders() {
        User user = promptUser();
        List<Order> orders = tradingService.getOpenOrders(user);
        if (orders.isEmpty()) {
            System.out.println("No open orders for " + user.getUsername() + ".");
        } else {
            System.out.println("Open orders for " + user.getUsername() + ":");
            for (Order o : orders) {
                System.out.println("  " + o.toPrettyString());
            }
        }
        AuditService.getInstance().log("VIEW_OPEN_ORDERS");
    }

    private static void viewTradeHistory() {
        User user = promptUser();
        List<Trade> trades = tradingService.getTradeHistory(user);
        if (trades.isEmpty()) {
            System.out.println("No trades for " + user.getUsername() + ".");
        } else {
            System.out.println("Trade history for " + user.getUsername() + ":");
            for (Trade t : trades) {
                System.out.println("  " + t.toPrettyString());
            }
        }
        AuditService.getInstance().log("VIEW_TRADE_HISTORY");
    }

    private static void viewPositions() {
        User user = promptUser();
        Portfolio portfolio = userService.getPortfolio(user);
        List<Position> positions = portfolio.getOpenPositions();
        if (positions.isEmpty()) {
            System.out.println("No open positions for " + user.getUsername() + ".");
        } else {
            System.out.println("Open positions for " + user.getUsername() + ":");
            for (Position p : positions) {
                System.out.println("  " + p.toPrettyString());
            }
        }
        AuditService.getInstance().log("VIEW_POSITIONS");
    }

    private static void viewOrderBook() {
        String marketSymbol = promptMarket();
        OrderBook ob = marketService.getOrderBook(marketSymbol);
        System.out.println(ob.toPrettyString());
        AuditService.getInstance().log("VIEW_ORDER_BOOK");
    }

    private static void filterTradesByMarket() {
        String marketSymbol = promptMarket();
        List<Trade> trades = tradingService.filterTradesByMarket(marketSymbol);
        if (trades.isEmpty()) {
            System.out.println("No trades found for " + marketSymbol + ".");
        } else {
            System.out.println("Trades for " + marketSymbol + ":");
            for (Trade t : trades) {
                System.out.println("  " + t.toPrettyString());
            }
        }
        AuditService.getInstance().log("FILTER_TRADES_BY_MARKET");
    }

    private static void viewOrdersSortedByPrice() {
        User user = promptUser();
        List<Order> orders = tradingService.getOrdersSortedByPrice(user);
        if (orders.isEmpty()) {
            System.out.println("No orders for " + user.getUsername() + ".");
        } else {
            System.out.println("Orders for " + user.getUsername() + " (sorted by price):");
            for (Order o : orders) {
                System.out.println("  " + o.toPrettyString());
            }
        }
        AuditService.getInstance().log("VIEW_ORDERS_SORTED_BY_PRICE");
    }

    private static void marketSummary() {
        marketService.printMarketSummary();
    }

    private static User promptUser() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        return userService.getUserByUsername(username);
    }

    private static String promptMarket() {
        System.out.println("Available markets: ");
        for (Map.Entry<String, Market> entry : marketService.getAllMarkets().entrySet()) {
            System.out.println("  " + entry.getKey());
        }
        System.out.print("Market symbol: ");
        return scanner.nextLine().trim();
    }

    private static OrderSide promptSide() {
        System.out.print("Side (BUY/SELL): ");
        return OrderSide.valueOf(scanner.nextLine().trim().toUpperCase());
    }
}
