# SmartFoodDeliverySystem - Complete Implementation Documentation

## Project Overview
**SmartFoodDeliverySystem** is a Java-based food delivery web application demonstrating 10+ Design Patterns with persistent SQLite storage. The system features a browser-based frontend (HTML/CSS/JS) and a Java backend with a lightweight HTTP server.

---

## 1. DESIGN PATTERNS IMPLEMENTED

### 1.1 Abstract Factory Pattern
**Location:** `src/abstractfactory/`
- **Interfaces:** `RestaurantFactory`, `Food`, `Drink`
- **Concrete Factories:** `PizzaHubFactory`, `SpiceHubFactory`
- **Purpose:** Creates restaurant-specific menu items (Food/Drink) without coupling client code to concrete classes
- **Integration:** `DatabaseRestaurantRepository` loads menu data from SQLite, factories provide behavioral methods (`createFood()`, `createDrink()`)

### 1.2 Factory Method Pattern
**Location:** `src/factory/`
- **Creator Interface:** `OrderCreator`
- **Concrete Creators:** `VegOrderCreator`, `NonVegOrderCreator`, `ComboOrderCreator`
- **Product:** `Order` (and subtypes `VegOrder`, `NonVegOrder`, `ComboOrder`)
- **Purpose:** Each order category uses its own creator; `FoodDeliveryService` selects creator based on item category

### 1.3 Builder Pattern
**Location:** `src/builder/OrderBuilder.java`
- **Purpose:** Validates and constructs `Order` objects step-by-step with fluent API
- **Usage:** `FoodDeliveryService.createOrders()` uses builder to validate required fields before order creation

### 1.4 Singleton Pattern
**Location:** `src/singleton/OrderManager.java`
- **Purpose:** Centralized order registration and management
- **Usage:** `OrderManager.getInstance().manageOrder()` called for every created order

### 1.5 Chain of Responsibility Pattern
**Location:** `src/chain/`
- **Handlers:** `PaymentHandler` → `RestaurantHandler` → `DeliveryHandler`
- **Purpose:** Validates orders through a pipeline (payment → restaurant → delivery)
- **Usage:** `FoodDeliveryService.validate(order)` executes the chain

### 1.6 Proxy Pattern
**Location:** `src/proxy/`
- **Subject:** `PaymentGateway`
- **Proxy:** `PaymentGatewayProxy` (controls access, adds logging/validation)
- **Real Subject:** `PaymentGatewayImpl` (simulates external payment processing)

### 1.7 Strategy Pattern
**Location:** `src/strategy/`
- **Context:** `PaymentService`
- **Strategies:** `UPIPaymentStrategy`, `CardPaymentStrategy`, `CashOnDeliveryStrategy`
- **Purpose:** Interchangeable payment algorithms selected at runtime

### 1.8 Observer Pattern
**Location:** `src/observer/`
- **Subject:** `OrderStatusSubject` (extends `Observable`)
- **Observers:** `CustomerNotificationObserver`, `RestaurantNotificationObserver`
- **Purpose:** Notifies observers when order status changes

### 1.9 Prototype Pattern
**Location:** `src/prototype/Prototype.java`, `models/Order.java`
- **Purpose:** Cloning orders for reorder functionality
- **Usage:** `FoodDeliveryService.reorder(orderId)` returns `Order.copy()`

### 1.10 Memento Pattern
**Location:** `src/memento/`
- **Originator:** `ShoppingCart`
- **Memento:** `CartMemento` (immutable snapshot of cart items)
- **Caretaker:** `DatabaseCartRepository` (persists/restores snapshots in SQLite)
- **Token Format:** `cart:itemId:name:price:quantity|itemId:name:price:quantity`

---

## 2. DATABASE ARCHITECTURE

### 2.1 SQLite Database File
**File:** `smartfooddelivery.db` (created automatically on first run)

### 2.2 Tables

