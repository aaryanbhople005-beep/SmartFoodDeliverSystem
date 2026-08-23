package chain;

import models.FoodItem;
import models.Order;

public abstract class OrderHandler {
    protected OrderHandler nextHandler;

    public OrderHandler setNextHandler(OrderHandler nextHandler) {
        this.nextHandler = nextHandler;
        return nextHandler;
    }

    public abstract boolean validate(Order order);

    protected boolean checkNext(Order order) {
        if (nextHandler == null) {
            return true;
        }
        return nextHandler.validate(order);
    }
}
