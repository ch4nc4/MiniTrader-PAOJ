package trading.dao;

import trading.model.Asset;

import java.util.List;
import java.util.Optional;

public interface AssetDao {
    void create(Asset asset);
    Optional<Asset> findBySymbol(String symbol); // symbol we are looking for might not exist in the db
    List<Asset> findAll();
    void update(Asset asset);
    void delete(String symbol);
}
