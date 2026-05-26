package trading.dao;

import trading.database.DatabaseConnection;
import trading.model.Market;
import trading.model.Trade;
import trading.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TradeDaoImpl implements TradeDao {

    private final UserDao userDao;
    private final MarketDao marketDao;

    public TradeDaoImpl(UserDao userDao, MarketDao marketDao) {
        this.userDao = userDao;
        this.marketDao = marketDao;
    }

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public int create(Trade trade) {
        String sql = "INSERT INTO trades (buyer_id, seller_id, market_symbol, price, quantity, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, trade.getBuyer().getId());
            ps.setInt(2, trade.getSeller().getId());
            ps.setString(3, trade.getMarket().getSymbol());
            ps.setDouble(4, trade.getPrice());
            ps.setDouble(5, trade.getQuantity());
            ps.setString(6, trade.getTimestamp().toString());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
            return trade.getId();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating trade: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Trade> findById(int id) {
        String sql = "SELECT * FROM trades WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding trade: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Trade> findAll() {
        String sql = "SELECT * FROM trades";
        List<Trade> trades = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                trades.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing trades: " + e.getMessage(), e);
        }
        return trades;
    }

    @Override
    public List<Trade> findByUserId(int userId) {
        String sql = "SELECT * FROM trades WHERE buyer_id = ? OR seller_id = ?";
        List<Trade> trades = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                trades.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding trades by user: " + e.getMessage(), e);
        }
        return trades;
    }

    @Override
    public List<Trade> findByMarketSymbol(String marketSymbol) {
        String sql = "SELECT * FROM trades WHERE market_symbol = ?";
        List<Trade> trades = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, marketSymbol);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                trades.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding trades by market: " + e.getMessage(), e);
        }
        return trades;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM trades WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting trade: " + e.getMessage(), e);
        }
    }

    private Trade mapRow(ResultSet rs) throws SQLException {
        User buyer = userDao.findById(rs.getInt("buyer_id"))
                .orElseThrow(() -> new SQLException("Buyer not found"));
        User seller = userDao.findById(rs.getInt("seller_id"))
                .orElseThrow(() -> new SQLException("Seller not found"));
        Market market = marketDao.findBySymbol(rs.getString("market_symbol"))
                .orElseThrow(() -> new SQLException("Market not found"));
        return new Trade(buyer, seller, market, rs.getDouble("price"), rs.getDouble("quantity"));
    }
}
