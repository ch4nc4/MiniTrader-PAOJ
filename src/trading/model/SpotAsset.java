package trading.model;

import trading.model.enums.AssetType;

public class SpotAsset extends Asset {
    private double circulatingSupply;

    public SpotAsset(String symbol, String name, double circulatingSupply) {
        super(symbol, name, AssetType.SPOT);
        this.circulatingSupply = circulatingSupply;
    }

    public double getCirculatingSupply() {
        return circulatingSupply;
    }

    public void setCirculatingSupply(double circulatingSupply) {
        this.circulatingSupply = circulatingSupply;
    }

    @Override
    public String toPrettyString() {
        return String.format("SpotAsset[symbol=%s, name=%s, supply=%.2f]",
                getSymbol(), getName(), circulatingSupply);
    }
}
