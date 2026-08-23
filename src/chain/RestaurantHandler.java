package chain;

import models.Order;

public class RestaurantHandler extends OrderHandler {
    @Override
    public boolean validate(Order order) {
        System.out.println("Validating restaurant details for: " + order.getRestaurantName());
        if (order.getRestaurantName() == null || order.getRestaurantName().isEmpty()) {
            System.out.println("Restaurant validation failed: Restaurant not specified.");
            return false;
        }
        System.out.println("Restaurant validation passed.");
        return checkNext(order);
    }
}
