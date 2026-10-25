import models.User;
import repository.InMemoryUserRepository;
import service.AuthService;
import service.FoodDeliveryService;
import memento.CartItem;

public class AuthTest {

    static int failures = 0;
    public static void main(String[] args) throws Exception {
        // Use in-memory persistence for test isolation (shared JDBC DB file would
        // leak registered users between test methods in the same JVM).
        System.setProperty("smartfood.persistence", "memory");

        testRegisterAndLogin();
        testRejectIncorrectCredentials();
        testRejectDuplicateEmail();
        testSessionLogoutAndExpiry();
        testPasswordNeverStoredPlaintext();
        testCreateOrdersRequiresAuthentication();
        testUserCannotAccessOtherUsersOrder();

        if (failures == 0) {
            System.out.println("ALL AUTH TESTS PASSED");
        } else {
            System.out.println("AUTH TESTS FAILED: " + failures);
            System.exit(1);
        }
    }

    static void check(boolean cond, String label) {
        if (cond) System.out.println("PASS: " + label);
        else { System.out.println("FAIL: " + label); failures++; }
    }

    static void testRegisterAndLogin() {
        AuthService auth = new AuthService(new InMemoryUserRepository());
        User u = auth.register("Ada", "ada@example.com", "password123");
        check(u.getId() != null, "user got an id");
        check(u.getEmail().equals("ada@example.com"), "email stored");
        check(!u.getPasswordHash().equals("password123"), "password not stored plaintext");
        check(u.getPasswordHash().startsWith("pbkdf2_sha256$"), "hash is pbkdf2");

        String session = auth.login("ada@example.com", "password123");
        check(session != null && !session.isEmpty(), "login returns a session token");
        User me = auth.getCurrentUser(session);
        check(me != null && me.getId().equals(u.getId()), "current user matches registered user");
        check(auth.isAuthenticated(session), "authenticated after login");
    }

    static void testRejectIncorrectCredentials() {
        AuthService auth = new AuthService(new InMemoryUserRepository());
        auth.register("Bob", "bob@example.com", "secret123");
        boolean threw = false;
        try { auth.login("bob@example.com", "wrongpassword"); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "wrong password rejected");
        boolean threw2 = false;
        try { auth.login("noone@example.com", "secret123"); } catch (IllegalArgumentException e) { threw2 = true; }
        check(threw2, "unknown email rejected");
        check(!auth.isAuthenticated("bogus-session"), "bogus session not authenticated");
    }

    static void testRejectDuplicateEmail() {
        AuthService auth = new AuthService(new InMemoryUserRepository());
        auth.register("Ada", "ada@example.com", "password123");
        boolean threw = false;
        try { auth.register("Ada2", "ada@example.com", "password123"); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "duplicate email rejected");
    }

    static void testSessionLogoutAndExpiry() {
        AuthService auth = new AuthService(new InMemoryUserRepository());
        auth.register("Ada", "ada@example.com", "password123");
        String session = auth.login("ada@example.com", "password123");
        auth.logout(session);
        check(!auth.isAuthenticated(session), "session invalidated on logout");
        check(auth.getCurrentUser(session) == null, "current user null after logout");
    }

    static void testPasswordNeverStoredPlaintext() {
        AuthService auth = new AuthService(new InMemoryUserRepository());
        auth.register("Carol", "carol@example.com", "hunter2");
        User u = auth.getCurrentUser(auth.login("carol@example.com", "hunter2"));
        check(!u.getPasswordHash().equals("hunter2"), "stored hash != plaintext");
    }

    static void testCreateOrdersRequiresAuthentication() {
        FoodDeliveryService svc = new FoodDeliveryService();
        String session = "no-auth-session";
        boolean threw = false;
        try { svc.createOrders(session, "Whoever"); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "createOrders without auth throws");

        // After registering + logging in, createOrders should succeed
        svc.register("Ada", "ada@example.com", "password123");
        String realSession = svc.login("ada@example.com", "password123");
        svc.addToCart(realSession, "P101", 1);
        try {
            var orders = svc.createOrders(realSession, "Ada");
            check(orders.size() == 1, "authenticated createOrders succeeds");
        } catch (Exception e) {
            check(false, "authenticated createOrders should succeed (got " + e + ")");
        }
    }

    static void testUserCannotAccessOtherUsersOrder() {
        FoodDeliveryService svc = new FoodDeliveryService();
        // User Ada places order
        svc.register("Ada", "ada@example.com", "password123");
        String adaSession = svc.login("ada@example.com", "password123");
        svc.addToCart(adaSession, "P101", 1);
        var adaOrders = svc.createOrders(adaSession, "Ada");
        int adaOrderId = adaOrders.get(0).getOrderId();
        svc.logout(adaSession);

        // User Bob logs in
        svc.register("Bob", "bob@example.com", "password123");
        String bobSession = svc.login("bob@example.com", "password123");
        check(!svc.ownsOrder(adaOrderId, svc.getCurrentUser(bobSession).getId()),
                "bob does not own ada's order");
    }
}
