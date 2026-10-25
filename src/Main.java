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
import ui.ConsoleInterface;
import ui.OrderInterface;

public class Main {

    private static OrderInterface ui = new ConsoleInterface();

    public static void main(String[] args) {
        ui.displayWelcome();

        int orderId = ui.readOrderId();
        String customerName = ui.readCustomerName();

        RestaurantFactory factory = null;
        String restaurantName = null;

        while (factory == null) {
            int choice = ui.chooseRestaurant();

            if (choice == 0) {
                ui.displayThankYou();
                ui.close();
                return;
            } else if (choice == 1) {
                factory = new PizzaHubFactory();
                restaurantName = "Pizza Hub";
            } else if (choice == 2) {
                factory = new SpiceHubFactory();
                restaurantName = "Spice Hub";
            } else {
                ui.displayError("Invalid choice. Please try again.");
            }
        }

        Order order = startOrderFlow(orderId, customerName, factory, restaurantName);

        ui.close();
    }

    private static Order startOrderFlow(int orderId, String customerName, RestaurantFactory factory, String restaurantName) {
        OrderCreator creator = null;
        String category = null;
        String orderTypeLabel = null;

        while (creator == null) {
            int choice = ui.chooseOrderType();

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
                ui.displayError("Invalid choice. Please try again.");
            }
        }

        FoodItem selectedItem = null;
        int quantity = 0;
        double totalAmount = 0.0;

        while (selectedItem == null) {
            FoodItem[] menuItems = factory.getMenu(category);

            ui.displayMenu(restaurantName, category, menuItems);

            int itemChoice = ui.chooseItem(menuItems.length);

            if (itemChoice == 0) {
                return startOrderFlow(orderId, customerName, factory, restaurantName);
            }

            if (itemChoice < 1 || itemChoice > menuItems.length) {
                ui.displayError("Invalid item choice. Please try again.");
                continue;
            }

            selectedItem = menuItems[itemChoice - 1];

            quantity = ui.readQuantity();

            if (quantity <= 0) {
                ui.displayError("Invalid quantity. Please enter a quantity greater than 0.");
                selectedItem = null;
            }
        }

        totalAmount = selectedItem.getPrice() * quantity;

        ui.displayItemSelected(selectedItem, quantity, totalAmount);

        boolean confirmed = false;

        while (!confirmed) {
            int reviewChoice = ui.reviewOrder(
                    orderId, customerName, restaurantName, orderTypeLabel,
                    selectedItem, quantity, totalAmount);

            if (reviewChoice == 1) {
                confirmed = true;
            } else if (reviewChoice == 2) {
                return startOrderFlow(orderId, customerName, factory, restaurantName);
            } else if (reviewChoice == 3) {
                ui.displayCancellation();
                return null;
            } else {
                ui.displayError("Invalid choice. Please try again.");
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

        int paymentChoice = ui.choosePaymentMethod(totalAmount);

        String paymentMethod;
        if (paymentChoice == 0) {
            System.out.println("\nOrder cancelled during payment.");
            return null;
        } else if (paymentChoice == 1) {
            String upiId = ui.readUpiId();
            System.out.println("[INFO] UPI payment method selected: " + upiId);
            paymentMethod = "UPI (" + upiId + ")";
        } else if (paymentChoice == 2) {
            String last4 = ui.readCardLast4();
            System.out.println("[INFO] Card payment method selected: ****" + last4);
            paymentMethod = "CARD (****" + last4 + ")";
        } else if (paymentChoice == 3) {
            paymentMethod = "CASH ON DELIVERY";
        } else {
            ui.displayError("Invalid choice. Please try again.");
            return null;
        }

        System.out.println("\n----------------------------------------");
        System.out.println("       PROXY DESIGN PATTERN");
        System.out.println("----------------------------------------");
        PaymentGateway gateway = new PaymentGatewayProxy();

        boolean paymentSuccess = handleProxyPayment(gateway, order, paymentMethod);

        ui.displayOrderConfirmed(
                order.getOrderId(), customerName, restaurantName,
                orderTypeLabel, order.getFoodItem(), order.getQuantity(),
                order.getFoodItem().getPrice(), order.getAmount(),
                paymentMethod, paymentSuccess);

        return order;
    }

    private static Order mainMenuLoop(int orderId, String customerName) {
        RestaurantFactory factory = null;
        String restaurantName = null;

        while (factory == null) {
            int choice = ui.chooseRestaurant();

            if (choice == 0) {
                ui.displayThankYou();
                ui.close();
                return null;
            } else if (choice == 1) {
                factory = new PizzaHubFactory();
                restaurantName = "Pizza Hub";
            } else if (choice == 2) {
                factory = new SpiceHubFactory();
                restaurantName = "Spice Hub";
            } else {
                ui.displayError("Invalid choice. Please try again.");
            }
        }

        return startOrderFlow(orderId, customerName, factory, restaurantName);
    }

    private static boolean handleProxyPayment(PaymentGateway gateway, Order order, String paymentMethod) {
        if (paymentMethod.startsWith("CASH ON DELIVERY")) {
            System.out.println("[OK] Cash on Delivery selected.");
            System.out.println("[OK] Order confirmed for payment on delivery.");
            return false;
        }

        gateway.processPayment(order);
        return true;
    }
}
