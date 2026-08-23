package models;

public abstract class Order {
    protected int orderId;
    protected String customerName;
    protected String restaurantName;
    protected FoodItem foodItem;
    protected int quantity;
    protected double amount;

    public Order(int orderId, String customerName, FoodItem foodItem, int quantity) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.foodItem = foodItem;
        this.quantity = quantity;
        this.restaurantName = foodItem.getRestaurant();
        this.amount = foodItem.getPrice() * quantity;
    }

    public abstract String getOrderType();

    public int getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAmount() {
        return amount;
    }
}
