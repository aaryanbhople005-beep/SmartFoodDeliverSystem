package web;

import memento.CartItem;
import repository.DatabaseCartRepository;
import service.FoodDeliveryService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * REST handler for cart operations.
 *
 * Routes (session id passed via ?session=):
 *   GET    /api/cart                            -> cart items + total
 *   POST   /api/cart/add?session=               -> add item (form: itemId,name,price,quantity)
 *   POST   /api/cart/quantity?session=          -> update quantity (form: itemId,quantity)
 *   POST   /api/cart/remove?session=            -> remove item (form: itemId)
 *   POST   /api/cart/save?session=              -> returns memento token
 *   POST   /api/cart/restore?session=           -> restore memento (form: token)
 *   GET    /api/cart/snapshots?session=         -> list saved snapshots for user
 *
 * Cart state is persisted in SQLite database. The session id routes to the correct
 * per-session cart. Database operations use DatabaseCartRepository.
 */
public class CartHandler extends ApiHandler {

    private final FoodDeliveryService service;

    public CartHandler(FoodDeliveryService service) {
        this.service = service;
    }

    @Override
    protected void handleExchange(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        List<String> segs = apiSegments(exchange);
        Map<String, String> form = new LinkedHashMap<>();
        if ("POST".equals(method)) {
            form = parseForm(readBody(exchange));
        }
        String sessionId = param(exchange.getRequestURI().getQuery(), "session");
        if ((sessionId == null || sessionId.isBlank()) && !form.isEmpty()) {
            sessionId = form.get("session");
        }
        if (sessionId == null || sessionId.isBlank()) {
            sendError(exchange, 400, "Session id is required (?session=).");
            return;
        }

        if ("GET".equals(method) && segs.size() == 1 && "cart".equals(segs.get(0))) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (CartItem it : service.getCartItems(sessionId)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("itemId", it.getItemId());
                m.put("name", it.getName());
                m.put("price", it.getPrice());
                m.put("quantity", it.getQuantity());
                m.put("subtotal", it.getSubtotal());
                items.add(m);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sessionId", sessionId);
            data.put("items", items);
            data.put("total", service.getCartTotal(sessionId));
            sendOk(exchange, Map.of("success", true, "data", data));
            return;
        }

        // GET /api/cart/snapshots?session= - list user's saved snapshots
        if ("GET".equals(method) && segs.size() == 2 && "cart".equals(segs.get(0)) && "snapshots".equals(segs.get(1))) {
            Integer userId = service.getUserId(sessionId);
            if (userId == null) {
                sendError(exchange, 401, "Authentication required.");
                return;
            }
            List<DatabaseCartRepository.CartSnapshot> snapshots = service.getCartSnapshots(userId);
            List<Map<String, Object>> snapList = new ArrayList<>();
            for (DatabaseCartRepository.CartSnapshot snap : snapshots) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", snap.id);
                m.put("token", snap.token);
                m.put("createdAt", snap.createdAt);
                snapList.add(m);
            }
            sendOk(exchange, Map.of("success", true, "data", snapList));
            return;
        }

        if ("POST".equals(method) && segs.size() >= 2 && "cart".equals(segs.get(0))) {
            String action = segs.get(1);

            if ("add".equals(action)) {
                String itemId = form.get("itemId");
                int quantity = parseInt(form.get("quantity"), 1);
                if (itemId == null || itemId.isBlank()) {
                    sendError(exchange, 400, "itemId is required.");
                    return;
                }
                if (quantity <= 0) {
                    sendError(exchange, 400, "Invalid quantity.");
                    return;
                }
                // The browser supplies only the item id and quantity. The backend
                // resolves the canonical name, price, category and restaurant.
                if (service.addToCart(sessionId, itemId, quantity)) {
                    sendOk(exchange, Map.of("success", true, "message", "Added to cart."));
                } else {
                    sendError(exchange, 400, "Failed to add item: unknown product or invalid quantity.");
                }
                return;
            }

            if ("quantity".equals(action)) {
                String itemId = form.get("itemId");
                int quantity = parseInt(form.get("quantity"), 1);
                if (service.updateQuantity(sessionId, itemId, quantity)) {
                    sendOk(exchange, Map.of("success", true, "message", "Quantity updated."));
                } else {
                    sendError(exchange, 400, "Invalid quantity or item not found.");
                }
                return;
            }

            if ("remove".equals(action)) {
                String itemId = form.get("itemId");
                service.removeFromCart(sessionId, itemId);
                sendOk(exchange, Map.of("success", true, "message", "Removed from cart."));
                return;
            }

            if ("save".equals(action)) {
                String token = service.saveCart(sessionId);
                Map<String, Object> saveData = new LinkedHashMap<>();
                saveData.put("token", token);
                sendOk(exchange, Map.of("success", true, "data", saveData,
                        "message", "Cart snapshot saved (Memento)."));
                return;
            }

            if ("restore".equals(action)) {
                String token = form.get("token");
                if (token == null || token.isBlank()) {
                    sendError(exchange, 400, "Token is required.");
                    return;
                }
                if (service.restoreCart(sessionId, token)) {
                    sendOk(exchange, Map.of("success", true, "message", "Cart restored (Memento)."));
                } else {
                    sendError(exchange, 400, "Invalid token.");
                }
                return;
            }

            sendError(exchange, 404, "Unknown cart action.");
            return;
        }

        sendError(exchange, 404, "Not found.");
    }

    private String param(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String k = decode(kv[0]);
            if (key.equals(k)) {
                return kv.length == 2 ? decode(kv[1]) : "";
            }
        }
        return null;
    }

    private String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    private int parseInt(String s, int def) {
        try { return s == null ? def : Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return s == null ? def : Double.parseDouble(s); } catch (Exception e) { return def; }
    }
}