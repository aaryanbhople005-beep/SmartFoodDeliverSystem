# SmartFoodDeliverySystem

A browser-based Smart Food Delivery Application built as a college Design Pattern Lab project.

## Overview

SmartFoodDeliverySystem is a complete food delivery application with a **browser-based frontend** (HTML/CSS/JavaScript) and a **Java backend**. The frontend communicates with the backend through a **REST API** (JSON) served by a lightweight JDK HTTP server. The existing Java design patterns are preserved and integrated into the real order workflow.

## Features

- Home page with restaurant discovery and featured items
- Restaurant listing and menu browsing (Veg / Non-Veg / Combo / Drinks)
- Functional shopping cart (add, update quantity, remove, save/restore snapshots)
- Checkout with customer name, order type, and payment method (UPI / Card / Cash on Delivery)
- Order creation validated through the existing Chain of Responsibility
- Order confirmation with details and Prototype-based reorder
- Order status display (Observer pattern, polled every 5 seconds)
- Responsive, food-delivery themed UI

## Technology Stack

- **Frontend**: HTML5, CSS3, vanilla JavaScript (ES modules), no frameworks
- **Backend**: Plain Java (JDK 25), built-in `com.sun.net.httpserver.HttpServer` — no external dependencies
- **Communication**: REST API over HTTP, JSON request/response
- **Build**: None (compiled with `javac`)

## Design Patterns Preserved & Integrated

| Pattern | Role in the application |
|---|---|
| **Singleton** | `OrderManager` registers every placed order |
| **Factory Method** | `OrderCreator` family creates Veg / Non-Veg / Combo orders |
| **Abstract Factory** | `RestaurantFactory` family supplies restaurant menus (no menu duplication) |
| **Chain of Responsibility** | `PaymentHandler` → `RestaurantHandler` → `DeliveryHandler` validate orders |
| **Proxy** | `PaymentGatewayProxy` guards `RealPaymentGateway` for UPI/Card payments |
| **MVC** | `controller.OrderController` + `view.FoodDeliveryView` (console demo); REST controllers + frontend (browser) |
| **Observer** | `OrderStatusSubject` notifies customer/restaurant observers on status change |
| **Strategy** | `PaymentService` selects UPI / Card / CashOnDelivery strategy |
| **Memento** | `ShoppingCart.save()` / `restore()` cart snapshots |
| **Builder** | `OrderBuilder` constructs validated orders |
| **Prototype** | `Order.copy()` duplicates an order for reorder |

## Project Structure

```
SmartFoodDeliverySystem/
├── src/
│   ├── Main.java                      # Original console entry point (unchanged)
│   ├── web/HttpServerApp.java         # Browser entry point: starts HTTP server + REST API
│   ├── web/ApiHandler.java            # Base REST handler (JSON helpers)
│   ├── web/JsonUtil.java              # Dependency-free JSON serializer
│   ├── web/RestaurantHandler.java     # GET /api/restaurants, menu
│   ├── web/CartHandler.java           # Cart CRUD + Memento save/restore
│   ├── web/OrderHandler.java          # Orders, payment, status, reorder (Prototype)
│   ├── service/FoodDeliveryService.java  # Application service orchestrating patterns
│   ├── controller/OrderController.java   # Existing MVC controller (console demo)
│   ├── view/FoodDeliveryView.java        # Existing console view (console demo)
│   ├── models/                        # Order, FoodItem, Restaurant, VegOrder, NonVegOrder, ComboOrder
│   ├── repository/                    # RestaurantRepository, OrderRepository (JDBC/DAO boundary)
│   │   ├── InMemoryRestaurantRepository.java
│   │   └── InMemoryOrderRepository.java
│   ├── singleton/                     # OrderManager
│   ├── factory/                       # OrderCreator family (Factory Method)
│   ├── abstractfactory/               # RestaurantFactory family (Abstract Factory)
│   ├── chain/                         # OrderHandler chain (Chain of Responsibility)
│   ├── proxy/                         # PaymentGateway + Proxy + Real (Proxy)
│   ├── observer/                      # Observer, OrderStatusSubject, notifications (Observer)
│   ├── strategy/                      # PaymentService + UPI/Card/COD strategies (Strategy)
│   ├── memento/                       # ShoppingCart + CartMemento + CartItem (Memento)
│   ├── builder/                       # OrderBuilder (Builder)
│   ├── prototype/                     # Prototype<T> (Prototype)
│   └── ui/                            # OrderInterface + ConsoleInterface (Interface assignment artifacts)
├── frontend/
│   ├── index.html                     # Home page
│   ├── restaurants.html               # Restaurant listing
│   ├── menu.html                      # Restaurant menu
│   ├── cart.html                      # Shopping cart + Memento save/restore
│   ├── checkout.html                  # Checkout + order placement
│   ├── order-confirmation.html        # Order confirmation
│   ├── order-status.html              # Order status
│   ├── css/style.css
│   └── js/*.js                        # API client, shared app logic, page scripts
├── documentation/
│   └── Interface_Implementation_Report.docx
├── out/                               # Compiled class output
├── .gitignore
└── README.md
```

