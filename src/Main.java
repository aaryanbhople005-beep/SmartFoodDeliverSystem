import models.Order;
import singleton.OrderManager;
import factory.OrderCreator;
import chain.OrderHandler;
import proxy.PaymentGateway;
import abstractfactory.RestaurantFactory;
import abstractfactory.PizzaHubFactory;
import abstractfactory.SpiceHubFactory;

public class Main {

    public static void main(String[] args) {

        RestaurantFactory pizzaHubFactory = new PizzaHubFactory();

        RestaurantFactory spiceHubFactory = new SpiceHubFactory();
        System.out.println("===== Abstract Factory Demo =====");

        System.out.println(
                "Restaurant: " +
                restaurantFactory.createRestaurant()
        );

        System.out.println(
                "Food: " +
                restaurantFactory.createFood()
        );

        System.out.println(
                "Drink: " +
                restaurantFactory.createDrink()
        );
    }
}