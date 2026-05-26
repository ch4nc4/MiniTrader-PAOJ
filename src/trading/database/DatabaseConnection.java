package trading.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection(String url) {
        try {
            this.connection = DriverManager.getConnection(url);
            initializeSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseConnection getInstance() { // synchronized used here to ensure we keep the singleton trait of the class
        if (instance == null) {
            instance = new DatabaseConnection("jdbc:sqlite:minitrader.db");
        }
        return instance;
    }

    public static synchronized DatabaseConnection getInstance(String url) {
        instance = new DatabaseConnection(url);
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeSchema() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT UNIQUE NOT NULL, "
                    + "email TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS assets ("
                    + "symbol TEXT PRIMARY KEY, "
                    + "name TEXT NOT NULL, "
                    + "asset_type TEXT NOT NULL, "
                    + "circulating_supply REAL, "
                    + "funding_rate REAL, "
                    + "max_leverage INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS markets ("
                    + "symbol TEXT PRIMARY KEY, "
                    + "base_asset_symbol TEXT NOT NULL REFERENCES assets(symbol), "
                    + "quote_asset TEXT NOT NULL, "
                    + "last_price REAL NOT NULL, "
                    + "volume_24h REAL DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS orders ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL REFERENCES users(id), "
                    + "market_symbol TEXT NOT NULL REFERENCES markets(symbol), "
                    + "side TEXT NOT NULL, "
                    + "price REAL NOT NULL, "
                    + "quantity REAL NOT NULL, "
                    + "status TEXT NOT NULL, "
                    + "order_type TEXT NOT NULL, "
                    + "leverage INTEGER, "
                    + "position_side TEXT, "
                    + "created_at TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS trades ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "buyer_id INTEGER NOT NULL REFERENCES users(id), "
                    + "seller_id INTEGER NOT NULL REFERENCES users(id), "
                    + "market_symbol TEXT NOT NULL REFERENCES markets(symbol), "
                    + "price REAL NOT NULL, "
                    + "quantity REAL NOT NULL, "
                    + "created_at TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS balances ("
                    + "user_id INTEGER NOT NULL REFERENCES users(id), "
                    + "asset_symbol TEXT NOT NULL, "
                    + "amount REAL NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (user_id, asset_symbol))");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
        instance = null;
    }
}
