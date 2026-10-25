package strategy;

import models.Order;

/**
 * Payment strategy. Returns true when the payment succeeded, false when it
 * failed. A false return must never be treated as success.
 */
public interface PaymentStrategy {
    boolean pay(Order order);
}