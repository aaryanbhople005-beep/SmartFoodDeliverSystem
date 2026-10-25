package service;

import abstractfactory.PizzaHubFactory;
import abstractfactory.RestaurantFactory;
import abstractfactory.SpiceHubFactory;
import builder.OrderBuilder;
import chain.DeliveryHandler;
import chain.OrderHandler;
import chain.PaymentHandler;
import chain.RestaurantHandler;
import factory.ComboOrderCreator;
import factory.NonVegOrderCreator;
import factory.OrderCreator;
import factory.VegOrderCreator;
import models.FoodItem;
import models.Order;
import models.Restaurant;
import models.User;
import observer.CustomerNotificationObserver;
import observer.OrderStatus;
import observer.OrderStatusSubject;
import observer.RestaurantNotificationObserver;
import proxy.PaymentGatewayProxy;
import repository.DatabaseOrderRepository;
import repository.DatabaseUserRepository;
import repository.InMemoryOrderRepository;
import repository.InMemoryRestaurantRepository;
import repository.InMemoryUserRepository;
import repository.OrderRepository;
import repository.RestaurantRepository;
import repository.UserRepository;
import strategy.CashOnDeliveryStrategy;
import strategy.CardPaymentStrategy;
import strategy.PaymentService;
import strategy.PaymentStrategy;
import strategy.UPIPaymentStrategy;
import prototype.Prototype;
import memento.CartItem;
import memento.CartMemento;
import memento.ShoppingCart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ApplicationService is the orchestrator for the browser-based food delivery app.
 *
 * It exposes small, coherent operations (get restaurants, get menu, cart CRUD,
 * create order, pay, get status, reorder) that the REST controllers call. All
 * business behavior still flows through the existing design patterns; this class
 * does not re-implement them.
 *
 * Persistence is behind repository interfaces so JDBC/DAO can be dropped in later.
 * The current implementations are in-memory and explicitly temporary (no database
 * connection).
 *
 * Patterns integrated here:
 *   - Abstract Factory -> RestaurantRepository provides menus (no menu duplication)
 *   - Factory Method     -> OrderCreator family creates orders by type (used in createOrders)
 *   - Builder            -> OrderBuilder validates and constructs order data (used in createOrders)
 *   - Singleton          -> OrderManager.registerOrder
 *   - Chain of Resp.     -> PaymentHandler -> RestaurantHandler -> DeliveryHandler
 *   - Proxy              -> PaymentGatewayProxy used by UPI/Card strategies
 *   - Strategy           -> PaymentService selects UPI/Card/COD strategy
 *   - Observer           -> OrderStatusSubject notifies customer/restaurant observers
 *   - Prototype          -> Order.copy() used for reorder
 *   - Memento            -> ShoppingCart.save()/restore() cart snapshots
 *   - MVC                -> service + controllers separate model from view
 *
 * The UI never calls these directly; it goes through the HTTP API.
 */
public class FoodDeliveryService {

    private final RestaurantRepository restaurantRepo;
    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final AuthService authService;
    private final ConcurrentMap<String, ShoppingCart> sessionCarts = new ConcurrentHashMap<>();

    /* In-memory dev/test mode flag. */
    private final boolean inMemoryMode;

    public FoodDeliveryService() {
        this.restaurantRepo = new InMemoryRestaurantRepository();
        this.inMemoryMode = "memory".equalsIgnoreCase(persistenceMode());
        this.orderRepo = createOrderRepository();
        this.userRepo = createUserRepository();
        this.authService = new AuthService(userRepo);
    }

    /** Exposed constructor for tests that want to force repositories. */
    FoodDeliveryService(OrderRepository orderRepo, RestaurantRepository restaurantRepo, AuthService authService) {
        this.orderRepo = orderRepo;
        this.restaurantRepo = restaurantRepo;
        this.authService = authService;
        this.userRepo = null;
        this.inMemoryMode = orderRepo instanceof InMemoryOrderRepository;
    }

