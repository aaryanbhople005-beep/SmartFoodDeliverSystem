# SmartFoodDeliverySystem

A browser-based Smart Food Delivery Application built as a college Design Pattern Lab project. It has a **browser-based frontend** (HTML/CSS/vanilla JavaScript) and a **Java backend** (JDK `com.sun.net.httpserver.HttpServer`, no frameworks) that communicates through a **REST API** (JSON). The Java design patterns are integrated into the real order workflow (not left as demos).

> **Entry point:** open `http://localhost:8080/index.html` in a browser **while the Java server is running**. Do not open the HTML files directly via `file://` — the Java HTTP server is the intended entry point and serves both the frontend and the API.

## Current implemented features

- Home page with restaurant discovery and featured items loaded from the backend API (no duplicated hardcoded menu).
- Restaurant listing and menu browsing for Pizza Hub and Spice Hub (Abstract Factory-backed menus), categories VEG / NON-VEG / COMBO / DRINKS.
- Functional shopping cart backed by the Java backend: add item (backend-verified product + price), increase/decrease quantity, remove, correct subtotal/total, clear/empty state, Memento save/restore snapshots.
- Checkout: customer name + payment method (COD / UPI / Card). The backend derives each item's order type from its actual product category (a checkout radio button can never misrepresent a Non-Veg item as Veg). Payment is processed for **every** created order.
- Order creation through Factory Method + Builder + Chain of Responsibility + Singleton + Observer.
- Order confirmation showing **all** items in the checkout (item, quantity, price, subtotal, order type, total, payment method, backend status).
- Order status page polled from the backend (Observer pattern); demo "Advance Status" button.
- Prototype-based reorder (`Order.copy()`) — creates a new draft order id, never reuses a persisted id.
- Real backend authentication: register, login, logout, current-user. Passwords hashed with PBKDF2 (never stored plain text). Server-side sessions. Duplicate email rejected. Invalid credentials rejected.
- Order ownership: a customer can only read/pay/reorder their own orders; guessing another customer's order id returns 403.
- JDBC/DAO persistent storage (SQLite) for orders and users: survives a server restart. Prepared statements, transactions where multiple writes must succeed together, safe schema migration (no dropped tables).
- Secure static file serving: path traversal blocked, only safe frontend extensions served, 404 for missing resources.

## Current limitations

- **Payment is in demo mode.** `RealPaymentGateway` is explicitly labelled as a simulation — no money is charged, no real card details are collected. To use a real sandbox gateway, wire a real provider into `RealPaymentGateway` / `PaymentGatewayProxy` (see "Payment mode and limitations").
- Persistence defaults to **JDBC (SQLite)**; if the SQLite JDBC driver is unavailable at runtime the service falls back to in-memory repositories with a console warning (`smartfood.persistence=memory` forces in-memory dev/test mode).
- Order status advancement (`advance-status`) is a manual demo endpoint, not real-time driver tracking. No real driver location tracking is claimed.
- Cart and session are tied to browser `localStorage`; they do not sync across browsers/devices.
- No Maven installation was detected in this environment; the project compiles/runs with `javac` directly (commands below). A `pom.xml` is provided for Maven-based builds.
- The SQLite JDBC driver uses native code; on JDK 25 you may see a `System::load` warning unless you add `--enable-native-access=ALL-UNNAMED` (see run command). Functionality is unaffected.

## Technology stack

- **Frontend**: HTML5, CSS3, vanilla JavaScript (ES modules), no frameworks.
- **Backend**: Plain Java (JDK 25), built-in `com.sun.net.httpserver.HttpServer` — no web framework.
- **Communication**: REST API over HTTP, JSON (dependency-free serializer in `web.JsonUtil`).
- **Database/driver**: SQLite via Xerial `sqlite-jdbc` 3.46.1.3 (in `lib/` and declared in `pom.xml`).
- **Build**: `javac` (working) or Maven (`pom.xml` provided).

## Design patterns and real workflow integration

