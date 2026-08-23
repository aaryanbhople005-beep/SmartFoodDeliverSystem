package chain;

import models.FoodItem;
import models.Order;

public class RestaurantHandler extends OrderHandler {


@Override
public boolean validate(Order order) {

    if (order.getRestaurantName() == null ||
        order.getRestaurantName().isEmpty()) {

        System.out.println("✗ Restaurant validation failed.");
        return false;
    }

    FoodItem item = order.getFoodItem();

    if (item == null) {
        System.out.println("✗ Food item validation failed.");
        return false;
    }

    System.out.println("✓ Restaurant and food item validated.");
    return checkNext(order);
}


}
