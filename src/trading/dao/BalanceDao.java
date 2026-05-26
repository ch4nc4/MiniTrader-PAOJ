package trading.dao;

import java.util.Map;

public interface BalanceDao {
    void save(int userId, String assetSymbol, double amount);
    Map<String, Double> findByUserId(int userId);
}