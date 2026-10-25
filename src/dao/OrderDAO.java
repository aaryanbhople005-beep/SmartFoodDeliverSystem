
package dao;

import models.Order;
import java.sql.SQLException;
import java.util.List;

public interface OrderDAO {
    void saveOrder(Order order) throws SQLException;

    List<Order> getAllOrders() throws SQLException;
}
