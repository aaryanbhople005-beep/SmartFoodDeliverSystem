package strategy;

import models.Order;
import proxy.PaymentGatewayProxy;

public class UPIPaymentStrategy implements PaymentStrategy {

    private final PaymentGatewayProxy paymentGateway;

    public UPIPaymentStrategy() {
        this.paymentGateway = new PaymentGatewayProxy();
    }

    @Override
    public void pay(Order order) {
        System.out.println("Processing UPI payment...");

        paymentGateway.processPayment(order);
    }
}