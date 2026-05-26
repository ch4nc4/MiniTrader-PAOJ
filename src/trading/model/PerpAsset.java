package trading.model;

import trading.model.enums.AssetType;

public class PerpAsset extends Asset {
    private double fundingRate;
    private int maxLeverage;

    public PerpAsset(String symbol, String name, double fundingRate, int maxLeverage) {
        super(symbol, name, AssetType.PERPETUAL);
        this.fundingRate = fundingRate;
        this.maxLeverage = maxLeverage;
    }

    public double getFundingRate() {
        return fundingRate;
    }

    public void setFundingRate(double fundingRate) {
        this.fundingRate = fundingRate;
    }

    public int getMaxLeverage() {
        return maxLeverage;
    }

    public void setMaxLeverage(int maxLeverage) {
        this.maxLeverage = maxLeverage;
    }

    @Override
    public String toPrettyString() {
        return String.format("PerpAsset[symbol=%s, name=%s, fundingRate=%.4f%%, maxLeverage=%dx]",
                getSymbol(), getName(), fundingRate * 100, maxLeverage);
    }
}
