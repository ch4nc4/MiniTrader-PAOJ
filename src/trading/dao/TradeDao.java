package trading.dao;

import trading.model.Trade;

import java.util.List;
import java.util.Optional;

public interface TradeDao {
    int create(Trade trade);
    Optional<Trade> findById(int id);
    List<Trade> findAll();
    List<Trade> findByUserId(int userId);
    List<Trade> findByMarketSymbol(String marketSymbol);
    void delete(int id);
}
