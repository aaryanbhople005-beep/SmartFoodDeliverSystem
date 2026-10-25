
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

public class JDBCOrderDAO implements OrderDAO {

    public JDBCOrderDAO() throws SQLException {
        createTable();
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
                + "quantity INTEGER NOT NULL"
                + ")";

        try (Connection conn = DBConnection.getConnection();
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public void saveOrder(Order order) throws SQLException {
        String sql = "INSERT OR REPLACE INTO orders "
                + "(order_id, customer_name, order_type, item_id, "
                + "item_name, price, category, restaurant, quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            stmt.executeUpdate();
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
                    order = new VegOrder(
                            orderId, customerName, item, quantity);
                } else if ("Non-Veg".equals(orderType)) {
                    order = new NonVegOrder(
                            orderId, customerName, item, quantity);
                } else if ("Combo".equals(orderType)) {
                    order = new ComboOrder(
                            orderId, customerName, item, quantity);
                } else {
                    throw new SQLException(
                            "Unknown order type: " + orderType);
                }

                orders.add(order);
            }
        }

        return orders;
    }
}
