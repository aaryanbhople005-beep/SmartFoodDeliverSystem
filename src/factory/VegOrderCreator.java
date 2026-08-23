// package factory;

// public class VegOrderCreator extends OrderCreator {
// }
package factory;

import models.Order;
import models.VegOrder;

public class VegOrderCreator extends OrderCreator {

    @Override
    public Order createOrder(
            int orderId,
            String customerName,
            double amount
    ) {
        return new VegOrder(
                orderId,
                customerName,
                amount
        );
    }
}