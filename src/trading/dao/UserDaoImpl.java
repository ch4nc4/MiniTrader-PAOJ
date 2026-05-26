package trading.dao;

import trading.database.DatabaseConnection;
import trading.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public User create(String username, String email) {
        String sql = "INSERT INTO users (username, email) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return new User(keys.getInt(1), username, email);
            }
            throw new SQLException("Failed to retrieve generated ID.");
        } catch (SQLException e) {
            throw new RuntimeException("Error creating user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, email FROM users WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, email FROM users WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, email FROM users";
        List<User> users = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing users: " + e.getMessage(), e);
        }
        return users;
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setInt(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("email"));
    }
}
