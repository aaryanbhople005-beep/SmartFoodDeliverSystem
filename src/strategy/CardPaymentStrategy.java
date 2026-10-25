package strategy;

import models.Order;
import proxy.PaymentGatewayProxy;

public class CardPaymentStrategy implements PaymentStrategy {

    private final PaymentGatewayProxy paymentGateway;

    public CardPaymentStrategy() {
        this.paymentGateway = new PaymentGatewayProxy();
    }

    @Override
    public void pay(Order order) {
        System.out.println("Processing card payment...");

        paymentGateway.processPayment(order);
    }
}