| Pattern | Classes involved | Real workflow where used |
|---|---|---|
| **Singleton** | `singleton.OrderManager` | `FoodDeliveryService.createOrders` calls `OrderManager.getInstance().manageOrder(...)` for every created order. |
| **Factory Method** | `factory.OrderCreator`, `VegOrderCreator`, `NonVegOrderCreator`, `ComboOrderCreator` | `FoodDeliveryService.createOrders` picks the creator per cart item from the item's actual category: VEG → `VegOrderCreator`, NON-VEG → `NonVegOrderCreator`, COMBO → `ComboOrderCreator`, DRINKS → `ComboOrderCreator` (documented mapping, see below). |
| **Abstract Factory** | `abstractfactory.RestaurantFactory`, `PizzaHubFactory`, `SpiceHubFactory`, `Food`, `Drink` | `repository.InMemoryRestaurantRepository` supplies menus from the factories; `FoodDeliveryService.findMenuItem` resolves canonical items across restaurants/categories. No menu duplication. |
| **Chain of Responsibility** | `chain.OrderHandler`, `PaymentHandler`, `RestaurantHandler`, `DeliveryHandler` | `FoodDeliveryService.validate(order)` runs the chain on every cart item before any order is committed. |
| **Proxy** | `proxy.PaymentGateway`, `proxy.PaymentGatewayProxy`, `proxy.RealPaymentGateway` | UPI/Card payments go through `PaymentGatewayProxy` guarding `RealPaymentGateway` (demo mode). |
| **Observer** | `observer.OrderStatusSubject`, `observer.OrderStatus`, `observer.CustomerNotificationObserver`, `observer.RestaurantNotificationObserver` | Each created order gets a status subject; customer/restaurant observers are notified on status changes. Status is persisted and survives restart. |
| **Strategy** | `strategy.PaymentService`, `strategy.PaymentStrategy`, `UPIPaymentStrategy`, `CardPaymentStrategy`, `CashOnDeliveryStrategy` | `FoodDeliveryService.pay(order, method)` selects a strategy; strategies return boolean success/failure (never treating `false` as success). |
| **Memento** | `memento.ShoppingCart`, `memento.CartMemento`, `memento.CartItem` | Cart save/restore snapshots via `/api/cart/save` and `/api/cart/restore`. Duplicate adds increase quantity (one row). |
| **Builder** | `builder.OrderBuilder` | `FoodDeliveryService.createOrders` calls `OrderBuilder.build()` to validate each order before the Factory Method constructs it. |
| **Prototype** | `prototype.Prototype<T>`, `models.Order.copy()` | `/api/orders/{id}/reorder` copies an existing order into a new draft with a new id; the original persisted id is never reused. |

**DRINKS classification:** The model defines VEG / NON-VEG / COMBO order types but has no dedicated drink order type. DRINKS cart items are checked out through `ComboOrderCreator` (the closest existing creator) — a documented simplification; a dedicated `DrinkOrderCreator` can be added later without changing the workflow.

## Project structure

```
SmartFoodDeliverySystem/
├── pom.xml                                  # Maven build config (sqlite-jdbc 3.46.1.3)
├── lib/sqlite-jdbc-3.46.1.3.jar             # SQLite JDBC driver (runtime dependency)
├── src/
│   ├── Main.java                            # Original console entry point (unchanged)
│   ├── web/HttpServerApp.java               # Browser entry: HTTP server + REST API + static files
│   ├── web/ApiHandler.java                  # Base REST handler (JSON helpers)
│   ├── web/JsonUtil.java                    # Dependency-free JSON serializer
│   ├── web/AuthHandler.java                 # /api/auth: register/login/logout/me
│   ├── web/RestaurantHandler.java           # /api/restaurants, menu
│   ├── web/CartHandler.java                 # /api/cart: CRUD + Memento save/restore
│   ├── web/OrderHandler.java                # /api/orders: create/pay/status/reorder/list
│   ├── service/FoodDeliveryService.java     # Application service; orchestrates all patterns
│   ├── service/AuthService.java             # Authentication: PBKDF2 hashing + server sessions
│   ├── controller/OrderController.java      # Existing MVC controller (console demo)
│   ├── models/                              # Order, FoodItem, Restaurant, User, VegOrder, NonVegOrder, ComboOrder
│   ├── repository/                          # Repository interfaces + implementations
│   │   ├── RestaurantRepository.java        # (Abstract Factory-backed menus)
│   │   ├── OrderRepository.java             # save/findById/findAll/status/ownership
│   │   ├── UserRepository.java              # save/findByEmail/findById/emailExists
│   │   ├── InMemoryRestaurantRepository.java
│   │   ├── InMemoryOrderRepository.java     # Dev/test (non-persistent)
│   │   ├── InMemoryUserRepository.java      # Dev/test (non-persistent)
│   │   ├── DatabaseOrderRepository.java     # JDBC (production)
│   │   └── DatabaseUserRepository.java      # JDBC (production)
│   ├── dao/                                 # JDBC data access
│   │   ├── OrderDAO.java                    # interface
│   │   └── JDBCOrderDAO.java                # prepared statements + safe migration
│   ├── database/DBConnection.java           # SQLite connection (jdbc:sqlite:smartfooddelivery.db)
│   ├── singleton/                           # OrderManager
│   ├── factory/                             # OrderCreator family (Factory Method)
│   ├── abstractfactory/                     # RestaurantFactory family (Abstract Factory)
│   ├── chain/                               # OrderHandler chain (Chain of Responsibility)
│   ├── proxy/                               # PaymentGateway + Proxy + Real (Proxy)
│   ├── observer/                            # OrderStatusSubject + observers (Observer)
│   ├── strategy/                            # PaymentService + strategies (Strategy)
│   ├── memento/                             # ShoppingCart + CartMemento + CartItem (Memento)
│   ├── builder/                             # OrderBuilder (Builder)
│   └── prototype/                           # Prototype<T> (Prototype)
├── frontend/
│   ├── index.html, restaurants.html, menu.html, sign-in.html, sign-up.html,
│   │   cart.html, checkout.html, order-confirmation.html, order-status.html
│   ├── css/style.css
│   └── js/*.js                              # api.js, app.js, home.js, menu.js, cart.js, checkout.js, order.js, restaurants.js
├── out/                                     # Compiled classes (gitignored)
└── README.md
```

