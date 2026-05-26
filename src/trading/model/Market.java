package trading.model;

import trading.model.enums.AssetType;

public class Market implements Printable {
    private final String symbol;
    private final Asset baseAsset;
    private final String quoteAsset;
    private double lastPrice;
    private double volume24h;

    public Market(String symbol, Asset baseAsset, String quoteAsset, double lastPrice) {
        this.symbol = symbol;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.lastPrice = lastPrice;
        this.volume24h = 0.0;
    }

    public String getSymbol() {
        return symbol;
    }

    public Asset getBaseAsset() {
        return baseAsset;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public double getVolume24h() {
        return volume24h;
    }

    public void addVolume(double amount) {
        this.volume24h += amount;
    }

    public AssetType getMarketType() {
        return baseAsset.getAssetType();
    }

    @Override
    public String toPrettyString() {
        return String.format("Market[%s | price=%.2f | vol24h=%.2f | type=%s]",
                symbol, lastPrice, volume24h, getMarketType());
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}
