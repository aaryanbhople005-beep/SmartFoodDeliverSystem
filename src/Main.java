import controller.OrderController;
import view.FoodDeliveryView;

/**
 * Application entry point.
 * Initializes the View and Controller and starts the MVC order workflow.
 */
public class Main {

    public static void main(String[] args) {
        FoodDeliveryView view = new FoodDeliveryView();
        OrderController controller = new OrderController(view);
        controller.start();
    }
}