## Prerequisites

- **JDK 25** (tested: Temurin/OpenJDK 25.0.4 LTS) — `javac` and `java` on PATH.
- **SQLite JDBC driver**: already bundled at `lib/sqlite-jdbc-3.46.1.3.jar` and declared in `pom.xml`.
- No Node.js / npm required. No external web framework.

## Build commands (PowerShell, run from the project root)

### Option A — javac (works without Maven)

```powershell
javac -d out -cp "lib/sqlite-jdbc-3.46.1.3.jar" -sourcepath src $(Get-ChildItem -Recurse src -Filter *.java | Select-Object -ExpandProperty FullName)
```

### Option B — Maven (if Maven is installed)

```powershell
mvn -q compile
mvn -q package
```

## Database initialization

No manual schema script is required. On startup the JDBC repositories run `CREATE TABLE IF NOT EXISTS` for `orders` and `users`, plus a safe migration that adds any missing columns (`status`, `user_id`) without dropping existing data. The database file `smartfooddelivery.db` is created in the working directory.

Persistence mode is controlled by `smartfood.persistence` (system property) or `SMARTFOOD_PERSISTENCE` (env var):
- `jdbc` (default): persistent SQLite storage.
- `memory`: in-memory repositories (dev/test only; data lost on restart).

## Backend start command

```powershell
java --enable-native-access=ALL-UNNAMED -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" web.HttpServerApp
```

You should see:

```
SmartFoodDeliverySystem server started on http://localhost:8080
Frontend: http://localhost:8080/index.html
API base: http://localhost:8080/api
```

The port is configurable via `SMARTFOOD_PORT` or `-Dsmartfood.port=...` (default 8080).

## Browser URL

Open **http://localhost:8080/index.html** while the server is running.

