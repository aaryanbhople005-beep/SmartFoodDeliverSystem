package factory;

import models.FoodItem;
import models.Order;
import models.VegOrder;

public class VegOrderCreator extends OrderCreator {
    @Override
    public Order createOrder(
            int orderId,
            String customerName,
            FoodItem foodItem,
            int quantity
    ) {
        return new VegOrder(orderId, customerName, foodItem, quantity);
    }
}
