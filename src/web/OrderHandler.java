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
 *   GET    /api/orders/{id}                          -> order details
 *   GET    /api/orders/{id}/status                   -> order status (Observer)
 *   POST   /api/orders                               -> create order (Factory Method + Chain + Singleton + Observer)
 *   POST   /api/orders/{id}/pay                      -> pay (Strategy + Proxy)
 *   POST   /api/orders/{id}/advance-status           -> advance status (demo)
 *   POST   /api/orders/{id}/reorder                  -> reorder via Prototype copy
 *   GET    /api/payment/methods                      -> supported payment methods
 *
 * Order creation flows through Factory Method -> Chain of Responsibility ->
 * Singleton -> Observer. Payment flows through Strategy + Proxy. Reorder flows
 * through Prototype. Cart flows through Memento.
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

        // ---- Order detail / status (reads) ----
        if ("GET".equals(method) && segs.size() == 2 && "orders".equals(segs.get(0))) {
            int id = parseInt(segs.get(1));
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
            Map<String, String> form = parseForm(readBody(exchange));
            String sessionId = form.get("session");
            String orderType = form.get("orderType");
            String customerName = form.get("customerName");
            try {
                List<Order> orders = service.createOrders(sessionId, orderType, customerName);
                if (orders == null || orders.isEmpty()) {
                    sendError(exchange, 400, "No orders were created.");
                    return;
                }
                Order first = orders.get(0);
                List<Map<String, Object>> orderList = new ArrayList<>();
                for (Order o : orders) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("orderId", o.getOrderId());
                    m.put("customerName", o.getCustomerName());
                    m.put("restaurantName", o.getRestaurantName());
                    m.put("orderType", o.getOrderType());
                    m.put("itemName", o.getFoodItem().getName());
                    m.put("quantity", o.getQuantity());
                    m.put("amount", o.getAmount());
                    orderList.add(m);
                }
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("orders", orderList);
                data.put("orderId", first.getOrderId()); // primary order for confirmation page
                data.put("customerName", first.getCustomerName());
                data.put("restaurantName", first.getRestaurantName());
                data.put("orderType", first.getOrderType());
                data.put("itemName", first.getFoodItem().getName());
                data.put("quantity", first.getQuantity());
                data.put("amount", first.getAmount());
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

            if ("pay".equals(action)) {
                Map<String, String> form = parseForm(readBody(exchange));
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

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}