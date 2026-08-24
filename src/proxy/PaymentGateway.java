package proxy;

import models.Order;

public interface PaymentGateway {
    boolean processPayment(Order order);
}
