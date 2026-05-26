package trading.dao;

import trading.database.DatabaseConnection;
import trading.model.Asset;
import trading.model.PerpAsset;
import trading.model.SpotAsset;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssetDaoImpl implements AssetDao {

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void create(Asset asset) {
        String sql = "INSERT INTO assets (symbol, name, asset_type, circulating_supply, funding_rate, max_leverage) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, asset.getSymbol());
            ps.setString(2, asset.getName());
            ps.setString(3, asset.getAssetType().name());
            if (asset instanceof SpotAsset) {
                ps.setDouble(4, ((SpotAsset) asset).getCirculatingSupply());
                ps.setNull(5, Types.REAL);
                ps.setNull(6, Types.INTEGER);
            } else if (asset instanceof PerpAsset) {
                ps.setNull(4, Types.REAL);
                ps.setDouble(5, ((PerpAsset) asset).getFundingRate());
                ps.setInt(6, ((PerpAsset) asset).getMaxLeverage());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating asset: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Asset> findBySymbol(String symbol) {
        String sql = "SELECT * FROM assets WHERE symbol = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding asset: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Asset> findAll() {
        String sql = "SELECT * FROM assets";
        List<Asset> assets = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                assets.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing assets: " + e.getMessage(), e);
        }
        return assets;
    }

    @Override
    public void update(Asset asset) {
        String sql = "UPDATE assets SET name = ?, circulating_supply = ?, funding_rate = ?, max_leverage = ? "
                + "WHERE symbol = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, asset.getName());
            if (asset instanceof SpotAsset) {
                ps.setDouble(2, ((SpotAsset) asset).getCirculatingSupply());
                ps.setNull(3, Types.REAL);
                ps.setNull(4, Types.INTEGER);
            } else if (asset instanceof PerpAsset) {
                ps.setNull(2, Types.REAL);
                ps.setDouble(3, ((PerpAsset) asset).getFundingRate());
                ps.setInt(4, ((PerpAsset) asset).getMaxLeverage());
            }
            ps.setString(5, asset.getSymbol());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating asset: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String symbol) {
        String sql = "DELETE FROM assets WHERE symbol = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting asset: " + e.getMessage(), e);
        }
    }

    private Asset mapRow(ResultSet rs) throws SQLException {
        String type = rs.getString("asset_type");
        if ("SPOT".equals(type)) {
            return new SpotAsset(rs.getString("symbol"), rs.getString("name"),
                    rs.getDouble("circulating_supply"));
        } else {
            return new PerpAsset(rs.getString("symbol"), rs.getString("name"),
                    rs.getDouble("funding_rate"), rs.getInt("max_leverage"));
        }
    }
}
