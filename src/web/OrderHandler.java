package web;

import models.FoodItem;
import models.Order;
import observer.OrderStatus;
import service.FoodDeliveryService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * REST handler for order and payment operations.
 *
 * Routes:
 *   GET    /api/orders                              -> current user's orders (auth)
 *   GET    /api/orders/{id}                         -> order details (auth + ownership)
 *   GET    /api/orders/{id}/status                  -> order status (auth + ownership)
 *   POST   /api/orders                              -> create order (Factory Method + Chain + Singleton + Observer; auth)
 *   POST   /api/orders/{id}/pay                     -> pay (Strategy + Proxy; auth + ownership)
 *   POST   /api/orders/{id}/advance-status          -> advance status (demo; auth + ownership)
 *   POST   /api/orders/{id}/reorder                 -> reorder via Prototype copy (auth + ownership)
 *   GET    /api/payment/methods                     -> supported payment methods
 *
 * Order creation flows through Factory Method -> Chain of Responsibility ->
 * Singleton -> Observer. Payment flows through Strategy + Proxy. Reorder flows
 * through Prototype. Cart flows through Memento.
 *
 * Every order is associated with the authenticated user who placed it, and a
 * customer can only read/pay/reorder their own orders.
 */
public class OrderHandler extends ApiHandler {

    private final FoodDeliveryService service;

    public OrderHandler(FoodDeliveryService service) {
        this.service = service;
    }

