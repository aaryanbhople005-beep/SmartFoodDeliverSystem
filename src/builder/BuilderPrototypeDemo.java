
package builder;

import models.FoodItem;
import models.Order;
import prototype.Prototype;

public class BuilderPrototypeDemo {

    public static void main(String[] args) {

        FoodItem pizza = new FoodItem(
            "F101",
            "Margherita Pizza",
            249.00,
            "Veg",
            "Pizza Hub"
        );

        // Builder Pattern: construct an order step by step
        Order original = new OrderBuilder()
            .orderId(1001)
            .customerName("Aarav")
            .foodItem(pizza)
            .quantity(2)
            .orderType(OrderBuilder.OrderType.VEG)
            .build();

        System.out.println("=== BUILDER PATTERN ===");
        printOrder(original);

        // Prototype Pattern: clone the existing order
        Prototype<Order> prototype = original;
        Order copied = prototype.copy();

        System.out.println("\n=== PROTOTYPE PATTERN ===");
        printOrder(copied);

        System.out.println(
            "Separate order object: " + (original != copied));

        System.out.println(
            "Separate food item: "
            + (original.getFoodItem() != copied.getFoodItem()));
    }

    private static void printOrder(Order order) {
        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Customer: " + order.getCustomerName());
        System.out.println("Type: " + order.getOrderType());
        System.out.println("Food: " + order.getFoodItem().getName());
        System.out.println("Quantity: " + order.getQuantity());
        System.out.printf("Amount: Rs.%.2f%n", order.getAmount());
    }
}
