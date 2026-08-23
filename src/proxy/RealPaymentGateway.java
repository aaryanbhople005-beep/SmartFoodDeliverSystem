package proxy;

import models.Order;

public class RealPaymentGateway implements PaymentGateway {
    @Override
    public void processPayment(Order order) {
        System.out.println("✓ Payment processed successfully.");
    }
}
