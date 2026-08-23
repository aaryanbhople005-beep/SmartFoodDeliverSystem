package proxy;

import models.Order;

public class PaymentGatewayProxy implements PaymentGateway {
    private RealPaymentGateway realGateway;

    public PaymentGatewayProxy() {
        this.realGateway = new RealPaymentGateway();
    }

    @Override
    public void processPayment(Order order) {
        double amount = order.getAmount();

        if (amount <= 0) {
            System.out.println("Payment failed: invalid amount.");
            return;
        }

        if (amount > 10000) {
            System.out.println("Payment failed: amount exceeds maximum limit.");
            return;
        }

        System.out.println("Payment validation passed for Rs." + amount);
        realGateway.processPayment(order);
    }
}
