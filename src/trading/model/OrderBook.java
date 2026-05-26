package trading.model;

import trading.model.enums.OrderSide;
import trading.model.enums.OrderStatus;

import java.util.Comparator;
import java.util.TreeSet;

public class OrderBook implements Printable {
    private final Market market;
    private final TreeSet<Order> buyOrders;
    private final TreeSet<Order> sellOrders;

    public OrderBook(Market market) {
        this.market = market;
        this.buyOrders = new TreeSet<>(Comparator.comparingDouble(Order::getPrice).reversed()
                .thenComparingInt(Order::getId));
        this.sellOrders = new TreeSet<>(Comparator.comparingDouble(Order::getPrice)
                .thenComparingInt(Order::getId));
    }

    public Market getMarket() {
        return market;
    }

    public TreeSet<Order> getBuyOrders() {
        return buyOrders;
    }

    public TreeSet<Order> getSellOrders() {
        return sellOrders;
    }

    public void addOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            buyOrders.add(order);
        } else {
            sellOrders.add(order);
        }
    }

    public void removeOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            buyOrders.remove(order);
        } else {
            sellOrders.remove(order);
        }
    }

    public Order getBestBuy() {
        return buyOrders.isEmpty() ? null : buyOrders.first();
    }

    public Order getBestSell() {
        return sellOrders.isEmpty() ? null : sellOrders.first();
    }

    @Override
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Order Book: %s ===\n", market.getSymbol()));
        sb.append("--- Sell Orders (asks) ---\n");
        if (sellOrders.isEmpty()) {
            sb.append("  (empty)\n");
        } else {
            for (Order o : sellOrders) {
                if (o.getStatus() == OrderStatus.OPEN) {
                    sb.append("  ").append(o.toPrettyString()).append("\n");
                }
            }
        }
        sb.append("--- Buy Orders (bids) ---\n");
        if (buyOrders.isEmpty()) {
            sb.append("  (empty)\n");
        } else {
            for (Order o : buyOrders) {
                if (o.getStatus() == OrderStatus.OPEN) {
                    sb.append("  ").append(o.toPrettyString()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}
