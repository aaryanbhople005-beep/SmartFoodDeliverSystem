package models;

public class NonVegOrder extends Order {
    public NonVegOrder(int orderId, String customerName, FoodItem foodItem, int quantity) {
        super(orderId, customerName, foodItem, quantity);
    }

    @Override
    public String getOrderType() {
        return "Non-Veg";
    }
}
