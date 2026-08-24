import abstractfactory.PizzaHubFactory;
import abstractfactory.RestaurantFactory;
import abstractfactory.SpiceHubFactory;
import chain.DeliveryHandler;
import chain.OrderHandler;
import chain.PaymentHandler;
import chain.RestaurantHandler;
import factory.ComboOrderCreator;
import factory.NonVegOrderCreator;
import factory.OrderCreator;
import factory.VegOrderCreator;
import models.FoodItem;
import models.Order;
import proxy.PaymentGateway;
import proxy.PaymentGatewayProxy;
import singleton.OrderManager;
import java.util.Scanner;

public class Main {

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("       SMART FOOD DELIVERY SYSTEM");
        System.out.println("========================================");

        System.out.print("Enter Order ID: ");
        int orderId = readInt();

        System.out.print("Enter Customer Name: ");
        String customerName = readString();

        RestaurantFactory factory = null;
        String restaurantName = null;

        while (factory == null) {
            System.out.println("\n---------- CHOOSE RESTAURANT ----------");
            System.out.println("1. Pizza Hub");
            System.out.println("2. Spice Hub");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            int choice = readInt();

            if (choice == 0) {
                System.out.println("\n========================================");
                System.out.println("       ORDER PROCESS COMPLETED");
                System.out.println("       THANK YOU FOR ORDERING!");
                System.out.println("========================================");
                scanner.close();
                return;
            } else if (choice == 1) {
                factory = new PizzaHubFactory();
                restaurantName = "Pizza Hub";
            } else if (choice == 2) {
                factory = new SpiceHubFactory();
                restaurantName = "Spice Hub";
            } else {
                System.out.println("[ERROR] Invalid choice. Please try again.");
            }
        }

        Order order = startOrderFlow(orderId, customerName, factory, restaurantName);

