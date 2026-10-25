package proxy;

import models.Order;

/**
 * Real payment gateway integration point.
 *
 * IMPORTANT: this is a DEMO simulation, not a real payment provider. No money
 * is charged and no real card details are collected. Configure a real sandbox
 * gateway (see README) before processing live payments.
 */
public class RealPaymentGateway implements PaymentGateway {

    private static final double MAX_AMOUNT = 100000.0;

    @Override
    public boolean processPayment(Order order) {
        double amount = order.getAmount();

        System.out.println("[DEMO MODE] RealPaymentGateway: no money is charged.");

        if (order == null) {
            System.out.println("[DEMO] Payment rejected: order is null.");
            return false;
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            System.out.println("[DEMO] Payment rejected: invalid amount Rs." + amount);
            return false;
        }
        if (amount > MAX_AMOUNT) {
            System.out.println("[DEMO] Payment rejected: amount exceeds limit.");
            return false;
        }

        System.out.println("[DEMO] Payment of Rs." + amount + " simulated successfully (no funds transferred).");
        return true;
    }
}
