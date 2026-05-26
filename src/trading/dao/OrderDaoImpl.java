package trading.dao;

import trading.database.DatabaseConnection;
import trading.model.*;
import trading.model.enums.OrderSide;
import trading.model.enums.OrderStatus;
import trading.model.enums.PositionSide;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDaoImpl implements OrderDao {

    private final UserDao userDao;
    private final MarketDao marketDao;

    public OrderDaoImpl(UserDao userDao, MarketDao marketDao) {
        this.userDao = userDao;
        this.marketDao = marketDao;
    }

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public int create(Order order) {
        String sql = "INSERT INTO orders (user_id, market_symbol, side, price, quantity, status, "
                + "order_type, leverage, position_side, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getUser().getId());
            ps.setString(2, order.getMarket().getSymbol());
            ps.setString(3, order.getSide().name());
            ps.setDouble(4, order.getPrice());
            ps.setDouble(5, order.getQuantity());
            ps.setString(6, order.getStatus().name());
            if (order instanceof PerpOrder) {
                PerpOrder po = (PerpOrder) order;
                ps.setString(7, "PERP");
                ps.setInt(8, po.getLeverage());
                ps.setString(9, po.getPositionSide().name());
            } else {
                ps.setString(7, "SPOT");
                ps.setNull(8, Types.INTEGER);
                ps.setNull(9, Types.VARCHAR);
            }
            ps.setString(10, order.getTimestamp().toString());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int dbId = keys.getInt(1);
                order.setId(dbId);
                return dbId;
            }
            return order.getId();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating order: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Order> findById(int id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding order: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> findAll() {
        String sql = "SELECT * FROM orders";
        List<Order> orders = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing orders: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public List<Order> findByUserId(int userId) {
        String sql = "SELECT * FROM orders WHERE user_id = ?";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding orders by user: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public void updateStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating order status: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting order: " + e.getMessage(), e);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        int dbId = rs.getInt("id");
        int userId = rs.getInt("user_id");
        String marketSymbol = rs.getString("market_symbol");
        User user = userDao.findById(userId)
                .orElseThrow(() -> new SQLException("User not found: " + userId));
        Market market = marketDao.findBySymbol(marketSymbol)
                .orElseThrow(() -> new SQLException("Market not found: " + marketSymbol));
        OrderSide side = OrderSide.valueOf(rs.getString("side"));
        double price = rs.getDouble("price");
        double quantity = rs.getDouble("quantity");
        String orderType = rs.getString("order_type");

        Order order;
        if ("PERP".equals(orderType)) {
            int leverage = rs.getInt("leverage");
            PositionSide posSide = PositionSide.valueOf(rs.getString("position_side"));
            order = new PerpOrder(user, market, side, price, quantity, leverage, posSide);
        } else {
            order = new SpotOrder(user, market, side, price, quantity);
        }
        order.setId(dbId);
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        return order;
    }
}