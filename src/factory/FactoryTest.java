package factory;

import models.Order;

public class FactoryTest {

    public static void main(String[] args) {

        OrderCreator vegCreator = new VegOrderCreator();
        OrderCreator nonVegCreator = new NonVegOrderCreator();
        OrderCreator comboCreator = new ComboOrderCreator();

        Order vegOrder = vegCreator.createOrder(
                101,
                "Madhura",
                350
        );

        Order nonVegOrder = nonVegCreator.createOrder(
                102,
                "Madhura",
                500
        );

        Order comboOrder = comboCreator.createOrder(
                103,
                "Madhura",
                700
        );

        System.out.println(
                vegOrder.getOrderId()
                + " | "
                + vegOrder.getOrderType()
                + " | ₹"
                + vegOrder.getAmount()
        );

        System.out.println(
                nonVegOrder.getOrderId()
                + " | "
                + nonVegOrder.getOrderType()
                + " | ₹"
                + nonVegOrder.getAmount()
        );

        System.out.println(
                comboOrder.getOrderId()
                + " | "
                + comboOrder.getOrderType()
                + " | ₹"
                + comboOrder.getAmount()
        );
    }
}