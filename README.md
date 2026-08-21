# SmartFoodDeliverySystem

A Design Patterns project for a Smart Food Delivery System.

## Design Patterns Used

1. **Singleton** - Manages single instance of the OrderManager
2. **Factory Method** - Creates different types of orders (Veg, Non-Veg, Combo)
3. **Abstract Factory** - Creates restaurant-specific food and drink families (PizzaHub, SpiceHub)
4. **Chain of Responsibility** - Processes order through a chain (Order -> Payment -> Restaurant -> Delivery)
5. **Proxy** - Controls access to the PaymentGateway

## Project Structure

```
src/
├── models/           # Core domain models (Order, FoodItem, Restaurant)
├── singleton/        # Singleton pattern (OrderManager)
├── factory/          # Factory Method pattern (OrderCreator and subclasses)
├── abstractfactory/  # Abstract Factory pattern (RestaurantFactory and concrete factories)
├── chain/            # Chain of Responsibility (Order handlers)
├── proxy/            # Proxy pattern (PaymentGateway)
└── Main.java         # Application entry point
```

## Team Assignment

This project is structured for a 5-member team, with each member working on a separate design pattern:

- **Member 1**: Singleton (`OrderManager`)
- **Member 2**: Factory Method (`OrderCreator` family)
- **Member 3**: Abstract Factory (`RestaurantFactory` family)
- **Member 4**: Chain of Responsibility (`OrderHandler` chain)
- **Member 5**: Proxy (`PaymentGateway` family)

Members can also collaborate on the shared `models/` package as needed.
