package repository;

import database.DBConnection;
import memento.CartItem;
import memento.CartMemento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQLite-backed cart repository for persistent cart storage.
 * Replaces the in-memory sessionCarts map with database persistence.
 */
public class DatabaseCartRepository {

    /**
     * Get all cart items for a session.
     */
    public List<CartItem> getCartItems(String sessionId) {
        List<CartItem> items = new ArrayList<>();
        String sql = """
            SELECT item_id, name, price, category, restaurant, quantity
            FROM cart_items WHERE session_id = ?
        """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new CartItem(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("category"),
                        rs.getString("restaurant"),
                        rs.getInt("quantity")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load cart for session: " + sessionId, e);
        }
        
        return items;
    }

    /**
     * Get cart total for a session.
     */
    public double getCartTotal(String sessionId) {
        String sql = "SELECT SUM(price * quantity) FROM cart_items WHERE session_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get cart total", e);
        }
        return 0.0;
    }

    /**
     * Add or update an item in the cart.
     */
    public boolean addToCart(String sessionId, CartItem item) {
        if (sessionId == null || sessionId.isBlank() || item == null) {
            return false;
        }
        
        ensureCartExists(sessionId);
        
        String sql = """
            INSERT INTO cart_items (session_id, item_id, name, price, category, restaurant, quantity)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(session_id, item_id) DO UPDATE SET
                quantity = cart_items.quantity + excluded.quantity,
                name = excluded.name,
                price = excluded.price,
                category = excluded.category,
                restaurant = excluded.restaurant
        """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            stmt.setString(2, item.getItemId());
            stmt.setString(3, item.getName());
            stmt.setDouble(4, item.getPrice());
            stmt.setString(5, item.getCategory());
            stmt.setString(6, item.getRestaurant());
            stmt.setInt(7, item.getQuantity());
            
            stmt.executeUpdate();
            updateCartTimestamp(sessionId);
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add item to cart", e);
        }
    }

    /**
     * Update item quantity in cart.
     */
    public boolean updateQuantity(String sessionId, String itemId, int quantity) {
        if (sessionId == null || sessionId.isBlank() || itemId == null || quantity <= 0) {
            return false;
        }
        
        String sql = "UPDATE cart_items SET quantity = ? WHERE session_id = ? AND item_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, quantity);
            stmt.setString(2, sessionId);
            stmt.setString(3, itemId);
            
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                updateCartTimestamp(sessionId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update quantity", e);
        }
    }

    /**
     * Remove an item from cart.
     */
    public boolean removeFromCart(String sessionId, String itemId) {
        if (sessionId == null || sessionId.isBlank() || itemId == null) {
            return false;
        }
        
        String sql = "DELETE FROM cart_items WHERE session_id = ? AND item_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            stmt.setString(2, itemId);
            
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                updateCartTimestamp(sessionId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove item from cart", e);
        }
    }

    /**
     * Clear all items from cart.
     */
    public void clearCart(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        
        String sql = "DELETE FROM cart_items WHERE session_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            stmt.executeUpdate();
            updateCartTimestamp(sessionId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear cart", e);
        }
    }

    /**
     * Save a cart snapshot (Memento pattern).
     */
    public String saveCartSnapshot(String sessionId, Integer userId, CartMemento memento) {
        if (sessionId == null || sessionId.isBlank() || memento == null) {
            throw new IllegalArgumentException("Invalid snapshot parameters");
        }
        
        // Generate token
        String token = mementoToToken(memento);
        
        String sql = """
            INSERT INTO cart_snapshots (session_id, user_id, token, cart_data)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            if (userId != null) stmt.setInt(2, userId); else stmt.setNull(2, java.sql.Types.INTEGER);
            stmt.setString(3, token);
            stmt.setString(4, token); // cart_data stores the token for now
            
            stmt.executeUpdate();
            return token;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save cart snapshot", e);
        }
    }

    /**
     * Get all snapshots for a user.
     */
    public List<CartSnapshot> getSnapshotsForUser(Integer userId) {
        List<CartSnapshot> snapshots = new ArrayList<>();
        String sql = "SELECT id, session_id, user_id, token, created_at FROM cart_snapshots WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    snapshots.add(new CartSnapshot(
                        rs.getInt("id"),
                        rs.getString("session_id"),
                        rs.getInt("user_id"),
                        rs.getString("token"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load snapshots", e);
        }
        return snapshots;
    }

    /**
     * Restore cart from snapshot.
     */
    public boolean restoreCart(String sessionId, String token) {
        if (sessionId == null || sessionId.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        
        // Verify snapshot exists
        String checkSql = "SELECT 1 FROM cart_snapshots WHERE token = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to verify snapshot", e);
        }
        
        // Parse token and restore
        CartMemento memento = tokenToMemento(token);
        if (memento == null) return false;
        
        // Clear current cart and restore
        clearCart(sessionId);
        for (CartItem item : memento.getSavedItems()) {
            addToCart(sessionId, item);
        }
        return true;
    }

    private void ensureCartExists(String sessionId) {
        String sql = """
            INSERT OR IGNORE INTO carts (session_id) VALUES (?)
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to ensure cart exists", e);
        }
    }

    /**
     * Associate a session with a user ID (called on login).
     * Updates the carts table to link the session with the user.
     */
    public void associateSessionWithUser(String sessionId, int userId) {
        String sql = "UPDATE carts SET user_id = ? WHERE session_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to associate session with user", e);
        }
    }

    private void updateCartTimestamp(String sessionId) {
        String sql = "UPDATE carts SET updated_at = datetime('now') WHERE session_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Non-critical
        }
    }

    /**
     * Get cart items by user ID (for login persistence).
     * Returns items from the most recent cart associated with the user.
     */
    public List<CartItem> getCartItemsByUserId(int userId) {
        List<CartItem> items = new ArrayList<>();
        String sql = """
            SELECT ci.item_id, ci.name, ci.price, ci.category, ci.restaurant, ci.quantity
            FROM cart_items ci
            JOIN carts c ON ci.session_id = c.session_id
            WHERE c.user_id = ?
            ORDER BY ci.item_id
        """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new CartItem(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("category"),
                        rs.getString("restaurant"),
                        rs.getInt("quantity")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load cart for user: " + userId, e);
        }
        
        return items;
    }

    /**
     * Transfer cart from old session to new session (on login).
     */
    public void transferCart(String oldSessionId, String newSessionId) {
        if (oldSessionId == null || oldSessionId.isBlank() || 
            newSessionId == null || newSessionId.isBlank() ||
            oldSessionId.equals(newSessionId)) {
            return;
        }
        
        // Ensure new session cart exists
        ensureCartExists(newSessionId);
        
        // Get items from old session
        List<CartItem> oldItems = getCartItems(oldSessionId);
        if (oldItems.isEmpty()) return;
        
        // Add each item to new session
        for (CartItem item : oldItems) {
            addToCart(newSessionId, item);
        }
    }

    /**
     * Load user's cart from database and create a new session cart.
     * Used on login to restore user's persistent cart.
     */
    public void loadUserCartForSession(int userId, String newSessionId) {
        if (userId <= 0 || newSessionId == null || newSessionId.isBlank()) return;
        
        List<CartItem> userItems = getCartItemsByUserId(userId);
        if (userItems.isEmpty()) return;
        
        ensureCartExists(newSessionId);
        
        for (CartItem item : userItems) {
            addToCart(newSessionId, item);
        }
    }

    private String mementoToToken(CartMemento memento) {
        StringBuilder sb = new StringBuilder("cart:");
        List<CartItem> items = memento.getSavedItems();
        for (int i = 0; i < items.size(); i++) {
            CartItem it = items.get(i);
            sb.append(it.getItemId()).append(":").append(it.getName()).append(":")
              .append(it.getPrice()).append(":").append(it.getQuantity());
            if (i < items.size() - 1) sb.append("|");
        }
        return sb.toString();
    }

    private CartMemento tokenToMemento(String token) {
        if (token == null || token.isBlank() || !token.startsWith("cart:")) {
            return null;
        }
        String body = token.substring(5);
        if (body.isEmpty()) return null;
        
        // Create a temporary cart to build memento
        memento.ShoppingCart tempCart = new memento.ShoppingCart();
        boolean hasValidItem = false;
        
        for (String part : body.split("\\|")) {
            String[] fields = part.split(":");
            if (fields.length >= 4) {
                try {
                    tempCart.addItem(new CartItem(fields[0], fields[1],
                            Double.parseDouble(fields[2]),
                            Integer.parseInt(fields[3])));
                    hasValidItem = true;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        if (!hasValidItem) return null;
        return tempCart.save();
    }

    /**
     * Cart snapshot DTO.
     */
    public static class CartSnapshot {
        public final int id;
        public final String sessionId;
        public final int userId;
        public final String token;
        public final String createdAt;

        public CartSnapshot(int id, String sessionId, int userId, String token, String createdAt) {
            this.id = id;
            this.sessionId = sessionId;
            this.userId = userId;
            this.token = token;
            this.createdAt = createdAt;
        }
    }
}