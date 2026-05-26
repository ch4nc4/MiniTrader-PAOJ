package trading.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Trade implements Printable {
    private static int nextId = 1;

    private final int id;
    private final User buyer;
    private final User seller;
    private final Market market;
    private final double price;
    private final double quantity;
    private final LocalDateTime timestamp;

    public Trade(User buyer, User seller, Market market, double price, double quantity) {
        this.id = nextId++;
        this.buyer = buyer;
        this.seller = seller;
        this.market = market;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getSeller() {
        return seller;
    }

    public Market getMarket() {
        return market;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toPrettyString() {
        return String.format("Trade[id=%d, %s, buyer=%s, seller=%s, price=%.2f, qty=%.4f, time=%s]",
                id, market.getSymbol(), buyer.getUsername(), seller.getUsername(),
                price, quantity, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}
