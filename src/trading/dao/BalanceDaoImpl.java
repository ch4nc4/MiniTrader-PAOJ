package trading.dao;

import trading.database.DatabaseConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BalanceDaoImpl implements BalanceDao {

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void save(int userId, String assetSymbol, double amount) {
        String sql = "INSERT INTO balances (user_id, asset_symbol, amount) VALUES (?, ?, ?) "
                + "ON CONFLICT(user_id, asset_symbol) DO UPDATE SET amount = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, assetSymbol);
            ps.setDouble(3, amount);
            ps.setDouble(4, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving balance: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Double> findByUserId(int userId) {
        String sql = "SELECT asset_symbol, amount FROM balances WHERE user_id = ?";
        Map<String, Double> balances = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                balances.put(rs.getString("asset_symbol"), rs.getDouble("amount"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading balances: " + e.getMessage(), e);
        }
        return balances;
    }
}