package chain;

import models.Order;

public class DeliveryHandler extends OrderHandler {
    @Override
    public boolean validate(Order order) {
        System.out.println("Validating delivery details for customer: " + order.getCustomerName());
        if (order.getCustomerName() == null || order.getCustomerName().isEmpty()) {
            System.out.println("Delivery validation failed: Customer details missing.");
            return false;
        }
        System.out.println("Delivery validation passed.");
        return checkNext(order);
    }
}
