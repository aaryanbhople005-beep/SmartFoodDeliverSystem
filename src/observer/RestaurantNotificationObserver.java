package observer;

import java.util.Objects;

public class RestaurantNotificationObserver implements Observer {
    private final String restaurantName;

    public RestaurantNotificationObserver(String restaurantName) {
        this.restaurantName = Objects.requireNonNull(restaurantName, "restaurantName cannot be null");
    }

    @Override
    public void update(OrderStatusChange change) {
        System.out.println("Restaurant notification for " + restaurantName
                + ": Order #" + change.getOrderId()
                + " status changed from " + change.getPreviousStatus()
                + " to " + change.getNewStatus() + ".");
    }
}
