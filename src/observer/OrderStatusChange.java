package observer;

public final class OrderStatusChange {
    private final int orderId;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;

    public OrderStatusChange(int orderId, OrderStatus previousStatus, OrderStatus newStatus) {
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public int getOrderId() {
        return orderId;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }
}
