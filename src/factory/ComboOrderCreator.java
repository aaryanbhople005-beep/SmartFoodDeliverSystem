// package factory;

// public class ComboOrderCreator extends OrderCreator {
// }
package factory;

import models.Order;
import models.ComboOrder;

public class ComboOrderCreator extends OrderCreator {

    @Override
    public Order createOrder(
            int orderId,
            String customerName,
            double amount
    ) {
        return new ComboOrder(
                orderId,
                customerName,
                amount
        );
    }
}