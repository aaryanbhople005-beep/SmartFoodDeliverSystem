package strategy;

import models.Order;

public class CashOnDeliveryStrategy implements PaymentStrategy {

    @Override
    public boolean pay(Order order) {
        if (order.getAmount() <= 0) {
            System.out.println("Cash on Delivery rejected: Invalid order amount.");
            return false;
        }
        System.out.println("Cash on Delivery selected (demo).");
        System.out.println("Amount due on delivery: Rs." + order.getAmount());
        return true;
    }
}