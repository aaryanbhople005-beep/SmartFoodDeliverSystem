package controller;

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
import view.FoodDeliveryView;

/**
 * Controller layer of the MVC architecture.
 * Coordinates the order workflow between the View, the Model, and the
 * existing design patterns (Factory Method, Abstract Factory, Singleton,
 * Chain of Responsibility, Proxy).
 */
public class OrderController {

    private final FoodDeliveryView view;

    public OrderController(FoodDeliveryView view) {
        this.view = view;
    }

    public void start() {
        view.displayWelcome();
        int orderId = view.readInt("Enter Order ID: ");
        String customerName = view.readString("Enter Customer Name: ");

        while (true) {
            RestaurantFactory factory = null;
            String restaurantName = null;

            while (factory == null) {
                view.displayRestaurantMenu();
                int choice = view.readInt("Choice: ");

                if (choice == 0) {
                    view.displayExitMessage();
                    view.close();
                    return;
                } else if (choice == 1) {
                    factory = new PizzaHubFactory();
                    restaurantName = "Pizza Hub";
                } else if (choice == 2) {
                    factory = new SpiceHubFactory();
                    restaurantName = "Spice Hub";
                } else {
                    view.displayError("Invalid choice. Please try again.");
                }
            }

            ORDER_TYPE: while (true) {
                OrderCreator creator = null;
                String category = null;
                String orderTypeLabel = null;

                while (creator == null) {
                    view.displayOrderTypeMenu();
                    int choice = view.readInt("Choice: ");

                    if (choice == 0) {
                        break ORDER_TYPE;
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
                        view.displayError("Invalid choice. Please try again.");
                    }
                }

                ITEM: while (true) {
                    FoodItem[] menuItems = factory.getMenu(category);
                    view.displayMenu(restaurantName, category, menuItems);

                    int itemChoice = view.readInt("Select Item: ");

                    if (itemChoice == 0) {
                        break ITEM;
                    }

                    if (itemChoice < 1 || itemChoice > menuItems.length) {
                        view.displayError("Invalid item choice. Please try again.");
                        continue;
                    }

                    FoodItem selectedItem = menuItems[itemChoice - 1];
                    int quantity = view.readQuantity();
                    double totalAmount = selectedItem.getPrice() * quantity;

                    view.displayItemSelected(selectedItem, quantity, totalAmount);

                    REVIEW: while (true) {
                        int reviewChoice = view.displayOrderReview(
                                orderId, customerName, restaurantName,
                                orderTypeLabel, selectedItem, quantity, totalAmount);

                        if (reviewChoice == 1) {
                            Order order = creator.createOrder(orderId, customerName, selectedItem, quantity);
                            view.displayOrderCreated(order);

                            OrderManager manager = OrderManager.getInstance();
                            manager.manageOrder(order.getOrderType() + " Order #" + order.getOrderId());
                            view.displayOrderRegistered();

                            view.displayChainValidationHeader();
                            boolean valid = validateOrder(order);
                            if (!valid) {
                                view.displayValidationFailed();
                                view.close();
                                return;
                            }
                            view.displayValidationPassed();

                            String paymentMethod = selectPaymentMethod(totalAmount);
                            if (paymentMethod == null) {
                                view.displayPaymentCancelled();
                                view.close();
                                return;
                            }

                            view.displayProxySection();
                            boolean paymentSuccess = processPayment(order, paymentMethod);

                            view.displayOrderConfirmation(
                                    order, customerName, restaurantName,
                                    orderTypeLabel, paymentMethod, paymentSuccess);
                            view.close();
                            return;
                        } else if (reviewChoice == 2) {
                            break ITEM;
                        } else if (reviewChoice == 3) {
                            view.displayOrderCancelled();
                            view.close();
                            return;
                        } else {
                            view.displayError("Invalid choice. Please try again.");
                        }
                    }
                }
                continue ORDER_TYPE;
            }
        }
    }

    private String selectPaymentMethod(double totalAmount) {
        while (true) {
            view.displayPaymentMenu(totalAmount);
            int choice = view.readInt("Choice: ");

            if (choice == 0) {
                return null;
            } else if (choice == 1) {
                String upiId = view.readString("Enter UPI ID: ");
                view.displayUpiSelected(upiId);
                return "UPI (" + upiId + ")";
            } else if (choice == 2) {
                String last4 = view.readString("Enter last 4 digits of card: ");
                view.displayCardSelected(last4);
                return "CARD (****" + last4 + ")";
            } else if (choice == 3) {
                return "CASH ON DELIVERY";
            } else {
                view.displayError("Invalid choice. Please try again.");
            }
        }
    }

    private boolean processPayment(Order order, String paymentMethod) {
        if (paymentMethod.startsWith("CASH ON DELIVERY")) {
            view.displayCashOnDelivery();
            return false;
        }

        PaymentGateway gateway = new PaymentGatewayProxy();
        gateway.processPayment(order);
        return true;
    }

    private boolean validateOrder(Order order) {
        OrderHandler paymentHandler = new PaymentHandler();
        OrderHandler restaurantHandler = new RestaurantHandler();
        OrderHandler deliveryHandler = new DeliveryHandler();

        paymentHandler
                .setNextHandler(restaurantHandler)
                .setNextHandler(deliveryHandler);

        return paymentHandler.validate(order);
    }
}
