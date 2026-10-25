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
import observer.CustomerNotificationObserver;
import observer.OrderStatus;
import observer.OrderStatusSubject;
import observer.RestaurantNotificationObserver;
import proxy.PaymentGatewayProxy;
import repository.InMemoryOrderRepository;
import repository.InMemoryRestaurantRepository;
import repository.OrderRepository;
import repository.RestaurantRepository;
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

    private final RestaurantRepository restaurantRepo = new InMemoryRestaurantRepository();
    private final OrderRepository orderRepo = new InMemoryOrderRepository();
    private final ConcurrentMap<String, ShoppingCart> sessionCarts = new ConcurrentHashMap<>();

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

    public boolean addToCart(String sessionId, CartItem item) {
        if (item == null) {
            return false;
        }
        // Validate price and quantity before adding
        if (item.getPrice() < 0 || item.getQuantity() <= 0) {
            return false;
        }
        getCart(sessionId).addItem(item.copy());
        return true;
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
     * Creates one order per cart item.
     *
     * Flow per item:
     *   1. OrderBuilder validates and constructs order data          (Builder pattern)
     *   2. OrderCreator.createOrder() instantiates type-specific order  (Factory Method pattern)
     *   3. OrderManager.getInstance().manageOrder(...)             (Singleton pattern)
     *   4. PaymentHandler -> RestaurantHandler -> DeliveryHandler  (Chain of Responsibility)
     *   5. OrderStatusSubject registers customer/restaurant observers (Observer pattern)
     *
     * @param sessionId   the cart session
     * @param orderType   "VEG", "NON_VEG", or "COMBO"
     * @param customerName the customer placing the order
     * @return list of created orders (one per cart item)
     */
    public List<Order> createOrders(String sessionId, String orderType, String customerName) {
        List<CartItem> items = getCartItems(sessionId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required.");
        }

        OrderBuilder.OrderType builderType;
        try {
            builderType = OrderBuilder.OrderType.valueOf(orderType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown order type: " + orderType);
        }

        OrderCreator creator = creatorFor(orderType);
        if (creator == null) {
            throw new IllegalArgumentException("Unknown order type: " + orderType);
        }

        List<Order> createdOrders = new ArrayList<>();
        int nextId = nextOrderId();

        for (CartItem item : items) {
            // Build FoodItem from cart item data (use actual category and restaurant, not hardcoded)
            FoodItem foodItem = new FoodItem(
                    item.getItemId(),
                    item.getName(),
                    item.getPrice(),
                    item.getCategory() != null ? item.getCategory() : "VEG",
                    item.getRestaurant()
            );

            // --- Builder pattern: validate and construct order data ---
            OrderBuilder builder = new OrderBuilder()
                    .orderId(nextId)
                    .customerName(customerName)
                    .foodItem(foodItem)
                    .quantity(item.getQuantity())
                    .orderType(builderType);
            // builder.build() validates all required fields are present and positive
            builder.build();

            // --- Factory Method pattern: creator instantiates the type-specific order ---
            Order order = creator.createOrder(nextId, customerName, foodItem, item.getQuantity());
            nextId++;

            // Singleton: register the order
            singleton.OrderManager.getInstance().manageOrder(
                    order.getOrderType() + " Order #" + order.getOrderId());

            // Chain of Responsibility validation
            boolean valid = validate(order);
            if (!valid) {
                throw new IllegalStateException("Order validation failed for item: " + foodItem.getName());
            }

            // Observer: register status subject and observers
            OrderStatusSubject subject = new OrderStatusSubject(order.getOrderId(), OrderStatus.PLACED);
            subject.registerObserver(new CustomerNotificationObserver(customerName));
            subject.registerObserver(new RestaurantNotificationObserver(order.getRestaurantName()));
            registerSubject(order.getOrderId(), subject);

            // Save to repository
            orderRepo.save(order);
            orderRepo.updateStatus(order.getOrderId(), OrderStatus.PLACED);

            createdOrders.add(order);
        }

        // Clear cart after all orders placed
        clearCart(sessionId);

        return createdOrders;
    }

    private OrderCreator creatorFor(String orderType) {
        if (orderType == null) return null;
        switch (orderType.toUpperCase()) {
            case "VEG": return new VegOrderCreator();
            case "NON_VEG": return new NonVegOrderCreator();
            case "COMBO": return new ComboOrderCreator();
            default: return null;
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
        OrderStatusSubject subject = ((InMemoryOrderRepository) orderRepo).getSubject(orderId);
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
            paymentService.processPayment(order);
            return new PaymentResult(true, "Payment processed.", method);
        } catch (IllegalStateException e) {
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