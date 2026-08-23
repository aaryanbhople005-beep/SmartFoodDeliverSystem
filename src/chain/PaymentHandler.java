package chain;

import models.Order;

public class PaymentHandler extends OrderHandler {


@Override
public boolean validate(Order order) {

    if (order.getAmount() <= 0) {
        System.out.println("✗ Payment validation failed: Invalid amount.");
        return false;
    }

    System.out.println("✓ Payment details validated.");
    return checkNext(order);
}


}
