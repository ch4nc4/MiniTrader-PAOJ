package trading.dao;

import trading.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    User create(String username, String email);
    Optional<User> findById(int id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void update(User user);
    void delete(int id);
}
