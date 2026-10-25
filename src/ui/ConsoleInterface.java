package ui;

import models.FoodItem;

import java.util.Scanner;

/**
 * ConsoleInterface is the console-based implementation of {@link OrderInterface}.
 *
 * It holds the {@link Scanner} used for reading user input and performs all
 * System.out printing for the SmartFoodDeliverySystem console application.
 *
 * This class keeps the presentation logic in one place so that Main.java can
 * focus on the application flow and the existing design patterns
 * (Singleton, Factory Method, Abstract Factory, Chain of Responsibility, Proxy).
 */
public class ConsoleInterface implements OrderInterface {

    private final Scanner scanner = new Scanner(System.in);

    /* ---------- Welcome and input ---------- */

    @Override
    public void displayWelcome() {
        System.out.println("========================================");
        System.out.println("       SMART FOOD DELIVERY SYSTEM");
        System.out.println("========================================");
    }

    @Override
    public int readOrderId() {
        System.out.print("Enter Order ID: ");
        return readInt();
    }

    @Override
    public String readCustomerName() {
        System.out.print("Enter Customer Name: ");
        return readString();
    }

    /* ---------- Restaurant selection ---------- */

    @Override
    public int chooseRestaurant() {
        System.out.println("\n---------- CHOOSE RESTAURANT ----------");
        System.out.println("1. Pizza Hub");
        System.out.println("2. Spice Hub");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
        return readInt();
    }

    /* ---------- Order type selection ---------- */

    @Override
    public int chooseOrderType() {
        System.out.println("\n----------- CHOOSE ORDER TYPE -----------");
        System.out.println("1. Veg");
        System.out.println("2. Non-Veg");
        System.out.println("3. Combo");
        System.out.println("0. Back");
        System.out.print("Choice: ");
        return readInt();
    }

    /* ---------- Menu display ---------- */

    @Override
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

        System.out.print("\nSelect Item: ");
    }

    /* ---------- Item and quantity selection ---------- */

    @Override
    public int chooseItem(int menuSize) {
        return readInt();
    }

    @Override
    public int readQuantity() {
        System.out.print("Enter Quantity: ");
        return readInt();
    }

    /* ---------- Item selected display ---------- */

    @Override
    public void displayItemSelected(FoodItem item, int quantity, double total) {
        System.out.println("\n---");
        System.out.println("          ITEM SELECTED");
        System.out.println("---");
        System.out.println("Item     : " + item.getName());
        System.out.printf("Price    : Rs.%.2f%n", item.getPrice());
        System.out.println("Quantity : " + quantity);
        System.out.printf("Total    : Rs.%.2f%n", total);
        System.out.println("---");
    }

    /* ---------- Order review ---------- */

    @Override
    public int reviewOrder(int orderId, String customerName, String restaurantName,
                           String orderType, FoodItem item, int quantity, double total) {
        System.out.println("\n========================================");
        System.out.println("             ORDER REVIEW");
        System.out.println("========================================");
        System.out.println("Order ID   : " + orderId);
        System.out.println("Customer   : " + customerName);
        System.out.println("Restaurant : " + restaurantName);
        System.out.println("Order Type : " + orderType);
        System.out.println();
        System.out.println("Item       : " + item.getName());
        System.out.printf("Unit Price : Rs.%.2f%n", item.getPrice());
        System.out.println("Quantity   : " + quantity);
        System.out.printf("Total      : Rs.%.2f%n", total);
        System.out.println("========================================");
        System.out.println("1. Confirm Order");
        System.out.println("2. Change Item");
        System.out.println("3. Cancel Order");
        System.out.print("Choice: ");
        return readInt();
    }

    /* ---------- Payment ---------- */

    @Override
    public int choosePaymentMethod(double totalAmount) {
        System.out.println("\n========================================");
        System.out.println("               PAYMENT");
        System.out.println("========================================");
        System.out.printf("Amount Payable : Rs.%.2f%n", totalAmount);
        System.out.println("1. UPI");
        System.out.println("2. Card");
        System.out.println("3. Cash on Delivery");
        System.out.println("0. Cancel");
        System.out.print("Choice: ");
        return readInt();
    }

    @Override
    public String readUpiId() {
        System.out.print("Enter UPI ID: ");
        return readString();
    }

    @Override
    public String readCardLast4() {
        System.out.print("Enter last 4 digits of card: ");
        return readString();
    }

    /* ---------- Results and summary ---------- */

    @Override
    public void displayPaymentResult(boolean success, String paymentMethod) {
        System.out.println("\n========================================");
        System.out.println("             ORDER CONFIRMED");
        System.out.println("========================================");
        System.out.println("Payment Method    : " + paymentMethod);
        System.out.println("Payment Status    : " + (success ? "SUCCESS" : "PAY ON DELIVERY"));
    }

    @Override
    public void displayOrderConfirmed(int orderId, String customerName, String restaurantName,
                                      String orderType, FoodItem item, int quantity, double unitPrice,
                                      double amount, String paymentMethod, boolean paymentSuccess) {
        System.out.println("\n========================================");
        System.out.println("             ORDER CONFIRMED");
        System.out.println("========================================");
        System.out.println("Order ID          : " + orderId);
        System.out.println("Customer          : " + customerName);
        System.out.println("Restaurant        : " + restaurantName);
        System.out.println();
        System.out.println("Item              : " + item.getName());
        System.out.println("Order Type        : " + orderType);
        System.out.println("Quantity          : " + quantity);
        System.out.printf("Unit Price        : Rs.%.2f%n", unitPrice);
        System.out.printf("Total Amount      : Rs.%.2f%n", amount);
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

    @Override
    public void displayCancellation() {
        System.out.println("\nOrder cancelled.");
    }

    @Override
    public void displayThankYou() {
        System.out.println("\n========================================");
        System.out.println("       ORDER PROCESS COMPLETED");
        System.out.println("       THANK YOU FOR ORDERING!");
        System.out.println("========================================");
    }

    @Override
    public void displayError(String message) {
        System.out.println("[ERROR] " + message);
    }

    @Override
    public void close() {
        scanner.close();
    }

    /* ---------- Helper input methods ---------- */

    private int readInt() {
        while (!scanner.hasNextInt()) {
            System.out.println("[ERROR] Invalid input. Please enter a number.");
            scanner.next();
            System.out.print("Choice: ");
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    private String readString() {
        String value = scanner.nextLine().trim();
        while (value.isEmpty()) {
            System.out.print("[ERROR] Input cannot be empty. Please enter a value: ");
            value = scanner.nextLine().trim();
        }
        return value;
    }
}