| Table | Columns | Purpose |
|-------|---------|---------|
| **restaurants** | `id` (PK), `name`, `description`, `category` | Restaurant catalog |
| **menu_items** | `id` (PK), `item_id`, `restaurant_id` (FK), `name`, `price`, `category` | Menu items per restaurant |
| **users** | `id` (PK), `name`, `email` (UNIQUE), `password_hash`, `created_at` | User accounts with PBKDF2 hashes |
| **carts** | `session_id` (PK), `user_id` (FK), `created_at`, `updated_at` | One cart per session, linked to user |
| **cart_items** | `id` (PK), `session_id` (FK), `item_id`, `name`, `price`, `category`, `restaurant`, `quantity` | Cart contents |
| **cart_snapshots** | `id` (PK), `session_id` (FK), `user_id` (FK), `token`, `cart_data`, `created_at` | Memento snapshots |
| **sessions** | `token` (PK), `user_id` (FK), `expiry_ms`, `created_at` | Persistent auth sessions |

### 2.3 Indexes
- `idx_menu_items_restaurant` on `menu_items(restaurant_id)`
- `idx_cart_items_session` on `cart_items(session_id)`
- `idx_cart_snapshots_user` on `cart_snapshots(user_id)`
- `idx_sessions_expiry` on `sessions(expiry_ms)`
- `idx_sessions_user` on `sessions(user_id)`

### 2.4 Foreign Key Constraints
- `menu_items.restaurant_id` → `restaurants(id)` ON DELETE CASCADE
- `carts.user_id` → `users(id)` ON DELETE SET NULL
- `cart_items.session_id` → `carts(session_id)` ON DELETE CASCADE
- `cart_snapshots.session_id` → `carts(session_id)` ON DELETE CASCADE
- `cart_snapshots.user_id` → `users(id)` ON DELETE SET NULL
- `sessions.user_id` → `users(id)` ON DELETE CASCADE

### 2.5 Seed Data (Auto-inserted on first run)
- **2 Restaurants:** Pizza Hub, Spice Hub
- **24 Menu Items:** 12 per restaurant across VEG, NON-VEG, COMBO, DRINKS categories

### 2.5 Persistence Mode Configuration
**System Property:** `smartfood.persistence`
- `jdbc` (default): Uses SQLite-backed repositories
- `memory`: Uses in-memory repositories (for testing)

---

## 3. BACKEND ARCHITECTURE

### 3.1 Core Services
| Class | Responsibility |
|-------|----------------|
| `FoodDeliveryService` | Main orchestrator - coordinates all patterns, cart management, orders, payments |
| `AuthService` | Registration, login, logout, session management, PBKDF2 password hashing |
| `PaymentService` | Strategy context for payment processing |

### 3.2 Repository Layer (Interface-Based)
| Interface | JDBC Implementation | In-Memory Implementation |
|-----------|---------------------|-------------------------|
| `RestaurantRepository` | `DatabaseRestaurantRepository` | `InMemoryRestaurantRepository` |
| `OrderRepository` | `DatabaseOrderRepository` | `InMemoryOrderRepository` |
| `UserRepository` | `DatabaseUserRepository` | `InMemoryUserRepository` |
| - | `DatabaseCartRepository` (new) | - |
| - | `DatabaseSessionRepository` (new) | - |

### 3.3 HTTP Server
**Class:** `web/HttpServerApp.java`
- **Port:** 8080 (configurable via `SMARTFOOD_PORT` or `smartfood.port`)
- **Static Files:** Serves from `frontend/` directory with path traversal protection
- **API Base:** `/api`
- **Executor:** Cached thread pool

### 3.4 REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/restaurants` | GET | List all restaurants |
| `/api/restaurants/{id}` | GET | Get restaurant by ID |
| `/api/restaurants/{id}/menu?category={cat}` | GET | Get menu items for restaurant/category |
| `/api/auth/register` | POST | Register new user (name, email, password) |
| `/api/auth/login` | POST | Login (email, password) → returns session token |
| `/api/auth/logout` | POST | Logout (session) |
| `/api/auth/me` | GET | Get current user (session) |
| `/api/cart` | GET | Get cart items + total (session) |
| `/api/cart/add` | POST | Add item to cart (session, itemId, quantity) |
| `/api/cart/quantity` | POST | Update item quantity (session, itemId, quantity) |
| `/api/cart/remove` | POST | Remove item from cart (session, itemId) |
| `/api/cart/save` | POST | Save cart snapshot (session) → returns token |
| `/api/cart/restore` | POST | Restore cart from token (session, token) |
| `/api/cart/snapshots` | GET | List user's saved snapshots (session) |
| `/api/orders` | POST | Create orders from cart (session, customerName) |
| `/api/orders/{id}` | GET | Get order details |
| `/api/orders/status/{id}` | GET | Get order status |
| `/api/orders/advance/{id}` | POST | Advance order status (demo) |
| `/api/payment` | GET | List payment methods |
| `/api/payment/pay` | POST | Process payment (orderId, method) |

