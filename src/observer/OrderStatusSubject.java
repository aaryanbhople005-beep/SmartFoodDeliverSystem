package observer;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class OrderStatusSubject {
    private final int orderId;
    private final Set<Observer> observers = new LinkedHashSet<>();
    private OrderStatus status;

    public OrderStatusSubject(int orderId, OrderStatus initialStatus) {
        this.orderId = orderId;
        this.status = Objects.requireNonNull(initialStatus, "initialStatus cannot be null");
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void registerObserver(Observer observer) {
        observers.add(Objects.requireNonNull(observer, "observer cannot be null"));
    }

    public void removeObserver(Observer observer) {
        observers.remove(Objects.requireNonNull(observer, "observer cannot be null"));
    }

    public void setStatus(OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus cannot be null");
        if (status == newStatus) {
            return;
        }

        OrderStatusChange change = new OrderStatusChange(orderId, status, newStatus);
        status = newStatus;
        notifyObservers(change);
    }

    public void notifyObservers(OrderStatusChange change) {
        Objects.requireNonNull(change, "change cannot be null");
        for (Observer observer : observers) {
            observer.update(change);
        }
    }
}
