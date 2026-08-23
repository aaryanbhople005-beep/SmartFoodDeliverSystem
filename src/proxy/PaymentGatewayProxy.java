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
            System.out.println("✗ Payment rejected: Invalid amount.");
            return;
        }

        if (amount > 10000) {
            System.out.println("✗ Payment rejected: Amount exceeds ₹10,000 limit.");
            return;
        }

        System.out.println("✓ Payment authorized by proxy.");
        realGateway.processPayment(order);
    }
}
