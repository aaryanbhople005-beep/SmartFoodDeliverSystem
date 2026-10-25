package memento;

public class CartItem {
    private final String itemId;
    private final String name;
    private final double price;
    private final String category;
    private final String restaurant;
    private int quantity;

    public CartItem(String itemId, String name, double price, int quantity) {
        this(itemId, name, price, "VEG", "", quantity);
    }

    public CartItem(String itemId, String name, double price, String category, String restaurant, int quantity) {
        if (itemId == null || name == null) {
            throw new IllegalArgumentException("Item ID and name cannot be null.");
        }
        if (price < 0 || quantity <= 0) {
            throw new IllegalArgumentException("Price must be non-negative and quantity must be positive.");
        }

        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.category = category;
        this.restaurant = restaurant;
        this.quantity = quantity;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return price * quantity;
    }

    public CartItem copy() {
        return new CartItem(itemId, name, price, category, restaurant, quantity);
    }

    @Override
    public String toString() {
        return name + " x " + quantity + " = Rs." + getSubtotal();
    }
}