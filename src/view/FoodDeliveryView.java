package view;

import models.FoodItem;
import models.Order;

import java.util.Scanner;

/**
 * View layer of the MVC architecture.
 * Responsible for all console-based presentation and user input.
 * Does not contain business workflow logic or coordinate design patterns.
 */
public class FoodDeliveryView {

    private final Scanner scanner = new Scanner(System.in);

    public void displayWelcome() {
        System.out.println("========================================");
        System.out.println("       SMART FOOD DELIVERY SYSTEM");
        System.out.println("========================================");
    }

    public void displayRestaurantMenu() {
        System.out.println("\n---------- CHOOSE RESTAURANT ----------");
        System.out.println("1. Pizza Hub");
        System.out.println("2. Spice Hub");
        System.out.println("0. Exit");
    }

    public void displayOrderTypeMenu() {
        System.out.println("\n----------- CHOOSE ORDER TYPE -----------");
        System.out.println("1. Veg");
        System.out.println("2. Non-Veg");
        System.out.println("3. Combo");
        System.out.println("0. Back");
    }

    public void displayMenu(String restaurantName, String category, FoodItem[] menuItems) {
        System.out.println("\n========================================");
        System.out.println("  " + restaurantName.toUpperCase() + " - " + category + " MENU");
        System.out.println("========================================");
        for (int i = 0; i < menuItems.length; i++) {
            System.out.printf("%d. %-6s %-28s Rs.%.2f%n",
                    (i + 1),
                    menuItems[i].getItemId(),
                    menuItems[i].getName(),
                    menuItems[i].getPrice());
        }
        System.out.println("0. Back");
    }

    public int readQuantity() {
        while (true) {
            int quantity = readInt("Enter Quantity: ");
            if (quantity > 0) {
                return quantity;
            }
            System.out.println("[ERROR] Invalid quantity. Please enter a quantity greater than 0.");
        }
    }

    public void displayItemSelected(FoodItem item, int quantity, double totalAmount) {
        System.out.println("\n---");
        System.out.println("          ITEM SELECTED");
        System.out.println("---");
        System.out.println("Item     : " + item.getName());
        System.out.printf("Price    : Rs.%.2f%n", item.getPrice());
        System.out.println("Quantity : " + quantity);
        System.out.printf("Total    : Rs.%.2f%n", totalAmount);
        System.out.println("---");
    }

    public int displayOrderReview(int orderId, String customerName, String restaurantName,
                                  String orderTypeLabel, FoodItem item, int quantity, double totalAmount) {
        System.out.println("\n========================================");
        System.out.println("             ORDER REVIEW");
        System.out.println("========================================");
        System.out.println("Order ID   : " + orderId);
        System.out.println("Customer   : " + customerName);
        System.out.println("Restaurant : " + restaurantName);
        System.out.println("Order Type : " + orderTypeLabel);
        System.out.println();
        System.out.println("Item       : " + item.getName());
        System.out.printf("Unit Price : Rs.%.2f%n", item.getPrice());
        System.out.println("Quantity   : " + quantity);
        System.out.printf("Total      : Rs.%.2f%n", totalAmount);
        System.out.println("========================================");
        System.out.println("1. Confirm Order");
        System.out.println("2. Change Item");
        System.out.println("3. Cancel Order");
        return readInt("Choice: ");
    }

    public void displayPaymentMenu(double totalAmount) {
        System.out.println("\n========================================");
        System.out.println("               PAYMENT");
        System.out.println("========================================");
        System.out.printf("Amount Payable : Rs.%.2f%n", totalAmount);
        System.out.println("1. UPI");
        System.out.println("2. Card");
        System.out.println("3. Cash on Delivery");
        System.out.println("0. Cancel");
    }

    public void displayUpiSelected(String upiId) {
        System.out.println("[INFO] UPI payment method selected: " + upiId);
    }

    public void displayCardSelected(String last4) {
        System.out.println("[INFO] Card payment method selected: ****" + last4);
    }

    public void displayCashOnDelivery() {
        System.out.println("[OK] Cash on Delivery selected.");
        System.out.println("[OK] Order confirmed for payment on delivery.");
    }

    public void displayChainValidationHeader() {
        System.out.println("\n----------------------------------------");
        System.out.println("  CHAIN OF RESPONSIBILITY");
        System.out.println("----------------------------------------");
    }

    public void displayProxySection() {
        System.out.println("\n----------------------------------------");
        System.out.println("       PROXY DESIGN PATTERN");
        System.out.println("----------------------------------------");
    }

    public void displayOrderCreated(Order order) {
        System.out.println("\n----------------------------------------");
        System.out.println("          FACTORY METHOD");
        System.out.println("----------------------------------------");
        System.out.println("[OK] " + order.getOrderType() + " order created successfully.");
        System.out.println("Order Type: " + order.getOrderType());
    }

    public void displayOrderRegistered() {
        System.out.println("\n----------------------------------------");
        System.out.println("           SINGLETON");
        System.out.println("----------------------------------------");
        System.out.println("[OK] Order registered successfully.");
    }

    public void displayValidationPassed() {
        System.out.println("[OK] All order validations passed.");
    }

    public void displayValidationFailed() {
        System.out.println("\n[ERROR] Order validation failed.");
    }

    public void displayPaymentCancelled() {
        System.out.println("\nOrder cancelled during payment.");
    }

    public void displayOrderCancelled() {
        System.out.println("\nOrder cancelled.");
    }

    public void displayError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public void displayExitMessage() {
        System.out.println("\n========================================");
        System.out.println("       ORDER PROCESS COMPLETED");
        System.out.println("       THANK YOU FOR ORDERING!");
        System.out.println("========================================");
    }

    public void displayOrderConfirmation(Order order, String customerName, String restaurantName,
                                         String orderTypeLabel, String paymentMethod, boolean paymentSuccess) {
        System.out.println("\n========================================");
        System.out.println("             ORDER CONFIRMED");
        System.out.println("========================================");
        System.out.println("Order ID          : " + order.getOrderId());
        System.out.println("Customer          : " + customerName);
        System.out.println("Restaurant        : " + restaurantName);
        System.out.println();
        System.out.println("Item              : " + order.getFoodItem().getName());
        System.out.println("Order Type        : " + orderTypeLabel);
        System.out.println("Quantity          : " + order.getQuantity());
        System.out.printf("Unit Price        : Rs.%.2f%n", order.getFoodItem().getPrice());
        System.out.printf("Total Amount      : Rs.%.2f%n", order.getAmount());
        System.out.println();
        System.out.println("Payment Method    : " + paymentMethod);
        System.out.println("Payment Status    : " + (paymentSuccess ? "SUCCESS" : "PAY ON DELIVERY"));
        System.out.println("Order Status      : CONFIRMED");
        System.out.println("Estimated Delivery: 30-40 minutes");
        System.out.println("========================================");
        System.out.println("       ORDER PROCESS COMPLETED");
        System.out.println("       THANK YOU FOR ORDERING!");
        System.out.println("=======================");
    }

    public int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.println("[ERROR] Invalid input. Please enter a number.");
            scanner.next();
            System.out.print(prompt);
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    public String readString(String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        while (value.isEmpty()) {
            System.out.print("[ERROR] Input cannot be empty. Please enter a value: ");
            value = scanner.nextLine().trim();
        }
        return value;
    }

    public void close() {
        scanner.close();
    }
}
