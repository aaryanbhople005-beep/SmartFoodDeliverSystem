package singleton;

public class OrderManager {

    private static OrderManager instance;

    private OrderManager() {
        System.out.println("Order Manager created.");
    }

    public static OrderManager getInstance() {
        if (instance == null) {
            instance = new OrderManager();
        }
        return instance;
    }

    public void manageOrder(String orderName) {
        System.out.println("Managing order: " + orderName);
    }
}