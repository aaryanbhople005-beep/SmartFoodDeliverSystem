package chain;

import models.Order;

public abstract class OrderHandler {
    protected OrderHandler nextHandler;

    public OrderHandler setNextHandler(OrderHandler nextHandler) {
        this.nextHandler = nextHandler;
        return nextHandler; // Returns nextHandler for convenient chaining
    }

    public abstract boolean validate(Order order);
    
    protected boolean checkNext(Order order) {
        if (nextHandler == null) {
            return true;
        }
        return nextHandler.validate(order);
    }
}
