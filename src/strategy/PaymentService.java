package strategy;

import models.Order;

public class PaymentService {

    private PaymentStrategy paymentStrategy;

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        if (paymentStrategy == null) {
            throw new IllegalArgumentException("Payment strategy cannot be null.");
        }

        this.paymentStrategy = paymentStrategy;
    }

    public boolean processPayment(Order order) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("Please select a payment method first.");
        }

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null.");
        }

        return paymentStrategy.pay(order);
    }
}