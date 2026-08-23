// package factory;

// public class NonVegOrderCreator extends OrderCreator {
// }
package factory;

import models.Order;
import models.NonVegOrder;

public class NonVegOrderCreator extends OrderCreator {

    @Override
    public Order createOrder(
            int orderId,
            String customerName,
            double amount
    ) {
        return new NonVegOrder(
                orderId,
                customerName,
                amount
        );
    }
}