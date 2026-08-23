package factory;

import models.Order;
import models.FoodItem;

public abstract class OrderCreator {
    public abstract Order createOrder(
            int orderId,
            String customerName,
            FoodItem foodItem,
            int quantity
    );
}
