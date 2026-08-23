package factory;

import models.ComboOrder;
import models.FoodItem;
import models.Order;

public class ComboOrderCreator extends OrderCreator {
    @Override
    public Order createOrder(
            int orderId,
            String customerName,
            FoodItem foodItem,
            int quantity
    ) {
        return new ComboOrder(orderId, customerName, foodItem, quantity);
    }
}
