package singleton;

public class SingletonDemo {

    public static void main(String[] args) {

        // Get the first instance
        OrderManager orderManager1 = OrderManager.getInstance();

        // Get the second instance
        OrderManager orderManager2 = OrderManager.getInstance();

        // Check whether both references point to the same object
        if (orderManager1 == orderManager2) {
            System.out.println("Both objects are the same instance.");
        }

        orderManager1.manageOrder("Pizza");
        orderManager2.manageOrder("Burger");
    }
}