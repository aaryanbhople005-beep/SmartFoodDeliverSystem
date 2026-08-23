package models;

public class ComboOrder extends Order {

    public ComboOrder(int orderId, String customerName, double amount) {
        super(orderId, customerName, amount);
    }

    @Override
    public String getOrderType() {
        return "COMBO";
    }
}