    @Override
    protected void handleExchange(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        List<String> segs = apiSegments(exchange);

        // Parse a POST body once so both the session token and action fields are
        // available without consuming the input stream twice.
        Map<String, String> form = new LinkedHashMap<>();
        if ("POST".equals(method)) {
            form = parseForm(readBody(exchange));
        }

        // ---- List current user's orders ----
        if ("GET".equals(method) && segs.size() == 1 && "orders".equals(segs.get(0))) {
            String sessionId = param(exchange.getRequestURI().getQuery(), "session");
            Integer userId = requireAuthenticated(exchange, sessionId);
            if (userId == null) return;
            List<Order> orders = service.getOrdersForUser(userId);
            List<Map<String, Object>> orderList = new ArrayList<>();
            for (Order o : orders) {
                orderList.add(orderSummary(o));
            }
            sendOk(exchange, Map.of("success", true, "data", orderList,
                    "message", orderList.size() + " order(s)."));
            return;
        }

        // ---- Order detail / status (reads) ----
        if ("GET".equals(method) && segs.size() == 2 && "orders".equals(segs.get(0))) {
            int id = parseInt(segs.get(1));
            Integer me = requireOwner(exchange, id, form);
            if (me == null) return;
            Order order = service.getOrder(id);
            if (order == null) {
                sendError(exchange, 404, "Order not found.");
                return;
            }
            FoodItem item = order.getFoodItem();
            Map<String, Object> food = new LinkedHashMap<>();
            food.put("itemId", item.getItemId());
            food.put("name", item.getName());
            food.put("price", item.getPrice());
            food.put("category", item.getCategory());
            food.put("restaurant", item.getRestaurant());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", order.getOrderId());
            m.put("customerName", order.getCustomerName());
            m.put("restaurantName", order.getRestaurantName());
            m.put("orderType", order.getOrderType());
            m.put("itemName", item.getName());
            m.put("itemPrice", item.getPrice());
            m.put("quantity", order.getQuantity());
            m.put("amount", order.getAmount());
            m.put("foodItem", food);
            sendOk(exchange, Map.of("success", true, "data", m));
            return;
        }

        if ("GET".equals(method) && segs.size() == 3 && "orders".equals(segs.get(0))
                && "status".equals(segs.get(2))) {
            int id = parseInt(segs.get(1));
            Integer me = requireOwner(exchange, id, form);
            if (me == null) return;
            OrderStatus status = service.getOrderStatus(id);
            if (status == null) {
                sendError(exchange, 404, "Order not found.");
                return;
            }
            sendOk(exchange, Map.of("success", true, "data", Map.of(
                    "orderId", id, "status", status.name())));
            return;
        }

        // ---- Create order ----
        if ("POST".equals(method) && segs.size() == 1 && "orders".equals(segs.get(0))) {
            String sessionId = form.get("session");
            String customerName = form.get("customerName");
            try {
                // The backend derives each item's order type from its actual
                // product category; the browser does not send a single orderType.
                List<Order> orders = service.createOrders(sessionId, customerName);
                if (orders == null || orders.isEmpty()) {
                    sendError(exchange, 400, "No orders were created.");
                    return;
                }
                Order first = orders.get(0);
                List<Map<String, Object>> orderList = new ArrayList<>();
                for (Order o : orders) {
                    orderList.add(orderSummary(o));
                }
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("orders", orderList);
                data.put("orderId", first.getOrderId());
                data.put("customerName", first.getCustomerName());
                data.put("restaurantName", first.getRestaurantName());
                data.put("orderType", first.getOrderType());
                data.put("itemName", first.getFoodItem().getName());
                data.put("quantity", first.getQuantity());
                data.put("amount", first.getAmount());
                data.put("total", orders.stream().mapToDouble(Order::getAmount).sum());
                sendCreated(exchange, Map.of("success", true, "data", data,
                        "message", orders.size() + " order(s) created and validated."));
                return;
            } catch (IllegalArgumentException | IllegalStateException e) {
                sendError(exchange, 400, e.getMessage());
                return;
            }
        }

        // ---- Order actions: /api/orders/{id}/pay | /advance-status | /reorder ----
        if ("POST".equals(method) && segs.size() == 3 && "orders".equals(segs.get(0))) {
            int id = parseInt(segs.get(1));
            String action = segs.get(2);

            Integer me = requireOwner(exchange, id, form);
            if (me == null) return;

            if ("pay".equals(action)) {
                String payMethod = form.get("method");
                FoodDeliveryService.PaymentResult result = service.pay(id, payMethod);
                if (!result.success) {
                    sendError(exchange, 400, result.message);
                    return;
                }
                sendOk(exchange, Map.of("success", true, "message", result.message,
                        "method", result.method));
                return;
            }

            if ("advance-status".equals(action)) {
                boolean ok = service.advanceStatus(id);
                if (!ok) {
                    sendError(exchange, 400, "Order not found or cannot advance.");
                    return;
                }
                sendOk(exchange, Map.of("success", true, "message", "Status advanced.",
                        "status", service.getOrderStatus(id).name()));
                return;
            }

            if ("reorder".equals(action)) {
                Order copy = service.reorder(id);
                if (copy == null) {
                    sendError(exchange, 404, "Order not found.");
                    return;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orderId", copy.getOrderId());
                m.put("customerName", copy.getCustomerName());
                m.put("restaurantName", copy.getRestaurantName());
                m.put("orderType", copy.getOrderType());
                m.put("itemName", copy.getFoodItem().getName());
                m.put("quantity", copy.getQuantity());
                m.put("amount", copy.getAmount());
                sendOk(exchange, Map.of("success", true, "data", m,
                        "message", "Order copied via Prototype. Submit as a new order."));
                return;
            }

            sendError(exchange, 404, "Unknown order action.");
            return;
        }

        // ---- Payment methods ----
        if ("GET".equals(method) && segs.size() == 2 && "payment".equals(segs.get(0))
                && "methods".equals(segs.get(1))) {
            sendOk(exchange, Map.of("success", true, "data", service.getPaymentMethods()));
            return;
        }

        sendError(exchange, 404, "Not found.");
    }

    private Map<String, Object> orderSummary(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("customerName", o.getCustomerName());
        m.put("restaurantName", o.getRestaurantName());
        m.put("orderType", o.getOrderType());
        m.put("itemId", o.getFoodItem().getItemId());
        m.put("itemName", o.getFoodItem().getName());
        m.put("itemPrice", o.getFoodItem().getPrice());
        m.put("quantity", o.getQuantity());
        m.put("amount", o.getAmount());
        return m;
    }

    /**
     * Requires an authenticated session. Returns the user id or null (error already sent).
     */
    private Integer requireAuthenticated(HttpExchange exchange, String sessionId) throws IOException {
        if (!service.isAuthenticated(sessionId)) {
            sendError(exchange, 401, "Authentication required. Sign in first.");
            return null;
        }
        Integer userId = service.getUserId(sessionId);
        if (userId == null) {
            sendError(exchange, 401, "Authentication required. Sign in first.");
            return null;
        }
        return userId;
    }

    /**
     * Requires an authenticated session that owns the given order. Returns the
     * user id or null (error already sent).
     */
    private Integer requireOwner(HttpExchange exchange, int orderId, Map<String, String> form)
            throws IOException {
        String sessionId = sessionIdFrom(exchange.getRequestURI().getQuery(), form);
        Integer userId = requireAuthenticated(exchange, sessionId);
        if (userId == null) return null;
        if (!service.ownsOrder(orderId, userId)) {
            sendError(exchange, 403, "You cannot access another customer's order.");
            return null;
        }
        return userId;
    }

    /**
     * Resolves the session token from the ?session= query parameter or, for
     * POST requests, the already-parsed form body.
     */
    private String sessionIdFrom(String query, Map<String, String> form) {
        String sid = param(query, "session");
        if ((sid == null || sid.isBlank()) && form.containsKey("session")) {
            sid = form.get("session");
        }
        return sid;
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
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
}