        scanner.close();
    }

    private static Order startOrderFlow(int orderId, String customerName, RestaurantFactory factory, String restaurantName) {
        OrderCreator creator = null;
        String category = null;
        String orderTypeLabel = null;

        while (creator == null) {
            System.out.println("\n----------- CHOOSE ORDER TYPE -----------");
            System.out.println("1. Veg");
            System.out.println("2. Non-Veg");
            System.out.println("3. Combo");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            int choice = readInt();

            if (choice == 0) {
                return mainMenuLoop(orderId, customerName);
            } else if (choice == 1) {
                creator = new VegOrderCreator();
                category = "VEG";
                orderTypeLabel = "VEG";
            } else if (choice == 2) {
                creator = new NonVegOrderCreator();
                category = "NON-VEG";
                orderTypeLabel = "NON-VEG";
            } else if (choice == 3) {
                creator = new ComboOrderCreator();
                category = "COMBO";
                orderTypeLabel = "COMBO";
            } else {
                System.out.println("[ERROR] Invalid choice. Please try again.");
            }
        }

        FoodItem selectedItem = null;
        int quantity = 0;
        double totalAmount = 0.0;

        while (selectedItem == null) {
            FoodItem[] menuItems = factory.getMenu(category);

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
            int itemChoice = readInt();

            if (itemChoice == 0) {
                return startOrderFlow(orderId, customerName, factory, restaurantName);
            }

            if (itemChoice < 1 || itemChoice > menuItems.length) {
                System.out.println("[ERROR] Invalid item choice. Please try again.");
                continue;
            }

            selectedItem = menuItems[itemChoice - 1];

            System.out.print("Enter Quantity: ");
            quantity = readInt();

            if (quantity <= 0) {
                System.out.println("[ERROR] Invalid quantity. Please enter a quantity greater than 0.");
                selectedItem = null;
            }
        }

        totalAmount = selectedItem.getPrice() * quantity;

        System.out.println("\n---");
        System.out.println("          ITEM SELECTED");
        System.out.println("---");
        System.out.println("Item     : " + selectedItem.getName());
        System.out.printf("Price    : Rs.%.2f%n", selectedItem.getPrice());
        System.out.println("Quantity : " + quantity);
        System.out.printf("Total    : Rs.%.2f%n", totalAmount);
        System.out.println("---");

        boolean confirmed = false;

        while (!confirmed) {
            System.out.println("\n========================================");
            System.out.println("             ORDER REVIEW");
            System.out.println("========================================");
            System.out.println("Order ID   : " + orderId);
            System.out.println("Customer   : " + customerName);
            System.out.println("Restaurant : " + restaurantName);
            System.out.println("Order Type : " + orderTypeLabel);
            System.out.println();
            System.out.println("Item       : " + selectedItem.getName());
            System.out.printf("Unit Price : Rs.%.2f%n", selectedItem.getPrice());
            System.out.println("Quantity   : " + quantity);
            System.out.printf("Total      : Rs.%.2f%n", totalAmount);
            System.out.println("========================================");
            System.out.println("1. Confirm Order");
            System.out.println("2. Change Item");
            System.out.println("3. Cancel Order");
            System.out.print("Choice: ");

            int reviewChoice = readInt();

            if (reviewChoice == 1) {
                confirmed = true;
            } else if (reviewChoice == 2) {
                return startOrderFlow(orderId, customerName, factory, restaurantName);
            } else if (reviewChoice == 3) {
                System.out.println("\nOrder cancelled.");
                return null;
            } else {
                System.out.println("[ERROR] Invalid choice. Please try again.");
            }
        }

        Order order = creator.createOrder(orderId, customerName, selectedItem, quantity);

        System.out.println("\n----------------------------------------");
        System.out.println("          FACTORY METHOD");
        System.out.println("----------------------------------------");
        System.out.println("[OK] " + order.getOrderType() + " order created successfully.");
        System.out.println("Order Type: " + order.getOrderType());

        System.out.println("\n----------------------------------------");
        System.out.println("           SINGLETON");
        System.out.println("----------------------------------------");
        OrderManager manager = OrderManager.getInstance();
        manager.manageOrder(order.getOrderType() + " Order #" + order.getOrderId());
        System.out.println("[OK] Order registered successfully.");

        System.out.println("\n----------------------------------------");
        System.out.println("  CHAIN OF RESPONSIBILITY");
        System.out.println("----------------------------------------");

        OrderHandler paymentHandler = new PaymentHandler();
        OrderHandler restaurantHandler = new RestaurantHandler();
        OrderHandler deliveryHandler = new DeliveryHandler();

        paymentHandler
                .setNextHandler(restaurantHandler)
                .setNextHandler(deliveryHandler);

        boolean valid = paymentHandler.validate(order);

        if (!valid) {
            System.out.println("\n[ERROR] Order validation failed.");
            return null;
        }

        System.out.println("[OK] All order validations passed.");

        String paymentMethod = choosePaymentMethod(totalAmount);

        if (paymentMethod == null) {
            System.out.println("\nOrder cancelled during payment.");
            return null;
        }

        System.out.println("\n----------------------------------------");
        System.out.println("       PROXY DESIGN PATTERN");
        System.out.println("----------------------------------------");
        PaymentGateway gateway = new PaymentGatewayProxy();

        boolean paymentSuccess = handleProxyPayment(gateway, order, paymentMethod);

        if (!paymentSuccess) {
            System.out.println("\n========================================");
            System.out.println("             ORDER CANCELLED");
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
            System.out.println("Payment Status    : FAILED");
            System.out.println("Order Status      : CANCELLED");
            System.out.println("========================================");
            System.out.println("       ORDER PROCESS COMPLETED");
            System.out.println("       THANK YOU FOR ORDERING!");
            System.out.println("=======================");
            return order;
        }

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
        System.out.println("Payment Status    : SUCCESS");
        System.out.println("Order Status      : CONFIRMED");
        System.out.println("Estimated Delivery: 30-40 minutes");
        System.out.println("========================================");
        System.out.println("       ORDER PROCESS COMPLETED");
        System.out.println("       THANK YOU FOR ORDERING!");
        System.out.println("=======================");

        return order;
    }

    private static Order mainMenuLoop(int orderId, String customerName) {
        RestaurantFactory factory = null;
        String restaurantName = null;

        while (factory == null) {
            System.out.println("\n---------- CHOOSE RESTAURANT ----------");
            System.out.println("1. Pizza Hub");
            System.out.println("2. Spice Hub");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            int choice = readInt();

            if (choice == 0) {
                System.out.println("\n========================================");
                System.out.println("       ORDER PROCESS COMPLETED");
                System.out.println("       THANK YOU FOR ORDERING!");
                System.out.println("========================================");
                scanner.close();
                return null;
            } else if (choice == 1) {
                factory = new PizzaHubFactory();
                restaurantName = "Pizza Hub";
            } else if (choice == 2) {
                factory = new SpiceHubFactory();
                restaurantName = "Spice Hub";
            } else {
                System.out.println("[ERROR] Invalid choice. Please try again.");
            }
        }

        return startOrderFlow(orderId, customerName, factory, restaurantName);
    }

    private static String choosePaymentMethod(double totalAmount) {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("               PAYMENT");
            System.out.println("========================================");
            System.out.printf("Amount Payable : Rs.%.2f%n", totalAmount);
            System.out.println("1. UPI");
            System.out.println("2. Card");
            System.out.println("3. Cash on Delivery");
            System.out.println("0. Cancel");
            System.out.print("Choice: ");

            int choice = readInt();

            if (choice == 0) {
                return null;
            } else if (choice == 1) {
                System.out.print("Enter UPI ID: ");
                String upiId = readString();
                System.out.println("[INFO] UPI payment method selected: " + upiId);
                return "UPI (" + upiId + ")";
            } else if (choice == 2) {
                System.out.print("Enter last 4 digits of card: ");
                String last4 = readString();
                System.out.println("[INFO] Card payment method selected: ****" + last4);
                return "CARD (****" + last4 + ")";
            } else if (choice == 3) {
                return "CASH ON DELIVERY";
            } else {
                System.out.println("[ERROR] Invalid choice. Please try again.");
            }
        }
    }

    private static boolean handleProxyPayment(PaymentGateway gateway, Order order, String paymentMethod) {
        if (paymentMethod.startsWith("CASH ON DELIVERY")) {
            System.out.println("[OK] Cash on Delivery selected.");
            System.out.println("[OK] Order confirmed for payment on delivery.");
            return true;
        }

        return gateway.processPayment(order);
    }

    private static int readInt() {
        while (!scanner.hasNextInt()) {
            System.out.println("[ERROR] Invalid input. Please enter a number.");
            scanner.next();
            System.out.print("Choice: ");
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    private static String readString() {
        String value = scanner.nextLine().trim();
        while (value.isEmpty()) {
            System.out.print("[ERROR] Input cannot be empty. Please enter a value: ");
            value = scanner.nextLine().trim();
        }
        return value;
    }
}
