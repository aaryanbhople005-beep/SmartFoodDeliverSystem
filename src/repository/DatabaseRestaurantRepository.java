package repository;

import abstractfactory.PizzaHubFactory;
import abstractfactory.RestaurantFactory;
import abstractfactory.SpiceHubFactory;
import database.DBConnection;
import models.FoodItem;
import models.Restaurant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite-backed implementation of {@link RestaurantRepository}.
 * 
 * Loads restaurant and menu data from the database while preserving the
 * Abstract Factory pattern for menu creation. The factories are still used
 * for creating food/drink objects, but menu data comes from SQLite.
 */
public class DatabaseRestaurantRepository implements RestaurantRepository {

    private final Map<String, RestaurantFactory> factories = new HashMap<>();

    public DatabaseRestaurantRepository() {
        // Keep Abstract Factory for food/drink creation behavior
        factories.put("pizza", new PizzaHubFactory());
        factories.put("spice", new SpiceHubFactory());
        
        // Initialize database schema
        try {
            DBConnection.initialize();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    @Override
    public List<Restaurant> findAll() {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT id, name, description, category FROM restaurants";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                list.add(new Restaurant(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("category")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load restaurants", e);
        }
        
        // Fallback to in-memory if database is empty
        if (list.isEmpty()) {
            list.add(new Restaurant("pizza", "Pizza Hub",
                    "Italian & continental pizzas, burgers and combos",
                    "Italian / Pizza"));
            list.add(new Restaurant("spice", "Spice Hub",
                    "North Indian and Biryani delicacies",
                    "Indian / Biryani"));
        }
        return list;
    }

    @Override
    public Restaurant findById(String restaurantId) {
        if (restaurantId == null) return null;
        
        String sql = "SELECT id, name, description, category FROM restaurants WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Restaurant(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category")
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load restaurant: " + restaurantId, e);
        }
        
        // Fallback to in-memory
        return switch (restaurantId.toLowerCase()) {
            case "pizza" -> new Restaurant("pizza", "Pizza Hub",
                    "Italian & continental pizzas, burgers and combos",
                    "Italian / Pizza");
            case "spice" -> new Restaurant("spice", "Spice Hub",
                    "North Indian and Biryani delicacies",
                    "Indian / Biryani");
            default -> null;
        };
    }

    @Override
    public FoodItem[] getMenu(String restaurantId, String category) {
        if (restaurantId == null || category == null) {
            return new FoodItem[0];
        }
        
        String sql = """
            SELECT item_id, name, price, category, restaurant_id
            FROM menu_items
            WHERE restaurant_id = ? AND category = ?
            ORDER BY item_id
        """;
        
        List<FoodItem> items = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, restaurantId.toLowerCase());
            stmt.setString(2, category.toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new FoodItem(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("category"),
                        rs.getString("restaurant_id")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load menu for " + restaurantId + " / " + category, e);
        }
        
        // Fallback to Abstract Factory if database has no items
        if (items.isEmpty()) {
            RestaurantFactory factory = factories.get(restaurantId.toLowerCase());
            if (factory != null) {
                return factory.getMenu(category.toUpperCase());
            }
            return new FoodItem[0];
        }
        
        return items.toArray(new FoodItem[0]);
    }

    @Override
    public String getRestaurantName(String restaurantId) {
        Restaurant restaurant = findById(restaurantId);
        return restaurant == null ? null : restaurant.getName();
    }
}