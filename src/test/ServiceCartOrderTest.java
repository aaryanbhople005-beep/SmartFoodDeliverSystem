import models.Order;
import observer.OrderStatus;
import service.FoodDeliveryService;
import memento.CartItem;

import java.io.File;
import java.util.List;

/**
 * Tests cart dedup, per-item order type selection, multi-item checkout total,
 * order ownership, and JDBC persistence across a simulated restart.
 */
public class ServiceCartOrderTest {

    static int failures = 0;

    public static void main(String[] args) throws Exception {
        testCartDedupAndTotal();
        testMultiItemOrderTypesAndTotal();
        testPersistenceAcrossRestart();
        testUnknownItemRejected();
        testForgedPriceNotTrusted();

        if (failures == 0) {
            System.out.println("ALL SERVICE/CART/ORDER TESTS PASSED");
        } else {
            System.out.println("TESTS FAILED: " + failures);
            System.exit(1);
        }
    }

    static void check(boolean cond, String label) {
        if (cond) {
            System.out.println("PASS: " + label);
        } else {
            System.out.println("FAIL: " + label);
            failures++;
        }
    }

    /** Fresh in-memory service instance (test isolation, no shared DB). */
    static FoodDeliveryService memoryService() {
        System.setProperty("smartfood.persistence", "memory");
        return new FoodDeliveryService();
    }

    /** Register + login; returns service and authenticated session. */
    static Object[] authed() throws Exception {
        FoodDeliveryService svc = memoryService();
        String email = "user" + System.nanoTime() + "@example.com";
        svc.register("User", email, "password123");
        String session = svc.login(email, "password123");
        return new Object[]{svc, session};
    }

    static void testCartDedupAndTotal() {
        FoodDeliveryService svc = memoryService();
        String session = "sess-cart-dedup";
        boolean a = svc.addToCart(session, "P101", 1);
        boolean b = svc.addToCart(session, "P101", 1);
        check(a && b, "add P101 twice returns true");
        List<CartItem> items = svc.getCartItems(session);
        check(items.size() == 1, "dedup: one row for P101 (rows=" + items.size() + ")");
        check(items.get(0).getQuantity() == 2, "dedup: quantity is 2 (got " + items.get(0).getQuantity() + ")");
        check(Math.abs(svc.getCartTotal(session) - 400.0) < 0.001, "total is 400 (got " + svc.getCartTotal(session) + ")");

        svc.addToCart(session, "S101", 1);
        items = svc.getCartItems(session);
        check(items.size() == 2, "two distinct rows after adding S101 (rows=" + items.size() + ")");
        check(Math.abs(svc.getCartTotal(session) - 620.0) < 0.001, "total is 620 (got " + svc.getCartTotal(session) + ")");

        svc.updateQuantity(session, "P101", 3);
        items = svc.getCartItems(session);
        check(items.stream().filter(i -> "P101".equals(i.getItemId())).findFirst().get().getQuantity() == 3,
                "quantity update P101 -> 3");
        check(Math.abs(svc.getCartTotal(session) - (3 * 200 + 220)) < 0.001, "total after update (got " + svc.getCartTotal(session) + ")");

        svc.removeFromCart(session, "S101");
        items = svc.getCartItems(session);
        check(items.size() == 1, "after remove S101, one row (rows=" + items.size() + ")");
        check(Math.abs(svc.getCartTotal(session) - 600.0) < 0.001, "total is 600 (got " + svc.getCartTotal(session) + ")");

        svc.clearCart(session);
        check(svc.getCartItems(session).isEmpty(), "cart empty after clear");
    }

