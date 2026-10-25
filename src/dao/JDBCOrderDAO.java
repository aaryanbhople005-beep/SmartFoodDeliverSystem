
package dao;

import database.DBConnection;
import models.ComboOrder;
import models.FoodItem;
import models.NonVegOrder;
import models.Order;
import models.VegOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of the order DAO using SQLite.
 *
 * All statements are prepared statements. The orders table persists the
 * order's customer, type, item (id/name/price/category/restaurant), quantity
 * and status so orders survive a server restart.
 *
 * The schema uses CREATE TABLE IF NOT EXISTS and never drops existing tables or
 * data.
 */
public class JDBCOrderDAO implements OrderDAO {

    public JDBCOrderDAO() throws SQLException {
        createTable();
        migrate();
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS orders ("
                + "order_id INTEGER PRIMARY KEY, "
                + "customer_name TEXT NOT NULL, "
                + "order_type TEXT NOT NULL, "
                + "item_id TEXT NOT NULL, "
                + "item_name TEXT NOT NULL, "
                + "price REAL NOT NULL, "
                + "category TEXT, "
                + "restaurant TEXT, "
                + "quantity INTEGER NOT NULL, "
                + "status TEXT NOT NULL DEFAULT 'PLACED', "
                + "user_id INTEGER"
                + ")";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Safe migration for databases created by earlier versions of this DAO.
     * Adds columns that did not exist before without dropping any data.
     */
    private void migrate() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(orders)")) {
            boolean hasUserId = false;
            boolean hasStatus = false;
            while (rs.next()) {
                String col = rs.getString("name");
                if ("user_id".equals(col)) hasUserId = true;
                if ("status".equals(col)) hasStatus = true;
            }
            if (!hasUserId) {
                stmt.execute("ALTER TABLE orders ADD COLUMN user_id INTEGER");
            }
            if (!hasStatus) {
                stmt.execute("ALTER TABLE orders ADD COLUMN status TEXT NOT NULL DEFAULT 'PLACED'");
            }
        }
    }

    @Override
    public void saveOrder(Order order) throws SQLException {
        String sql = "INSERT OR REPLACE INTO orders "
                + "(order_id, customer_name, order_type, item_id, "
                + "item_name, price, category, restaurant, quantity, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        FoodItem item = order.getFoodItem();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, order.getOrderId());
            stmt.setString(2, order.getCustomerName());
            stmt.setString(3, order.getOrderType());
            stmt.setString(4, item.getItemId());
            stmt.setString(5, item.getName());
            stmt.setDouble(6, item.getPrice());
            stmt.setString(7, item.getCategory());
            stmt.setString(8, item.getRestaurant());
            stmt.setInt(9, order.getQuantity());
            stmt.setString(10, orderStatusToDb(order));

            stmt.executeUpdate();
        }
    }

    public Order findOrderById(int orderId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return toOrder(rs);
                }
                return null;
            }
        }
    }

    public void updateStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        }
    }

    public String getStatus(int orderId) throws SQLException {
        String sql = "SELECT status FROM orders WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
                return null;
            }
        }
    }

    public void setOrderUserId(int orderId, int userId) throws SQLException {
        String sql = "UPDATE orders SET user_id = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        }
    }

    public Integer getOrderUserId(int orderId) throws SQLException {
        String sql = "SELECT user_id FROM orders WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("user_id", Integer.class);
                }
                return null;
            }
        }
    }

    @Override
    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_id";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(toOrder(rs));
            }
        }
        return orders;
    }

    private Order toOrder(ResultSet rs) throws SQLException {
        FoodItem item = new FoodItem(
                rs.getString("item_id"),
                rs.getString("item_name"),
                rs.getDouble("price"),
                rs.getString("category"),
                rs.getString("restaurant")
        );

        int orderId = rs.getInt("order_id");
        String customerName = rs.getString("customer_name");
        String orderType = rs.getString("order_type");
        int quantity = rs.getInt("quantity");

        Order order;
        if ("Veg".equals(orderType)) {
            order = new VegOrder(orderId, customerName, item, quantity);
        } else if ("Non-Veg".equals(orderType)) {
            order = new NonVegOrder(orderId, customerName, item, quantity);
        } else if ("Combo".equals(orderType)) {
            order = new ComboOrder(orderId, customerName, item, quantity);
        } else {
            throw new SQLException("Unknown order type: " + orderType);
        }
        return order;
    }

    private String orderStatusToDb(Order order) {
        // The Order model does not expose a status field; default to PLACED.
        // Status is tracked independently by OrderStatusSubject / repository.
        return "PLACED";
    }
}
