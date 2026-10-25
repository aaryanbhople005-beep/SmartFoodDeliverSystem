package repository;

import abstractfactory.PizzaHubFactory;
import abstractfactory.RestaurantFactory;
import abstractfactory.SpiceHubFactory;
import models.FoodItem;
import models.Restaurant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of RestaurantRepository.
 *
 * IMPORTANT: this is TEMPORARY data. It is NOT a database. It exists only so the
 * browser interface can demonstrate end-to-end functionality while JDBC/DAO
 * integration remains a future step. A future DatabaseRestaurantRepository
 * (JDBC/DAO) can replace this class because the service layer depends only on
 * the RestaurantRepository interface.
 *
 * Menu data is NOT duplicated: menu retrieval delegates to the existing
 * Abstract Factory implementations (PizzaHubFactory / SpiceHubFactory).
 */
public class InMemoryRestaurantRepository implements RestaurantRepository {

    private final Map<String, RestaurantFactory> factories = new HashMap<>();

    public InMemoryRestaurantRepository() {
        factories.put("pizza", new PizzaHubFactory());
        factories.put("spice", new SpiceHubFactory());
    }

    @Override
    public List<Restaurant> findAll() {
        List<Restaurant> list = new ArrayList<>();
        list.add(new Restaurant("pizza", "Pizza Hub",
                "Italian & continental pizzas, burgers and combos",
                "Italian / Pizza"));
        list.add(new Restaurant("spice", "Spice Hub",
                "North Indian and Biryani delicacies",
                "Indian / Biryani"));
        return list;
    }

    @Override
    public Restaurant findById(String restaurantId) {
        if (restaurantId == null) {
            return null;
        }
        switch (restaurantId.toLowerCase()) {
            case "pizza":
                return new Restaurant("pizza", "Pizza Hub",
                        "Italian & continental pizzas, burgers and combos",
                        "Italian / Pizza");
            case "spice":
                return new Restaurant("spice", "Spice Hub",
                        "North Indian and Biryani delicacies",
                        "Indian / Biryani");
            default:
                return null;
        }
    }

    @Override
    public FoodItem[] getMenu(String restaurantId, String category) {
        if (restaurantId == null || category == null) {
            return new FoodItem[0];
        }
        RestaurantFactory factory = factories.get(restaurantId.toLowerCase());
        if (factory == null) {
            return new FoodItem[0];
        }
        return factory.getMenu(category.toUpperCase());
    }

    @Override
    public String getRestaurantName(String restaurantId) {
        Restaurant restaurant = findById(restaurantId);
        return restaurant == null ? null : restaurant.getName();
    }
}