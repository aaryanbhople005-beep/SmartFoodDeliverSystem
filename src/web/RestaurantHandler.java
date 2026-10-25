package web;

import com.sun.net.httpserver.HttpExchange;
import models.FoodItem;
import models.Restaurant;
import service.FoodDeliveryService;

import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST handler for restaurant and menu operations.
 *
 * Routes:
 *   GET  /api/restaurants                       -> list of restaurants
 *   GET  /api/restaurants/{id}                  -> restaurant details
 *   GET  /api/restaurants/{id}/menu?category=   -> food items for a category
 *
 * Restaurant/menu data comes from the Abstract Factory via the repository -
 * nothing is hard-coded in this handler.
 */
public class RestaurantHandler extends ApiHandler {

    private final FoodDeliveryService service;

    public RestaurantHandler(FoodDeliveryService service) {
        this.service = service;
    }

    @Override
    protected void handleExchange(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        List<String> segs = apiSegments(exchange);

        // /api/restaurants
        if (segs.size() == 1 && "restaurants".equals(segs.get(0))) {
            if ("GET".equals(method)) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (Restaurant r : service.getRestaurants()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getName());
                    m.put("description", r.getDescription());
                    m.put("category", r.getCategory());
                    list.add(m);
                }
                sendOk(exchange, Map.of("success", true, "data", list));
                return;
            }
        }

        // /api/restaurants/{id}
        if (segs.size() == 2 && "restaurants".equals(segs.get(0))) {
            String id = segs.get(1);
            Restaurant r = service.getRestaurant(id);
            if (r == null) {
                sendError(exchange, 404, "Restaurant not found.");
                return;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("description", r.getDescription());
            m.put("category", r.getCategory());
            sendOk(exchange, Map.of("success", true, "data", m));
            return;
        }

        // /api/restaurants/{id}/menu?category=VEG
        if (segs.size() == 3 && "restaurants".equals(segs.get(0)) && "menu".equals(segs.get(2))) {
            String id = segs.get(1);
            String category = exchange.getRequestURI().getQuery() != null
                    ? param(exchange.getRequestURI().getQuery(), "category")
                    : null;
            if (category == null) category = "VEG";
            FoodItem[] items = service.getMenu(id, category);
            List<Map<String, Object>> list = new ArrayList<>();
            for (FoodItem f : items) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("itemId", f.getItemId());
                m.put("name", f.getName());
                m.put("price", f.getPrice());
                m.put("category", f.getCategory());
                m.put("restaurant", f.getRestaurant());
                list.add(m);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("restaurantId", id);
            data.put("category", category);
            data.put("items", list);
            sendOk(exchange, Map.of("success", true, "data", data));
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
}