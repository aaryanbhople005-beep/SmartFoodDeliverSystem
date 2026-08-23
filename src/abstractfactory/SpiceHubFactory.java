package abstractfactory;

public class SpiceHubFactory implements RestaurantFactory {
    @Override
    public Food createFood() {
        return () -> {
            System.out.println("Spice Hub Food Menu");
            System.out.println("Paneer Tikka - Rs. 220");
            System.out.println("Chicken Biryani - Rs. 280");
        };
    }

    @Override
    public Drink createDrink() {
        return () -> {
            System.out.println("Spice Hub Drink Menu");
            System.out.println("Masala Chaas - Rs. 40");
            System.out.println("Lemon Soda - Rs. 60");
        };
    }
}