    private static OrderRepository createOrderRepository() {
        String mode = persistenceMode();
        if ("jdbc".equalsIgnoreCase(mode)) {
            try {
                return new DatabaseOrderRepository();
            } catch (IllegalStateException e) {
                System.err.println("[WARN] JDBC persistence unavailable, falling back to in-memory: "
                        + e.getMessage());
                return new InMemoryOrderRepository();
            }
        }
        return new InMemoryOrderRepository();
    }

    private static UserRepository createUserRepository() {
        String mode = persistenceMode();
        if ("jdbc".equalsIgnoreCase(mode)) {
            try {
                return new DatabaseUserRepository();
            } catch (Exception e) {
                System.err.println("[WARN] JDBC user persistence unavailable, falling back to in-memory: "
                        + e.getMessage());
                return new InMemoryUserRepository();
            }
        }
        return new InMemoryUserRepository();
    }

    private static String persistenceMode() {
        String prop = System.getProperty("smartfood.persistence");
        String env = System.getenv("SMARTFOOD_PERSISTENCE");
        String value = (prop != null && !prop.isBlank()) ? prop : env;
        if (value == null || value.isBlank()) {
            return "jdbc"; // production: persistent JDBC storage
        }
        return value;
    }

    /**
     * Returns true when the service is running in in-memory dev/test mode
     * (non-persistent). Exposed for tests and documentation.
     */
    public boolean isInMemoryMode() {
        return inMemoryMode;
    }

    /* ---------------- Authentication ---------------- */

    public User register(String name, String email, String password) {
        return authService.register(name, email, password);
    }

    public String login(String email, String password) {
        return authService.login(email, password);
    }

    public void logout(String sessionId) {
        authService.logout(sessionId);
    }

    public User getCurrentUser(String sessionId) {
        return authService.getCurrentUser(sessionId);
    }

    public boolean isAuthenticated(String sessionId) {
        return authService.isAuthenticated(sessionId);
    }

