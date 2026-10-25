package repository;

import models.Order;
import observer.OrderStatus;
import observer.OrderStatusSubject;

import dao.JDBCOrderDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JDBC-backed implementation of {@link OrderRepository}.
 *
 * Persists orders through {@link JDBCOrderDAO} using prepared statements, so
 * orders survive a server restart. Order status and order-item information are
 * stored in the database. Order status changes are also reflected in an
 * in-memory {@link OrderStatusSubject} registry so the Observer pattern keeps
 * working against persisted orders.
 *
 * This is the production persistence configuration. Use
 * {@link InMemoryOrderRepository} only for development/test mode.
 */
public class DatabaseOrderRepository implements OrderRepository {

    private final JDBCOrderDAO dao;
    private final ConcurrentMap<Integer, OrderStatusSubject> subjects = new ConcurrentHashMap<>();

    public DatabaseOrderRepository() {
        try {
            this.dao = new JDBCOrderDAO(); // creates/migrates the schema (IF NOT EXISTS)
            loadSubjects();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not initialize JDBC order persistence: " + e.getMessage(), e);
        }
    }

    private void loadSubjects() throws SQLException {
        for (Order order : dao.getAllOrders()) {
            subjects.putIfAbsent(order.getOrderId(),
                    new OrderStatusSubject(order.getOrderId(), OrderStatus.PLACED));
        }
    }

    @Override
    public Order save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null.");
        }
        try {
            dao.saveOrder(order);
            subjects.putIfAbsent(order.getOrderId(),
                    new OrderStatusSubject(order.getOrderId(), OrderStatus.PLACED));
            return order;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist order: " + e.getMessage(), e);
        }
    }

    @Override
    public Order findById(int orderId) {
        try {
            return dao.findOrderById(orderId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read order " + orderId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> findAll() {
        try {
            return new ArrayList<>(dao.getAllOrders());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read orders: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(int orderId, OrderStatus status) {
        try {
            dao.updateStatus(orderId, status.name());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update order status: " + e.getMessage(), e);
        }
        OrderStatusSubject subject = subjects.get(orderId);
        if (subject != null) {
            subject.setStatus(status); // notifies registered observers
        } else {
            subjects.putIfAbsent(orderId, new OrderStatusSubject(orderId, status));
        }
    }

    @Override
    public OrderStatus getStatus(int orderId) {
        try {
            String status = dao.getStatus(orderId);
            if (status == null) return null;
            return OrderStatus.valueOf(status);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read order status: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public OrderStatusSubject getSubject(int orderId) {
        return subjects.get(orderId);
    }

    @Override
    public void associateUser(int orderId, int userId) {
        try {
            dao.setOrderUserId(orderId, userId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to associate order with user: " + e.getMessage(), e);
        }
    }

    @Override
    public Integer getOrderUserId(int orderId) {
        try {
            return dao.getOrderUserId(orderId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read order owner: " + e.getMessage(), e);
        }
    }

    public List<Order> getOrders() {
        return findAll();
    }

    /** Exposed for diagnostics / tests. */
    public boolean jdbcAvailable() {
        return dao != null;
    }
}
