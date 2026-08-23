// package models;

// public class Order {
// }
package models;

public abstract class Order {

    protected int orderId;
    protected String customerName;
    protected double amount;

    public Order(int orderId, String customerName, double amount) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.amount = amount;
    }

    public abstract String getOrderType();

    public int getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public double getAmount() {
        return amount;
    }
}
