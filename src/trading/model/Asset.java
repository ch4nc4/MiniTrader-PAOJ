package trading.model;

import trading.model.enums.AssetType;

public abstract class Asset implements Printable {
    private final String symbol;
    private String name;
    private final AssetType assetType;

    protected Asset(String symbol, String name, AssetType assetType) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}
