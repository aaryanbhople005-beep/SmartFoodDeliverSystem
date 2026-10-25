
package builder;

import models.ComboOrder;
import models.FoodItem;
import models.NonVegOrder;
import models.Order;
import models.VegOrder;

public class OrderBuilder {

    public enum OrderType {
        VEG, NON_VEG, COMBO
    }

    private int orderId;
    private String customerName;
    private FoodItem foodItem;
    private int quantity;
    private OrderType orderType;

    public OrderBuilder orderId(int orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderBuilder customerName(String customerName) {
        this.customerName = customerName;
        return this;
    }

    public OrderBuilder foodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
        return this;
    }

    public OrderBuilder quantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    public OrderBuilder orderType(OrderType orderType) {
        this.orderType = orderType;
        return this;
    }

    public Order build() {
        if (orderId <= 0 || quantity <= 0) {
            throw new IllegalArgumentException(
                "Order ID and quantity must be positive.");
        }

        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Customer name is required.");
        }

        if (foodItem == null || orderType == null) {
            throw new IllegalArgumentException(
                "Food item and order type are required.");
        }

        switch (orderType) {
            case VEG:
                return new VegOrder(
                    orderId, customerName, foodItem, quantity);

            case NON_VEG:
                return new NonVegOrder(
                    orderId, customerName, foodItem, quantity);

            case COMBO:
                return new ComboOrder(
                    orderId, customerName, foodItem, quantity);

            default:
                throw new IllegalStateException("Invalid order type.");
        }
    }
}
