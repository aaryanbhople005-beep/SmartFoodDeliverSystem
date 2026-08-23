package abstractfactory;

import models.FoodItem;

public interface RestaurantFactory {
    Food createFood();
    Drink createDrink();
    FoodItem[] getMenu(String category);
    String getRestaurantName();
}
