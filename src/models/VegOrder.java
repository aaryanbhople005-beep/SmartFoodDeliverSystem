package models;

public class VegOrder extends Order {
    public VegOrder(int orderId, String customerName, FoodItem foodItem, int quantity) {
        super(orderId, customerName, foodItem, quantity);
    }

    @Override
    public String getOrderType() {
        return "Veg";
    }
}
