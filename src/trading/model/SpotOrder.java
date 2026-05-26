package trading.model;

import trading.model.enums.OrderSide;

public class SpotOrder extends Order {

    public SpotOrder(User user, Market market, OrderSide side, double price, double quantity) {
        super(user, market, side, price, quantity);
    }

    @Override
    public String toPrettyString() {
        return String.format("SpotOrder[id=%d, %s %s %.4f @ %.2f, status=%s, user=%s]",
                getId(), getSide(), getMarket().getSymbol(), getQuantity(), getPrice(),
                getStatus(), getUser().getUsername());
    }
}
