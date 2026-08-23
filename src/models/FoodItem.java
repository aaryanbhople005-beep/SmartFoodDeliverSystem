package models;

public class FoodItem {
    private String itemId;
    private String name;
    private double price;
    private String category;
    private String restaurant;

    public FoodItem(String itemId, String name, double price, String category, String restaurant) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.category = category;
        this.restaurant = restaurant;
    }

    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getRestaurant() {
        return restaurant;
    }

    @Override
    public String toString() {
        return itemId + ": " + name + " - Rs." + price;
    }
}
