package trading.model;

import trading.model.enums.OrderSide;
import trading.model.enums.OrderStatus;
import java.time.LocalDateTime;

public abstract class Order implements Printable, Comparable<Order> {
    private static int nextId = 1;

    private int id;
    private final User user;
    private final Market market;
    private final OrderSide side;
    private final double price;
    private final double quantity;
    private OrderStatus status;
    private final LocalDateTime timestamp;

    protected Order(User user, Market market, OrderSide side, double price, double quantity) {
        this.id = nextId++;
        this.user = user;
        this.market = market;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.status = OrderStatus.OPEN;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        if (id >= nextId) {
            nextId = id + 1;
        }
    }

    public User getUser() {
        return user;
    }

    public Market getMarket() {
        return market;
    }

    public OrderSide getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(Order other) {
        return Double.compare(this.price, other.price);
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}