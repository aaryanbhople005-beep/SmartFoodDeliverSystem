package strategy;

import models.Order;

public interface PaymentStrategy {
    void pay(Order order);
}