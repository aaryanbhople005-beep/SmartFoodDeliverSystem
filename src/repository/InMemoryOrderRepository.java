package repository;

import models.Order;
import observer.OrderStatus;
import observer.OrderStatusSubject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of OrderRepository.
 *
 * IMPORTANT: this is TEMPORARY data. It is NOT a database. It exists only so the
 * browser interface can demonstrate end-to-end functionality while JDBC/DAO
 * integration remains a future step. A future DatabaseOrderRepository (JDBC/DAO)
 * can replace this class because the service layer depends only on the
 * OrderRepository interface.
 *
 * Each stored order is paired with an OrderStatusSubject (Observer pattern) so the
 * existing Observer implementation can drive real status updates.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Integer, Order> orders = new HashMap<>();
    private final Map<Integer, OrderStatusSubject> subjects = new HashMap<>();
    private final Map<Integer, Integer> orderUserId = new HashMap<>();

    @Override
    public Order save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null.");
        }
        orders.put(order.getOrderId(), order);
        subjects.putIfAbsent(order.getOrderId(),
                new OrderStatusSubject(order.getOrderId(), OrderStatus.PLACED));
        return order;
    }

    @Override
    public Order findById(int orderId) {
        return orders.get(orderId);
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    @Override
    public void updateStatus(int orderId, OrderStatus status) {
        OrderStatusSubject subject = subjects.get(orderId);
        if (subject != null) {
            subject.setStatus(status);
        }
    }

    @Override
    public OrderStatus getStatus(int orderId) {
        OrderStatusSubject subject = subjects.get(orderId);
        return subject == null ? null : subject.getStatus();
    }

    public OrderStatusSubject getSubject(int orderId) {
        return subjects.get(orderId);
    }

    @Override
    public void associateUser(int orderId, int userId) {
        orderUserId.put(orderId, userId);
    }

    @Override
    public Integer getOrderUserId(int orderId) {
        return orderUserId.get(orderId);
    }

    public List<Order> getOrders() {
        return Collections.unmodifiableList(new ArrayList<>(orders.values()));
    }
}