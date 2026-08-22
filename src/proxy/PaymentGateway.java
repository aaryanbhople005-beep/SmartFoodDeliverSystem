package proxy;

import models.Order;

public interface PaymentGateway {
    void processPayment(Order order);
}