---

## 4. FRONTEND ARCHITECTURE

### 4.1 Pages
| File | Description |
|------|-------------|
| `index.html` | Home page with hero, featured items, database storage info |
| `restaurants.html` | Restaurant listing |
| `menu.html` | Restaurant menu with category tabs (VEG, NON-VEG, COMBO, DRINKS) |
| `cart.html` | Cart with quantity controls, Memento save/restore, snapshots list |
| `checkout.html` | Checkout form with payment method selection |
| `order-confirmation.html` | Order confirmation with Prototype reorder demo |
| `order-status.html` | Order status with Observer pattern demo |
| `sign-in.html` | Login with redirect preservation |
| `sign-up.html` | Registration with validation |

### 4.2 JavaScript Modules
| File | Exports |
|------|---------|
| `js/api.js` | `apiGet`, `apiPost`, `getSession`, `ensureSession`, `setSession`, `clearSession`, `toast` |
| `js/app.js` | `cart`, `loadCart`, `renderCartBadge`, `formatMoney`, `loadCurrentUser`, `currentUser`, `login`, `register`, `logout`, `addToCart`, `updateQuantity`, `removeFromCart`, `setupLogout`, `initTheme` |
| `js/theme.js` | `initTheme`, `toggleTheme`, `getPreferredTheme`, `applyTheme` (localStorage persistence) |
| `js/auth-guard.js` | `requireAuth`, `getRedirectAfterLogin`, `clearRedirect`, `getRedirectMessage` |
| `js/home.js` | `loadFeaturedItems` |
| `js/restaurants.js` | `loadRestaurants` |
| `js/menu.js` | `loadMenu`, category tab handling |
| `js/cart.js` | `renderCart`, `saveCart`, `restoreCart`, `loadSnapshots` |
| `js/checkout.js` | `loadCheckout`, payment method handling |
| `js/order.js` | `loadOrderConfirmation`, `loadOrderStatus` |

### 4.3 CSS Architecture
**File:** `css/style.css`
- **CSS Variables:** Light/Dark theme support via `[data-theme="dark"]` selector
- **Variables:** 30+ custom properties for colors, shadows, spacing, radii
- **Components:** Navbar, cards, buttons, forms, cart items, status pills, toasts
- **Responsive:** Mobile-first with breakpoints at 600px

### 4.4 Authentication Flow
1. **Unauthenticated:** Navbar shows Home, Sign In, Sign Up, Theme Toggle
2. **Login:** Redirects to intended page or Home, transfers guest cart → user cart
3. **Authenticated:** Navbar shows Home, Cart (with badge), User Name, Sign Out, Theme Toggle
4. **Protected Pages:** Cart, Checkout, Orders → redirect to login if no valid session
5. **Session Persistence:** Survives server restart via `sessions` table

---

## 5. KEY IMPLEMENTATION DETAILS

### 5.1 Cart Persistence Flow
```
Guest adds items → cart_items stored with session_id
         ↓
User logs in → AuthHandler.handleLogin()
         ↓
1. transferCart(oldSessionId, newSessionId) - copies items
2. loadUserCartForSession(userId, newSessionId) - loads user's saved cart
3. associateSessionWithUser(newSessionId, userId) - links session to user
         ↓
All future cart ops use new session_id linked to user
```

### 5.2 Memento Snapshot Flow
```
Save Cart:
1. FoodDeliveryService.saveCart(sessionId)
2. Creates ShoppingCart from cartRepo items
3. CartMemento = cart.save()
4. token = mementoToToken(memento)
5. DatabaseCartRepository.saveCartSnapshot(sessionId, userId, memento)
6. Returns token to frontend

Restore Cart:
1. Frontend calls /api/cart/restore with token
2. CartHandler → FoodDeliveryService.restoreCart(sessionId, token)
3. DatabaseCartRepository.restoreCart(sessionId, token)
3a. Validates token exists in cart_snapshots
3b. Parses token → CartMemento
3c. Clears current cart, adds items from memento
```

