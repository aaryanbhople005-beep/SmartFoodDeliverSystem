package factory;

import models.FoodItem;
import models.NonVegOrder;
import models.Order;

public class NonVegOrderCreator extends OrderCreator {
    @Override
    public Order createOrder(
            int orderId,
            String customerName,
            FoodItem foodItem,
            int quantity
    ) {
        return new NonVegOrder(orderId, customerName, foodItem, quantity);
    }
}
