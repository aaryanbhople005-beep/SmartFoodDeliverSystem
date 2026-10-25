package repository;

import models.FoodItem;
import models.Restaurant;

import java.util.List;

/**
 * RestaurantRepository is the persistence abstraction for restaurant data.
 *
 * This interface is the JDBC/DAO integration point: a future DatabaseRestaurantRepository
 * (JDBC/DAO) can replace the current InMemoryRestaurantRepository without any change
 * to the service or frontend layers, because both depend on this interface.
 *
 * The current in-memory implementation delegates menu retrieval to the existing
 * Abstract Factory (RestaurantFactory), so no menu data is duplicated.
 */
public interface RestaurantRepository {

    List<Restaurant> findAll();

    Restaurant findById(String restaurantId);

    /**
     * @return the array of FoodItem returned by the Abstract Factory's getMenu(category)
     *         for the given restaurant and category, or an empty array if unsupported.
     */
    FoodItem[] getMenu(String restaurantId, String category);

    /**
     * @return the restaurant name as provided by the Abstract Factory, or null if unknown.
     */
    String getRestaurantName(String restaurantId);
}