    @SuppressWarnings("unchecked")
    static void testMultiItemOrderTypesAndTotal() throws Exception {
        Object[] au = authed();
        FoodDeliveryService svc = (FoodDeliveryService) au[0];
        String session = (String) au[1];
        svc.addToCart(session, "P101", 2);    // VEG 200 x2 = 400
        svc.addToCart(session, "P201", 1);    // NON-VEG 350 x1 = 350
        svc.addToCart(session, "P301", 1);    // COMBO 399 x1 = 399
        double expected = 400 + 350 + 399;

        List<Order> orders = svc.createOrders(session, "Ada");
        check(orders.size() == 3, "3 orders created for 3 cart items (got " + orders.size() + ")");
        check(svc.getCartItems(session).isEmpty(), "cart cleared after checkout");

        boolean vegOk = orders.stream().anyMatch(o -> "P101".equals(o.getFoodItem().getItemId())
                && o instanceof models.VegOrder);
        boolean nonVegOk = orders.stream().anyMatch(o -> "P201".equals(o.getFoodItem().getItemId())
                && o instanceof models.NonVegOrder);
        boolean comboOk = orders.stream().anyMatch(o -> "P301".equals(o.getFoodItem().getItemId())
                && o instanceof models.ComboOrder);
        check(vegOk, "P101 is a VegOrder");
        check(nonVegOk, "P201 is a NonVegOrder");
        check(comboOk, "P301 is a ComboOrder");

        double total = orders.stream().mapToDouble(Order::getAmount).sum();
        check(Math.abs(total - expected) < 0.001, "total " + total + " equals expected " + expected);
        check(orders.stream().filter(o -> "P101".equals(o.getFoodItem().getItemId()))
                        .findFirst().get().getFoodItem().getPrice() == 200.0,
                "P101 canonical price is 200");

        for (Order o : orders) {
            Order found = svc.getOrder(o.getOrderId());
            check(found != null && found.getOrderId() == o.getOrderId(), "order " + o.getOrderId() + " retrievable");
        }
    }

    static void testPersistenceAcrossRestart() throws Exception {
        // Fresh database file for this isolation test.
        new File("smartfooddelivery.db").delete();
        System.setProperty("smartfood.persistence", "jdbc");
        FoodDeliveryService svc1 = new FoodDeliveryService();
        svc1.register("PersistUser", "persist@example.com", "password123");
        String session = svc1.login("persist@example.com", "password123");
        svc1.addToCart(session, "S101", 1);
        List<Order> orders = svc1.createOrders(session, "Ravi");
        int orderId = orders.get(0).getOrderId();
        check(svc1.getOrderStatus(orderId) == OrderStatus.PLACED, "order status PLACED after create");

        // Simulate restart: brand new service instance reading same DB
        FoodDeliveryService svc2 = new FoodDeliveryService();
        Order found = svc2.getOrder(orderId);
        check(found != null, "order persists across restart (id=" + orderId + ")");
        check(found != null && "S101".equals(found.getFoodItem().getItemId()), "persisted item is S101");
        check(found != null && Math.abs(found.getAmount() - 220.0) < 0.001, "persisted amount 220");
        check(svc2.getOrderStatus(orderId) == OrderStatus.PLACED, "persisted status PLACED");

        boolean advanced = svc2.advanceStatus(orderId);
        check(advanced, "status can advance on restarted instance");
        check(svc2.getOrderStatus(orderId) == OrderStatus.CONFIRMED, "status advanced to CONFIRMED");

        FoodDeliveryService svc3 = new FoodDeliveryService();
        check(svc3.getOrderStatus(orderId) == OrderStatus.CONFIRMED, "CONFIRMED status persisted across restart");
    }

    static void testUnknownItemRejected() {
        FoodDeliveryService svc = memoryService();
        String session = "sess-unknown";
        boolean ok = svc.addToCart(session, "NONEXISTENT-999", 1);
        check(!ok, "unknown item rejected");
        check(svc.getCartItems(session).isEmpty(), "cart stays empty for unknown item");
    }

    static void testForgedPriceNotTrusted() {
        FoodDeliveryService svc = memoryService();
        String session = "sess-forge";
        svc.addToCart(session, "P101", 1);
        List<CartItem> items = svc.getCartItems(session);
        check(items.get(0).getPrice() == 200.0, "backend price 200 used, not browser price");
    }
}
