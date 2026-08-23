package models;

public class NonVegOrder extends Order {

    public NonVegOrder(int orderId, String customerName, double amount) {
        super(orderId, customerName, amount);
    }

    @Override
    public String getOrderType() {
        return "NON-VEG";
    }
}