### 5.3 Session Management
- **Token:** 24-byte URL-safe Base64 (32 chars)
- **TTL:** 30 minutes sliding expiry
- **Storage:** `sessions` table with `expiry_ms`
- **Validation:** Checks expiry, refreshes on each access
- **Cleanup:** `cleanupExpiredSessions()` removes expired tokens

### 5.4 Password Security
- **Algorithm:** PBKDF2WithHmacSHA256
- **Salt:** 16 bytes random per password
- **Iterations:** 65,536
- **Hash Length:** 16 bytes
- **Format:** `pbkdf2_sha256$iterations$salt$hash` (Base64 encoded)

---

## 6. UI FEATURES

### 6.1 Theme System
- **Default:** Light mode
- **Toggle:** Sun/Moon icon in navbar
- **Persistence:** `localStorage` key `sfds_theme`
- **System Preference:** Respects `prefers-color-scheme` if no stored preference
- **Scope:** All pages (Home, Restaurants, Menu, Cart, Checkout, Orders, Auth)

### 6.2 Navigation Bar
**Before Login:** SmartFood Logo | Home | Sign In | Sign Up | Theme Toggle
**After Login:** SmartFood Logo | Home | Cart (with badge) | User Name | Sign Out | Theme Toggle

### 6.3 Cart Badge
- Shows total quantity across all items
- Updates in real-time on add/remove/quantity change
- Hidden when cart is empty

### 6.4 Toast Notifications
- Fixed bottom-center position
- Types: Success (green), Error (red), Info (dark)
- Auto-dismiss after 3 seconds

### 6.5 Category Tabs (Menu Page)
- VEG, NON-VEG, COMBO, DRINKS
- Horizontal scroll on mobile
- Active state highlighting

### 6.6 Order Status Pipeline
PLACED → CONFIRMED → PREPARING → READY_FOR_PICKUP → OUT_FOR_DELIVERY → DELIVERED
- Manual "Advance Status" button for demo
- Observer notifications on each transition

---

## 7. TESTING SCENARIOS VERIFIED

| Scenario | Status |
|----------|--------|
| Restaurant data loads from SQLite | ✅ |
| Menu items load from SQLite by category | ✅ |
| Add to cart stores in SQLite | ✅ |
| Cart quantity updates persist | ✅ |
| Remove from cart updates database | ✅ |
| Cart persists after page refresh | ✅ |
| Cart persists after server restart | ✅ |
| Save Cart creates database snapshot | ✅ |
| Multiple snapshots can be saved | ✅ |
| Restore Cart restores correct snapshot | ✅ |
| Restore does not duplicate items | ✅ |
| Login/logout work correctly | ✅ |
| Sessions persist after server restart | ✅ |
| Cart transfers guest→user on login | ✅ |
| User cart loads on subsequent logins | ✅ |
| Checkout creates orders | ✅ |
| Cart clears after successful order | ✅ |
| Order status advances via Observer | ✅ |
| Payment strategies work (COD/UPI/Card) | ✅ |
| Prototype reorder works | ✅ |
| Theme toggle persists across pages/refresh | ✅ |
| No JavaScript errors | ✅ |
| No database errors | ✅ |

---

## 8. FILE STRUCTURE

```
SmartFoodDeliverySystem/
├── src/
│   ├── abstractfactory/          # Abstract Factory Pattern
│   ├── builder/                  # Builder Pattern
│   ├── chain/                    # Chain of Responsibility
│   ├── database/                 # DBConnection (schema + seed)
│   ├── dao/                      # JDBCOrderDAO
│   ├── factory/                  # Factory Method Pattern
│   ├── memento/                  # Memento Pattern (ShoppingCart, CartItem, CartMemento)
│   ├── models/                   # Domain models (User, Order, FoodItem, Restaurant, CartItem)
│   ├── observer/                 # Observer Pattern
│   ├── prototype/                # Prototype Pattern
│   ├── proxy/                    # Proxy Pattern
│   ├── repository/               # Repository interfaces + implementations
│   ├── service/                  # AuthService, FoodDeliveryService, PaymentService
│   ├── singleton/                # OrderManager
│   ├── strategy/                 # Strategy Pattern (Payment)
│   └── web/                      # HTTP handlers (CartHandler, AuthHandler, etc.)
├── frontend/
│   ├── *.html                    # 9 pages
│   ├── css/style.css             # Complete styling with dark mode
│   └── js/
│       ├── api.js                # API client + session management
│       ├── app.js                # Shared app logic (cart, auth, theme)
│       ├── theme.js              # Theme persistence
│       ├── auth-guard.js         # Route protection
│       ├── home.js               # Home page logic
│       ├── restaurants.js        # Restaurant listing
│       ├── menu.js               # Menu with category tabs
│       ├── cart.js               # Cart + Memento UI
│       ├── checkout.js           # Checkout flow
│       └── order.js              # Order confirmation/status
├── lib/sqlite-jdbc-3.46.1.3.jar  # SQLite JDBC driver
└── pom.xml                       # Maven config (if used)
```

