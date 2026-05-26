package trading.dao;

import trading.model.Market;

import java.util.List;
import java.util.Optional;

public interface MarketDao {
    void create(Market market);
    Optional<Market> findBySymbol(String symbol);
    List<Market> findAll();
    void update(Market market);
    void delete(String symbol);
}
