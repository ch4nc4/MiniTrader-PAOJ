package trading.dao;

import trading.database.DatabaseConnection;
import trading.model.Asset;
import trading.model.Market;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MarketDaoImpl implements MarketDao {

    private final AssetDao assetDao;

    public MarketDaoImpl(AssetDao assetDao) {
        this.assetDao = assetDao;
    }

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void create(Market market) {
        String sql = "INSERT INTO markets (symbol, base_asset_symbol, quote_asset, last_price, volume_24h) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, market.getSymbol());
            ps.setString(2, market.getBaseAsset().getSymbol());
            ps.setString(3, market.getQuoteAsset());
            ps.setDouble(4, market.getLastPrice());
            ps.setDouble(5, market.getVolume24h());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating market: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Market> findBySymbol(String symbol) {
        String sql = "SELECT * FROM markets WHERE symbol = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding market: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Market> findAll() {
        String sql = "SELECT * FROM markets";
        List<Market> markets = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                markets.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing markets: " + e.getMessage(), e);
        }
        return markets;
    }

    @Override
    public void update(Market market) {
        String sql = "UPDATE markets SET last_price = ?, volume_24h = ? WHERE symbol = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setDouble(1, market.getLastPrice());
            ps.setDouble(2, market.getVolume24h());
            ps.setString(3, market.getSymbol());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating market: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String symbol) {
        String sql = "DELETE FROM markets WHERE symbol = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting market: " + e.getMessage(), e);
        }
    }

    private Market mapRow(ResultSet rs) throws SQLException {
        String baseSymbol = rs.getString("base_asset_symbol");
        Asset baseAsset = assetDao.findBySymbol(baseSymbol)
                .orElseThrow(() -> new SQLException("Base asset not found: " + baseSymbol));
        Market market = new Market(rs.getString("symbol"), baseAsset,
                rs.getString("quote_asset"), rs.getDouble("last_price"));
        double volume = rs.getDouble("volume_24h");
        if (volume > 0) {
            market.addVolume(volume);
        }
        return market;
    }
}
