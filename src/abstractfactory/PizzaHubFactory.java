package abstractfactory;

import models.FoodItem;

public class PizzaHubFactory implements RestaurantFactory {
    @Override
    public Food createFood() {
        return () -> {
            System.out.println("Pizza Hub Food Categories: VEG, NON-VEG, COMBO");
        };
    }

    @Override
    public Drink createDrink() {
        return () -> {
            System.out.println("Pizza Hub Drink Categories: DRINKS");
        };
    }

    @Override
    public FoodItem[] getMenu(String category) {
        switch (category.toUpperCase()) {
            case "VEG":
                return new FoodItem[] {
                    new FoodItem("P101", "Margherita Pizza", 200.0, "VEG", "Pizza Hub"),
                    new FoodItem("P102", "Farmhouse Pizza", 300.0, "VEG", "Pizza Hub"),
                    new FoodItem("P103", "Paneer Tikka Pizza", 320.0, "VEG", "Pizza Hub")
                };
            case "NON-VEG":
                return new FoodItem[] {
                    new FoodItem("P201", "Chicken Pizza", 350.0, "NON-VEG", "Pizza Hub"),
                    new FoodItem("P202", "BBQ Chicken Pizza", 400.0, "NON-VEG", "Pizza Hub"),
                    new FoodItem("P203", "Chicken Tikka Pizza", 380.0, "NON-VEG", "Pizza Hub")
                };
            case "COMBO":
                return new FoodItem[] {
                    new FoodItem("P301", "Veg Pizza Combo", 399.0, "COMBO", "Pizza Hub"),
                    new FoodItem("P302", "Chicken Pizza Combo", 499.0, "COMBO", "Pizza Hub"),
                    new FoodItem("P303", "Family Pizza Combo", 799.0, "COMBO", "Pizza Hub")
                };
            case "DRINKS":
                return new FoodItem[] {
                    new FoodItem("P401", "Coke", 50.0, "DRINKS", "Pizza Hub"),
                    new FoodItem("P402", "Sprite", 50.0, "DRINKS", "Pizza Hub"),
                    new FoodItem("P403", "Cold Coffee", 80.0, "DRINKS", "Pizza Hub")
                };
            default:
                return new FoodItem[0];
        }
    }

    @Override
    public String getRestaurantName() {
        return "Pizza Hub";
    }
}
