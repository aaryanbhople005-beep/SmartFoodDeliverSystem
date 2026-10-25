package repository;

import models.FoodItem;
import models.Restaurant;

import java.util.List;

/**
 * OrderRepository is the persistence abstraction for orders.
 *
 * This interface is the JDBC/DAO integration point: a future DatabaseOrderRepository
 * (JDBC/DAO) can replace the current InMemoryOrderRepository without any change
 * to the service or frontend layers, because both depend on this interface.
 */
public interface OrderRepository {

    models.Order save(models.Order order);

    models.Order findById(int orderId);

    List<models.Order> findAll();

    void updateStatus(int orderId, observer.OrderStatus status);

    observer.OrderStatus getStatus(int orderId);
}