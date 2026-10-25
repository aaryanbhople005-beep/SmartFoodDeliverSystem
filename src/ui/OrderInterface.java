package ui;

import models.FoodItem;

/**
 * OrderInterface defines the contract for the console-based user interface
 * of the SmartFoodDeliverySystem.
 *
 * It abstracts all user-facing console interaction (menus, prompts, input
 * reading and result display) so that the application entry point (Main)
 * can delegate presentation concerns to a dedicated implementation class.
 *
 * This interface is implemented by {@link ConsoleInterface} and is used
 * directly by {@link Main}.
 */
public interface OrderInterface {

    /* ---------- Welcome and input ---------- */

    /** Display the application welcome message. */
    void displayWelcome();

    /** Read and return the order ID from the user. */
    int readOrderId();

    /** Read and return the customer name from the user. */
    String readCustomerName();

    /* ---------- Restaurant selection ---------- */

    /**
     * Display restaurant options and return the user's choice.
     * @return 1 for Pizza Hub, 2 for Spice Hub, 0 to exit
     */
    int chooseRestaurant();

    /* ---------- Order type selection ---------- */

    /**
     * Display order type options and return the user's choice.
     * @return 1 for Veg, 2 for Non-Veg, 3 for Combo, 0 to go back
     */
    int chooseOrderType();

    /* ---------- Menu display ---------- */

    /**
     * Display the food menu for a restaurant and category.
     * @param restaurantName the name of the selected restaurant
     * @param category the selected order category (VEG / NON-VEG / COMBO)
     * @param menuItems the array of food items to display
     */
    void displayMenu(String restaurantName, String category, FoodItem[] menuItems);

    /* ---------- Item and quantity selection ---------- */

    /**
     * Prompt the user to select a menu item.
     * @param menuSize the number of items in the menu
     * @return the user's item choice (1-based), or 0 to go back
     */
    int chooseItem(int menuSize);

    /** Prompt the user to enter a quantity and return it. */
    int readQuantity();

    /* ---------- Item selected display ---------- */

    /**
     * Display the details of the selected item.
     * @param item the selected food item
     * @param quantity the selected quantity
     * @param total the computed total amount
     */
    void displayItemSelected(FoodItem item, int quantity, double total);

    /* ---------- Order review ---------- */

    /**
     * Display the order review and return the user's choice.
     * @return 1 to confirm, 2 to change item, 3 to cancel
     */
    int reviewOrder(int orderId, String customerName, String restaurantName,
                    String orderType, FoodItem item, int quantity, double total);

    /* ---------- Payment ---------- */

    /**
     * Display payment options and return the user's choice.
     * @param totalAmount the amount payable
     * @return 1 for UPI, 2 for Card, 3 for Cash on Delivery, 0 to cancel
     */
    int choosePaymentMethod(double totalAmount);

    /** Read and return a UPI ID from the user. */
    String readUpiId();

    /** Read and return the last 4 digits of a card from the user. */
    String readCardLast4();

    /* ---------- Results and summary ---------- */

    /**
     * Display the payment result.
     * @param success whether the online payment succeeded
     * @param paymentMethod the selected payment method
     */
    void displayPaymentResult(boolean success, String paymentMethod);

    /**
     * Display the final order confirmation summary.
     * @param orderId the order ID
     * @param customerName the customer name
     * @param restaurantName the restaurant name
     * @param orderType the order type
     * @param item the ordered food item
     * @param quantity the ordered quantity
     * @param unitPrice the unit price of the item
     * @param amount the total amount
     * @param paymentMethod the selected payment method
     * @param paymentSuccess whether the online payment succeeded
     */
    void displayOrderConfirmed(int orderId, String customerName, String restaurantName,
                               String orderType, FoodItem item, int quantity, double unitPrice,
                               double amount, String paymentMethod, boolean paymentSuccess);

    /** Display an order cancellation message. */
    void displayCancellation();

    /** Display the thank-you / process-completed message. */
    void displayThankYou();

    /** Display an error or validation message. */
    void displayError(String message);

    /** Close any resources held by the implementation. */
    void close();
}
