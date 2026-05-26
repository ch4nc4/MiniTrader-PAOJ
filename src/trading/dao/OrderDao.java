package trading.dao;

import trading.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderDao {
    int create(Order order);
    Optional<Order> findById(int id);
    List<Order> findAll();
    List<Order> findByUserId(int userId);
    void updateStatus(int orderId, String status);
    void delete(int id);
}