---

## 9. COMPILATION & EXECUTION

### 9.1 Requirements
- Java 17+
- SQLite JDBC driver (included in `lib/`)

### 9.2 Windows PowerShell Commands

```powershell
cd "D:\College\sem-5\LAB\Design Pattern LAB\LA-1\SmartFoodDeliverySystem"

# Compile
rm -r out -ErrorAction SilentlyContinue
mkdir out
javac -d out -cp "lib/sqlite-jdbc-3.46.1.3.jar" -sourcepath src (Get-ChildItem -Recurse src -Filter *.java | Select-Object -ExpandProperty FullName)

# Run
java -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" web.HttpServerApp
```

### 9.3 Access Application
- **Frontend:** http://localhost:8080
- **API Base:** http://localhost:8080/api

---

## 10. DESIGN PATTERN SUMMARY TABLE

| Pattern | Category | Key Classes | Database Integration |
|---------|----------|-------------|---------------------|
| Abstract Factory | Creational | `RestaurantFactory`, `PizzaHubFactory`, `SpiceHubFactory` | `DatabaseRestaurantRepository` loads from SQLite |
| Factory Method | Creational | `OrderCreator`, `VegOrderCreator`, `NonVegOrderCreator`, `ComboOrderCreator` | Orders persisted via `DatabaseOrderRepository` |
| Builder | Creational | `OrderBuilder` | Validates before DB persist |
| Singleton | Creational | `OrderManager` | In-memory registry |
| Chain of Responsibility | Behavioral | `PaymentHandler`, `RestaurantHandler`, `DeliveryHandler` | Validates before DB persist |
| Proxy | Structural | `PaymentGatewayProxy`, `PaymentGatewayImpl` | Wraps payment processing |
| Strategy | Behavioral | `PaymentService`, `UPIPaymentStrategy`, `CardPaymentStrategy`, `CashOnDeliveryStrategy` | Selected at runtime |
| Observer | Behavioral | `OrderStatusSubject`, `CustomerNotificationObserver`, `RestaurantNotificationObserver` | Status changes in DB + notify |
| Prototype | Creational | `Prototype`, `Order.copy()` | Cloned orders persisted as new |
| Memento | Behavioral | `ShoppingCart`, `CartMemento`, `DatabaseCartRepository` | Snapshots in `cart_snapshots` table |

---

## 11. SECURITY CONSIDERATIONS

- ✅ Passwords hashed with PBKDF2 (never stored in plain text)
- ✅ Session tokens are cryptographically random (24 bytes)
- ✅ Session expiry enforced (30 min sliding)
- ✅ SQL injection prevented via PreparedStatements
- ✅ Path traversal protection in static file server
- ✅ Foreign key constraints enforce referential integrity
- ✅ Cart data associated with authenticated user only
- ✅ No sensitive data in localStorage (only theme preference)

---

## 12. KNOWN LIMITATIONS / FUTURE ENHANCEMENTS

| Area | Current State | Enhancement |
|------|---------------|-------------|
| Payment | Demo mode (no real processing) | Integrate real payment gateway |
| Orders | Created per cart item | Support multi-item orders |
| Delivery | Status simulation only | Real driver tracking |
| Cart | One per session | Support multiple saved carts |
| Admin | No admin interface | Restaurant/menu management UI |
| Tests | E2E test class exists | Full unit test coverage |

---

*Document generated from SmartFoodDeliverySystem implementation as of 2026-10-26*