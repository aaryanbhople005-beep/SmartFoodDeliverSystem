
package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database connection manager with schema initialization.
 */
public class DBConnection {

    private static final String URL = "jdbc:sqlite:smartfooddelivery.db";
    private static volatile boolean initialized = false;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // Enable foreign keys
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /**
     * Initialize database schema. Called once at startup.
     */
    public static synchronized void initialize() throws SQLException {
        if (initialized) return;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Restaurants table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS restaurants (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT,
                    category TEXT
                )
            """);
            
            // Menu items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS menu_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id TEXT NOT NULL,
                    restaurant_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    price REAL NOT NULL,
                    category TEXT NOT NULL,
                    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
                    UNIQUE(restaurant_id, item_id)
                )
            """);
            
            // Carts table (one per session/user)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS carts (
                    session_id TEXT PRIMARY KEY,
                    user_id INTEGER,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                )
            """);
            
            // Cart items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cart_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    price REAL NOT NULL,
                    category TEXT,
                    restaurant TEXT,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (session_id) REFERENCES carts(session_id) ON DELETE CASCADE,
                    UNIQUE(session_id, item_id)
                )
            """);
            
            // Cart snapshots (Memento pattern)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cart_snapshots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    user_id INTEGER,
                    token TEXT NOT NULL UNIQUE,
                    cart_data TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (session_id) REFERENCES carts(session_id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                )
            """);
            
            // Sessions table for persistent authentication
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    token TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    expiry_ms INTEGER NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);
            
            // Indexes for performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_menu_items_restaurant ON menu_items(restaurant_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cart_items_session ON cart_items(session_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cart_snapshots_user ON cart_snapshots(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_expiry ON sessions(expiry_ms)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id)");
            
            // Seed initial restaurant and menu data
            seedInitialData(stmt);
            
            initialized = true;
        }
    }
    
    private static void seedInitialData(Statement stmt) throws SQLException {
        // Check if data already exists
        try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM restaurants")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }
        
        // Insert restaurants
        stmt.execute("""
            INSERT INTO restaurants (id, name, description, category) VALUES
            ('pizza', 'Pizza Hub', 'Italian & continental pizzas, burgers and combos', 'Italian / Pizza'),
            ('spice', 'Spice Hub', 'North Indian and Biryani delicacies', 'Indian / Biryani')
        """);
        
        // Insert Pizza Hub menu items
        stmt.execute("""
            INSERT INTO menu_items (item_id, restaurant_id, name, price, category) VALUES
            ('P101', 'pizza', 'Margherita Pizza', 200.0, 'VEG'),
            ('P102', 'pizza', 'Farmhouse Pizza', 300.0, 'VEG'),
            ('P103', 'pizza', 'Paneer Tikka Pizza', 320.0, 'VEG'),
            ('P201', 'pizza', 'Chicken Pizza', 350.0, 'NON-VEG'),
            ('P202', 'pizza', 'BBQ Chicken Pizza', 400.0, 'NON-VEG'),
            ('P203', 'pizza', 'Chicken Tikka Pizza', 380.0, 'NON-VEG'),
            ('P301', 'pizza', 'Veg Pizza Combo', 399.0, 'COMBO'),
            ('P302', 'pizza', 'Chicken Pizza Combo', 499.0, 'COMBO'),
            ('P303', 'pizza', 'Family Pizza Combo', 799.0, 'COMBO'),
            ('P401', 'pizza', 'Coke', 50.0, 'DRINKS'),
            ('P402', 'pizza', 'Sprite', 50.0, 'DRINKS'),
            ('P403', 'pizza', 'Cold Coffee', 80.0, 'DRINKS')
        """);
        
        // Insert Spice Hub menu items
        stmt.execute("""
            INSERT INTO menu_items (item_id, restaurant_id, name, price, category) VALUES
            ('S101', 'spice', 'Paneer Tikka', 220.0, 'VEG'),
            ('S102', 'spice', 'Veg Biryani', 200.0, 'VEG'),
            ('S103', 'spice', 'Dal Tadka', 160.0, 'VEG'),
            ('S201', 'spice', 'Chicken Biryani', 280.0, 'NON-VEG'),
            ('S202', 'spice', 'Butter Chicken', 300.0, 'NON-VEG'),
            ('S203', 'spice', 'Chicken Tikka', 260.0, 'NON-VEG'),
            ('S301', 'spice', 'Veg Thali Combo', 299.0, 'COMBO'),
            ('S302', 'spice', 'Chicken Meal Combo', 399.0, 'COMBO'),
            ('S303', 'spice', 'Biryani Combo', 449.0, 'COMBO'),
            ('S401', 'spice', 'Masala Chaas', 40.0, 'DRINKS'),
            ('S402', 'spice', 'Lemon Soda', 60.0, 'DRINKS'),
            ('S403', 'spice', 'Mango Lassi', 80.0, 'DRINKS')
        """);
    }
}
