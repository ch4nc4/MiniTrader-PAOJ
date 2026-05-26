package trading.model;

import trading.model.enums.OrderSide;
import trading.model.enums.PositionSide;

public class PerpOrder extends Order {
    private final int leverage;
    private final PositionSide positionSide;

    public PerpOrder(User user, Market market, OrderSide side, double price, double quantity,
                     int leverage, PositionSide positionSide) {
        super(user, market, side, price, quantity);
        this.leverage = leverage;
        this.positionSide = positionSide;
    }

    public int getLeverage() {
        return leverage;
    }

    public PositionSide getPositionSide() {
        return positionSide;
    }

    @Override
    public String toPrettyString() {
        return String.format("PerpOrder[id=%d, %s %s %.4f @ %.2f, %dx %s, status=%s, user=%s]",
                getId(), getSide(), getMarket().getSymbol(), getQuantity(), getPrice(),
                leverage, positionSide, getStatus(), getUser().getUsername());
    }
}
