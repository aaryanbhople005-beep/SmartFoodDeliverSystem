package observer;

import java.util.Objects;

public class CustomerNotificationObserver implements Observer {
    private final String customerName;

    public CustomerNotificationObserver(String customerName) {
        this.customerName = Objects.requireNonNull(customerName, "customerName cannot be null");
    }

    @Override
    public void update(OrderStatusChange change) {
        System.out.println("Customer notification for " + customerName
                + ": Order #" + change.getOrderId()
                + " status changed from " + change.getPreviousStatus()
                + " to " + change.getNewStatus() + ".");
    }
}
