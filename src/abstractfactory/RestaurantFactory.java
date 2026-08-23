package abstractfactory;

import models.Restaurant;

public interface RestaurantFactory {

    Restaurant createRestaurant();

    Food createFood();

    Drink createDrink();
}