    /**
     * Resolves the authenticated user id for a session, throwing if the session
     * is missing or invalid. Order-writing operations require authentication.
     */
    public int requireUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Authentication required.");
        }
        Integer userId = authService.getUserId(sessionId);
        if (userId == null) {
            throw new IllegalArgumentException("Authentication required. Sign in first.");
        }
        return userId;
    }

    public Integer getUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        return authService.getUserId(sessionId);
    }

    public List<Order> getOrdersForUser(int userId) {
        List<Order> result = new ArrayList<>();
        for (Order o : orderRepo.findAll()) {
            Integer owner = orderRepo.getOrderUserId(o.getOrderId());
            if (owner != null && owner.equals(userId)) {
                result.add(o);
            }
        }
        return result;
    }

    public boolean ownsOrder(int orderId, int userId) {
        Integer owner = orderRepo.getOrderUserId(orderId);
        return owner != null && owner.equals(userId);
    }

    /* ---------- Restaurants (Abstract Factory) ---------- */

    public List<Restaurant> getRestaurants() {
        return restaurantRepo.findAll();
    }

    public Restaurant getRestaurant(String id) {
        return restaurantRepo.findById(id);
    }

    public FoodItem[] getMenu(String restaurantId, String category) {
        return restaurantRepo.getMenu(restaurantId, category);
    }

    public String getRestaurantName(String id) {
        return restaurantRepo.getRestaurantName(id);
    }

    /* ---------- Cart (Memento + per-session state) ---------- */

    private ShoppingCart getCart(String sessionId) {
        return sessionCarts.computeIfAbsent(sessionId, k -> new ShoppingCart());
    }

    public List<CartItem> getCartItems(String sessionId) {
        return new ArrayList<>(getCart(sessionId).getItems());
    }

    public double getCartTotal(String sessionId) {
        return getCart(sessionId).getTotal();
    }

    public boolean addToCart(String sessionId, String itemId, int quantity) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        if (quantity <= 0) {
            return false;
        }

        // Look up the canonical item through the restaurant repository / Abstract
        // Factory menus. Never trust name, price, category or restaurant supplied
        // by the browser.
        FoodItem canonical = findMenuItem(itemId);
        if (canonical == null) {
            return false; // unknown product
        }

        double price = canonical.getPrice();
        if (Double.isNaN(price) || Double.isInfinite(price) || price < 0) {
            return false; // invalid backend price
        }

        CartItem item = new CartItem(
                canonical.getItemId(),
                canonical.getName(),
                price,
                canonical.getCategory(),
                canonical.getRestaurant(),
                quantity
        );
        getCart(sessionId).addItem(item.copy());
        return true;
    }

    /**
     * Finds a menu item by its itemId across every restaurant and every category
     * exposed by the Abstract Factory. Menu data is never duplicated here; it is
     * read from the existing factory-backed repository.
     */
    private FoodItem findMenuItem(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (Restaurant r : restaurantRepo.findAll()) {
            for (String category : new String[]{"VEG", "NON-VEG", "COMBO", "DRINKS"}) {
                FoodItem[] items = restaurantRepo.getMenu(r.getId(), category);
                for (FoodItem f : items) {
                    if (itemId.equals(f.getItemId())) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public boolean updateQuantity(String sessionId, String itemId, int quantity) {
        try {
            getCart(sessionId).updateQuantity(itemId, quantity);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean removeFromCart(String sessionId, String itemId) {
        getCart(sessionId).removeItem(itemId);
        return true;
    }

    public void clearCart(String sessionId) {
        ShoppingCart cart = sessionCarts.remove(sessionId);
        if (cart != null) {
            for (CartItem item : new ArrayList<>(cart.getItems())) {
                cart.removeItem(item.getItemId());
            }
        }
    }

    /**
     * Saves a cart snapshot (Memento). Returns a serializable token representing
     * the current cart state. This token can later be passed back to restore().
     * Data is in-memory only.
     */
    public String saveCart(String sessionId) {
        CartMemento memento = getCart(sessionId).save();
        return mementoToToken(memento);
    }

    /**
     * Restores a cart from a previously saved token (Memento).
     */
    public boolean restoreCart(String sessionId, String token) {
        try {
            CartMemento memento = tokenToMemento(token);
            getCart(sessionId).restore(memento);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* ---------- Orders (Factory Method + Builder / Chain / Singleton / Proxy / Strategy / Observer / Prototype) ---------- */

    private int nextOrderId() {
        int max = 0;
        for (Order o : orderRepo.findAll()) {
            if (o.getOrderId() > max) {
                max = o.getOrderId();
            }
        }
        return max + 1;
    }

    /**
     * Creates one order per cart item, using the actual product category to
     * select the Factory Method creator for each item (no single checkout radio
     * button determines the type of every item).
     *
     * Flow per item:
     *   1. Resolve the canonical FoodItem from the backend (Abstract Factory).
     *   2. OrderBuilder validates order data (Builder pattern).
     *   3. Category-specific OrderCreator creates the order (Factory Method).
     *   4. OrderManager.getInstance().manageOrder(...) (Singleton).
     *   5. PaymentHandler -> RestaurantHandler -> DeliveryHandler (Chain).
     *   6. OrderStatusSubject + customer/restaurant observers (Observer).
     *
     * The complete cart is validated before any order is created or persisted,
     * so a failure on one item never leaves a partially created checkout. The
     * cart is cleared only after every order has been created successfully.
     *
     * @param sessionId   the cart session
     * @param customerName the customer placing the order
     * @return list of created orders (one per cart item)
     */
    public List<Order> createOrders(String sessionId, String customerName) {
        int userId = requireUserId(sessionId);

        List<CartItem> items = getCartItems(sessionId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required.");
        }

        // --- Phase 1: validate and build every order in memory first.
        // No order is persisted and the cart is not touched yet.
        List<Order> toCreate = new ArrayList<>();
        int nextId = nextOrderId();

        for (CartItem item : items) {
            // Canonical item from the backend, never from the browser.
            FoodItem canonical = findMenuItem(item.getItemId());
            if (canonical == null) {
                throw new IllegalArgumentException("Unknown product in cart: " + item.getItemId());
            }
            double price = canonical.getPrice();
            if (Double.isNaN(price) || Double.isInfinite(price) || price < 0) {
                throw new IllegalArgumentException("Invalid price for product: " + item.getItemId());
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for product: " + item.getItemId());
            }

            FoodItem foodItem = new FoodItem(
                    canonical.getItemId(),
                    canonical.getName(),
                    price,
                    canonical.getCategory(),
                    canonical.getRestaurant()
            );

            OrderBuilder.OrderType builderType = categoryToBuilderType(canonical.getCategory());
            OrderCreator creator = creatorForCategory(canonical.getCategory());
            if (creator == null) {
                throw new IllegalArgumentException("No order creator for category: " + canonical.getCategory());
            }

            // Builder: validate all required fields before construction.
            OrderBuilder builder = new OrderBuilder()
                    .orderId(nextId)
                    .customerName(customerName)
                    .foodItem(foodItem)
                    .quantity(item.getQuantity())
                    .orderType(builderType);
            builder.build(); // throws IllegalArgumentException if invalid

            // Factory Method: instantiate the category-specific order.
            Order order = creator.createOrder(nextId, customerName, foodItem, item.getQuantity());
            nextId++;

            toCreate.add(order);
        }

        // --- Phase 2: commit every order (Singleton, Chain, Observer, repo).
        for (Order order : toCreate) {
            // Singleton: register the order
            singleton.OrderManager.getInstance().manageOrder(
                    order.getOrderType() + " Order #" + order.getOrderId());

            // Chain of Responsibility validation
            boolean valid = validate(order);
            if (!valid) {
                throw new IllegalStateException("Order validation failed for item: " + order.getFoodItem().getName());
            }

            // Observer: register status subject and observers
            OrderStatusSubject subject = new OrderStatusSubject(order.getOrderId(), OrderStatus.PLACED);
            subject.registerObserver(new CustomerNotificationObserver(order.getCustomerName()));
            subject.registerObserver(new RestaurantNotificationObserver(order.getRestaurantName()));
            registerSubject(order.getOrderId(), subject);

            // Persist order
            orderRepo.save(order);
            orderRepo.updateStatus(order.getOrderId(), OrderStatus.PLACED);
            orderRepo.associateUser(order.getOrderId(), userId);
        }

        // Clear cart only after every order was created successfully.
        clearCart(sessionId);

        return toCreate;
    }

    private OrderCreator creatorForCategory(String category) {
        if (category == null) return null;
        switch (category.toUpperCase().replace('-', '_')) {
            case "VEG": return new VegOrderCreator();
            case "NON_VEG": return new NonVegOrderCreator();
            case "COMBO": return new ComboOrderCreator();
            case "DRINKS": return new ComboOrderCreator(); // see class doc for DRINKS classification
            default: return null;
        }
    }

    private OrderBuilder.OrderType categoryToBuilderType(String category) {
        if (category == null) return OrderBuilder.OrderType.COMBO;
        switch (category.toUpperCase().replace('-', '_')) {
            case "VEG": return OrderBuilder.OrderType.VEG;
            case "NON_VEG": return OrderBuilder.OrderType.NON_VEG;
            case "COMBO": return OrderBuilder.OrderType.COMBO;
            case "DRINKS": return OrderBuilder.OrderType.COMBO;
            default: return OrderBuilder.OrderType.COMBO;
        }
    }

    private boolean validate(Order order) {
        OrderHandler payment = new PaymentHandler();
        OrderHandler restaurant = new RestaurantHandler();
        OrderHandler delivery = new DeliveryHandler();
        payment.setNextHandler(restaurant).setNextHandler(delivery);
        return payment.validate(order);
    }

    public Order getOrder(int orderId) {
        return orderRepo.findById(orderId);
    }

    public OrderStatus getOrderStatus(int orderId) {
        return orderRepo.getStatus(orderId);
    }

    /**
     * Advances an order's status through Observer notification. Used to simulate
     * the order progressing (PLACED -> CONFIRMED -> ...). Not a real-time delivery
     * tracker; the backend reports the actual stored status.
     */
    public boolean advanceStatus(int orderId) {
        OrderStatusSubject subject = orderRepo.getSubject(orderId);
        if (subject == null) return false;
        OrderStatus current = subject.getStatus();
        OrderStatus next;
        switch (current) {
            case PLACED: next = OrderStatus.CONFIRMED; break;
            case CONFIRMED: next = OrderStatus.PREPARING; break;
            case PREPARING: next = OrderStatus.READY_FOR_PICKUP; break;
            case READY_FOR_PICKUP: next = OrderStatus.OUT_FOR_DELIVERY; break;
            case OUT_FOR_DELIVERY: next = OrderStatus.DELIVERED; break;
            default: return false;
        }
        subject.setStatus(next);
        orderRepo.updateStatus(orderId, next);
        return true;
    }

    public List<Map<String, String>> getPaymentMethods() {
        List<Map<String, String>> methods = new ArrayList<>();
        methods.add(Map.of("id", "UPI", "label", "Unified Payments Interface"));
        methods.add(Map.of("id", "CARD", "label", "Credit/Debit Card"));
        methods.add(Map.of("id", "COD", "label", "Cash on Delivery"));
        return methods;
    }

    /**
     * Processes payment via Strategy + Proxy. The selected strategy determines
     * which proxy-based payment path runs; COD bypasses the proxy as in the
     * existing project.
     */
    public PaymentResult pay(int orderId, String method) {
        Order order = orderRepo.findById(orderId);
        if (order == null) {
            return new PaymentResult(false, "Order not found.", null);
        }

        strategy.PaymentService paymentService = new strategy.PaymentService();
        PaymentStrategy strategy = strategyFor(method, order);
        paymentService.setPaymentStrategy(strategy);

        try {
            boolean ok = paymentService.processPayment(order);
            return new PaymentResult(ok, ok ? "Payment processed." : "Payment failed.", method);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return new PaymentResult(false, e.getMessage(), method);
        }
    }

    private PaymentStrategy strategyFor(String method, Order order) {
        if ("UPI".equalsIgnoreCase(method)) return new UPIPaymentStrategy();
        if ("CARD".equalsIgnoreCase(method)) return new CardPaymentStrategy();
        if ("COD".equalsIgnoreCase(method)) return new CashOnDeliveryStrategy();
        throw new IllegalArgumentException("Unknown payment method: " + method);
    }

    /**
     * Reorders an existing order via Prototype (Order.copy()). The returned copy
     * can be reviewed and then submitted as a fresh order through createOrders().
     */
    public Order reorder(int orderId) {
        Order original = orderRepo.findById(orderId);
        if (original == null) return null;
        Prototype<Order> prototype = original;
        return prototype.copy();
    }

    /* ---------- Subject registry (kept with order repo for simplicity) ---------- */

    private final ConcurrentMap<Integer, OrderStatusSubject> subjects = new ConcurrentHashMap<>();

    private void registerSubject(int orderId, OrderStatusSubject subject) {
        subjects.put(orderId, subject);
    }

    public OrderStatusSubject getSubject(int orderId) {
        return subjects.get(orderId);
    }

    /* ---------- Light DTOs returned to controllers ---------- */

    public static class PaymentResult {
        public final boolean success;
        public final String message;
        public final String method;
        public PaymentResult(boolean success, String message, String method) {
            this.success = success; this.message = message; this.method = method;
        }
    }

    private String mementoToToken(CartMemento memento) {
        StringBuilder sb = new StringBuilder("cart:");
        List<CartItem> items = memento.getSavedItems();
        for (int i = 0; i < items.size(); i++) {
            CartItem it = items.get(i);
            sb.append(it.getItemId()).append(":").append(it.getName()).append(":")
              .append(it.getPrice()).append(":").append(it.getQuantity());
            if (i < items.size() - 1) sb.append("|");
        }
        return sb.toString();
    }

    private CartMemento tokenToMemento(String token) {
        ShoppingCart cart = new ShoppingCart();
        String body = token;
        if (body.startsWith("cart:")) {
            body = body.substring(5);
        }
        for (String part : body.split("\\|")) {
            String[] fields = part.split(":");
            if (fields.length >= 4) {
                cart.addItem(new CartItem(fields[0], fields[1],
                        Double.parseDouble(fields[2]),
                        Integer.parseInt(fields[3])));
            }
        }
        return cart.save();
    }
}