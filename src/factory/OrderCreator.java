// package factory;

// public abstract class OrderCreator {
// }
package factory;

import models.Order;

public abstract class OrderCreator {

    public abstract Order createOrder(
            int orderId,
            String customerName,
            double amount
    );
}