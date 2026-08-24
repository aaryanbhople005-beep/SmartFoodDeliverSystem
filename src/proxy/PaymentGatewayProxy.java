package proxy;

import models.Order;

public class PaymentGatewayProxy implements PaymentGateway {
    private RealPaymentGateway realGateway;

    public PaymentGatewayProxy() {
        this.realGateway = new RealPaymentGateway();
    }

    @Override
    public boolean processPayment(Order order) {
        double amount = order.getAmount();

        if (amount <= 0) {
            System.out.println("✗ Payment rejected: Invalid amount.");
            return false;
        }

        if (amount > 10000) {
            System.out.println("✗ Payment rejected: Amount exceeds ₹10,000 limit.");
            return false;
        }

        System.out.println("✓ Payment authorized by proxy.");
        return realGateway.processPayment(order);
    }
}
