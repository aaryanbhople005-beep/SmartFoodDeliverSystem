package abstractfactory;

import models.FoodItem;

public class SpiceHubFactory implements RestaurantFactory {
    @Override
    public Food createFood() {
        return () -> {
            System.out.println("Spice Hub Food Categories: VEG, NON-VEG, COMBO");
        };
    }

    @Override
    public Drink createDrink() {
        return () -> {
            System.out.println("Spice Hub Drink Categories: DRINKS");
        };
    }

    @Override
    public FoodItem[] getMenu(String category) {
        switch (category.toUpperCase()) {
            case "VEG":
                return new FoodItem[] {
                    new FoodItem("S101", "Paneer Tikka", 220.0, "VEG", "Spice Hub"),
                    new FoodItem("S102", "Veg Biryani", 200.0, "VEG", "Spice Hub"),
                    new FoodItem("S103", "Dal Tadka", 160.0, "VEG", "Spice Hub")
                };
            case "NON-VEG":
                return new FoodItem[] {
                    new FoodItem("S201", "Chicken Biryani", 280.0, "NON-VEG", "Spice Hub"),
                    new FoodItem("S202", "Butter Chicken", 300.0, "NON-VEG", "Spice Hub"),
                    new FoodItem("S203", "Chicken Tikka", 260.0, "NON-VEG", "Spice Hub")
                };
            case "COMBO":
                return new FoodItem[] {
                    new FoodItem("S301", "Veg Thali Combo", 299.0, "COMBO", "Spice Hub"),
                    new FoodItem("S302", "Chicken Meal Combo", 399.0, "COMBO", "Spice Hub"),
                    new FoodItem("S303", "Biryani Combo", 449.0, "COMBO", "Spice Hub")
                };
            case "DRINKS":
                return new FoodItem[] {
                    new FoodItem("S401", "Masala Chaas", 40.0, "DRINKS", "Spice Hub"),
                    new FoodItem("S402", "Lemon Soda", 60.0, "DRINKS", "Spice Hub"),
                    new FoodItem("S403", "Mango Lassi", 80.0, "DRINKS", "Spice Hub")
                };
            default:
                return new FoodItem[0];
        }
    }

    @Override
    public String getRestaurantName() {
        return "Spice Hub";
    }
}
