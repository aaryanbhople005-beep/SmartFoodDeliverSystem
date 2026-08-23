package abstractfactory;

public class PizzaHubFactory implements RestaurantFactory {
    @Override
    public Food createFood() {
        return () -> {
            System.out.println("Pizza Hub Food Menu");
            System.out.println("Margherita Pizza - Rs. 200");
            System.out.println("Farmhouse Pizza - Rs. 300");
        };
    }

    @Override
    public Drink createDrink() {
        return () -> {
            System.out.println("Pizza Hub Drink Menu");
            System.out.println("Coke - Rs. 50");
            System.out.println("Sprite - Rs. 50");
        };
    }
}
