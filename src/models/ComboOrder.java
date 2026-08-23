package models;

public class ComboOrder extends Order {
    public ComboOrder(int orderId, String customerName, FoodItem foodItem, int quantity) {
        super(orderId, customerName, foodItem, quantity);
    }

    @Override
    public String getOrderType() {
        return "Combo";
    }
}
