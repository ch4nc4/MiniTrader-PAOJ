package trading.model;

import trading.model.enums.PositionSide;

public class Position implements Printable {
    private static int nextId = 1;

    private final int id;
    private final User user;
    private final Market market;
    private final PositionSide side;
    private final double entryPrice;
    private final double quantity;
    private final int leverage;
    private boolean open;

    public Position(User user, Market market, PositionSide side, double entryPrice,
                    double quantity, int leverage) {
        this.id = nextId++;
        this.user = user;
        this.market = market;
        this.side = side;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.leverage = leverage;
        this.open = true;
    }

    public int getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Market getMarket() {
        return market;
    }

    public PositionSide getSide() {
        return side;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public double getQuantity() {
        return quantity;
    }

    public int getLeverage() {
        return leverage;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        this.open = false;
    }

    public double getUnrealizedPnl() {
        double currentPrice = market.getLastPrice();
        double priceDiff = (side == PositionSide.LONG)
                ? (currentPrice - entryPrice)
                : (entryPrice - currentPrice);
        return priceDiff * quantity * leverage;
    }

    @Override
    public String toPrettyString() {
        return String.format("Position[id=%d, %s %s, entry=%.2f, qty=%.4f, %dx, PnL=%.2f, %s]",
                id, side, market.getSymbol(), entryPrice, quantity, leverage,
                getUnrealizedPnl(), open ? "OPEN" : "CLOSED");
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}
