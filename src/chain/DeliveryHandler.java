package chain;

import models.Order;

public class DeliveryHandler extends OrderHandler {

@Override
public boolean validate(Order order) {

    if (order.getCustomerName() == null ||
        order.getCustomerName().isEmpty()) {

        System.out.println("✗ Delivery validation failed: Customer name missing.");
        return false;
    }

    System.out.println("✓ Customer and delivery details validated.");
    return checkNext(order);
}

}