## Prerequisites

- JDK 25 (or later) — `javac` and `java` on PATH
- No Maven/Gradle and no external libraries are required

## How to Run

### 1. Compile the backend

From the project root:

```powershell
javac -d out -sourcepath src $(Get-ChildItem -Recurse src -Filter *.java | Select-Object -ExpandProperty FullName)
```

### 2. Start the backend (browser entry point)

```powershell
java -cp out web.HttpServerApp
```

You should see:

```
SmartFoodDeliverySystem server started on http://localhost:8080
Frontend: http://localhost:8080/index.html
API base: http://localhost:8080/api
```

### 3. Open the frontend

Open `http://localhost:8080/index.html` in your browser.

### 4. (Optional) Run the original console demo

```powershell
java -cp out Main
```

## REST API Overview

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/restaurants` | List restaurants (Abstract Factory) |
| GET | `/api/restaurants/{id}` | Restaurant details |
| GET | `/api/restaurants/{id}/menu?category=VEG` | Menu for a category |
| GET | `/api/cart?session=xxx` | Cart items + total |
| POST | `/api/cart/add` | Add item to cart |
| POST | `/api/cart/quantity` | Update item quantity |
| POST | `/api/cart/remove` | Remove item |
| POST | `/api/cart/save` | Save cart snapshot (Memento token) |
| POST | `/api/cart/restore` | Restore cart from token |
| POST | `/api/orders` | Create order (Factory Method, Chain, Singleton, Observer) |
| GET | `/api/orders/{id}` | Order details |
| GET | `/api/orders/{id}/status` | Order status |
| POST | `/api/orders/{id}/pay?method=UPI\|CARD\|COD` | Process payment (Strategy + Proxy) |
| POST | `/api/orders/{id}/advance-status` | Advance status (demo) |
| POST | `/api/orders/{id}/reorder` | Duplicate order (Prototype) |
| GET | `/api/payment/methods` | Supported payment methods |

## Database / JDBC / DAO Status

**JDBC and DAO are NOT implemented in this task.** The repository layer (`repository.RestaurantRepository`, `repository.OrderRepository`) is defined as an interface so a future `DatabaseRestaurantRepository` / `DatabaseOrderRepository` (JDBC/DAO) can replace the current in-memory implementations without touching the service or frontend.

- Current persistence: in-memory (`InMemoryRestaurantRepository`, `InMemoryOrderRepository`) — temporary, non-persistent.
- Data is lost when the server stops.
- No database credentials, SQL, or JDBC code exist in the project.
- Future JDBC/DAO work should implement the repository interfaces and wire them into `FoodDeliveryService`.

## Testing Performed

- Backend compiles cleanly with `javac` (no errors).
- Server starts and serves frontend + API.
- End-to-end flows verified via REST: restaurants, menus, cart add/update/remove, Memento save/restore, order creation (Veg/Non-Veg), Chain of Responsibility validation, Singleton registration, UPI/Card/COD payment (Strategy + Proxy), order status + Observer advancement, Prototype reorder.

## Known Limitations

- No real database: carts and orders are in-memory and reset when the server restarts.
- Order status advancement is a manual demo endpoint, not real-time driver tracking.
- No authentication or user accounts (out of scope).
- Cart state is tied to a session id generated in the browser; it does not survive across different browsers/devices.

## Future Improvements

- JDBC/DAO implementation behind the repository interfaces.
- Real-time status push (WebSocket / Server-Sent Events) instead of polling.
- Persistent cart and order storage.
- User authentication and order history.
- Payment integration with a real gateway (sandbox).