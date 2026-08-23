package models;

public class VegOrder extends Order {

    public VegOrder(int orderId, String customerName, double amount) {
        super(orderId, customerName, amount);
    }

    @Override
    public String getOrderType() {
        return "VEG";
    }
}