## API routes

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/restaurants` | List restaurants (Abstract Factory) | No |
| GET | `/api/restaurants/{id}` | Restaurant details | No |
| GET | `/api/restaurants/{id}/menu?category=VEG\|NON-VEG\|COMBO\|DRINKS` | Menu for a category | No |
| GET | `/api/cart?session=...` | Cart items + total (backend source of truth) | No |
| POST | `/api/cart/add` | `itemId`, `quantity` (backend resolves canonical item/price) | No |
| POST | `/api/cart/quantity` | `itemId`, `quantity` | No |
| POST | `/api/cart/remove` | `itemId` | No |
| POST | `/api/cart/save` | Save cart snapshot (Memento token) | No |
| POST | `/api/cart/restore` | Restore cart from token | No |
| POST | `/api/auth/register` | `name`, `email`, `password` | No |
| POST | `/api/auth/login` | `email`, `password` → returns session token | No |
| POST | `/api/auth/logout` | `session` | Yes |
| GET | `/api/auth/me?session=...` | Current user | Yes |
| GET | `/api/orders?session=...` | Current user's orders | Yes |
| GET | `/api/orders/{id}?session=...` | Order details (owner only) | Yes |
| GET | `/api/orders/{id}/status?session=...` | Order status (Observer) | Yes |
| POST | `/api/orders` | `session`, `customerName` → creates one order per cart item | Yes |
| POST | `/api/orders/{id}/pay` | `session`, `method` (COD/UPI/Card) | Yes (owner) |
| POST | `/api/orders/{id}/advance-status` | `session` — demo status advance | Yes (owner) |
| POST | `/api/orders/{id}/reorder` | `session` — Prototype copy | Yes (owner) |
| GET | `/api/payment/methods` | Supported payment methods | No |

## Authentication configuration

- Registration validates name (≥2 chars), valid email, password (≥6 chars); duplicate email is rejected.
- Passwords are hashed with **PBKDF2WithHmacSHA256** (16-byte random salt, 65,536 iterations, 16-byte hash). Plain-text passwords are never stored or returned.
- Login issues a server-recognized session token stored in browser `localStorage` (`sfds_session`). An arbitrary browser-generated identifier is **not** accepted as proof of authentication.
- Sessions expire after 30 minutes of inactivity and are invalidated on logout.
- `GET /api/auth/me` returns the current user; order reads/writes require a valid session and order ownership.

## Payment mode and limitations

- `RealPaymentGateway` is a clearly labelled **demo simulation**. It prints `[DEMO MODE] ... no money is charged.` and validates the amount (rejects null, NaN, infinite, ≤0, and >₹100,000). It returns `true` only for valid amounts — a simulated success, not a real completed payment.
- `PaymentGatewayProxy` rejects invalid amounts (>₹10,000) before delegating; strategies propagate the boolean result so a `false` is never treated as success.
- No real card details are collected. COD bypasses the proxy (pay on delivery).
- To configure a real sandbox gateway: implement `proxy.PaymentGateway` against the provider's SDK, wire it into `RealPaymentGateway` (or replace it), provide credentials via environment variables (never in source control), and set `smartfood.payment.mode=live` if you add such a switch.

## Test commands (PowerShell, from the project root)

Tests are plain Java `main` classes (no JUnit) that exercise the browser-to-backend flow and are executed with `java`. All were run and passed.

```powershell
# Compile everything (main + tests)
javac -d out -cp "lib/sqlite-jdbc-3.46.1.3.jar" -sourcepath "src;src/test" $(Get-ChildItem -Recurse src -Filter *.java | Select-Object -ExpandProperty FullName)

# 1. Static file security (path traversal denied, 404 for missing)
java -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" StaticFileSecurityTest

# 2. Cart dedup, per-item order types, multi-item total, persistence across restart
java -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" ServiceCartOrderTest

# 3. Authentication (register/login/wrong credentials/duplicate email/session/order ownership)
java -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" AuthTest
```

Exit code `0` = all passed. A non-zero exit means a failure.

## Common troubleshooting

| Problem | Cause / fix |
|---|---|
| `javac: package com.sun.net.httpserver does not exist` | JDK 25 includes it; ensure you're using JDK 25, not an older JDK. |
| `No suitable driver found for jdbc:sqlite` | The `lib/sqlite-jdbc-3.46.1.3.jar` must be on the classpath (`-cp "out;lib/sqlite-jdbc-3.46.1.3.jar"`). |
| `System::load` warning | Add `--enable-native-access=ALL-UNNAMED` to the `java` command (harmless if absent). |
| Frontend shows "backend not running" | Start `web.HttpServerApp` first and open `http://localhost:8080/index.html` (not `file://`). |
| Sign in/register returns 400/401 | Check validation errors in the response; duplicate email or wrong password are rejected by design. |
| "You cannot access another customer's order" (403) | Expected — orders are owner-scoped. Sign in as the order's owner. |
| Port already in use | Set `SMARTFOOD_PORT` or `-Dsmartfood.port=<port>`. |

## Persistent storage notes

Orders and users are persisted through JDBC/DAO behind the repository interfaces. In-memory implementations exist only as an explicitly labelled dev/test mode (`smartfood.persistence=memory`) and are **not** the production configuration. The database is initialized automatically; existing tables and data are never dropped.
