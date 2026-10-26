package web;

import com.sun.net.httpserver.HttpExchange;
import models.User;
import service.FoodDeliveryService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication API handler.
 *
 * Routes:
 *   POST /api/auth/register   form: name, email, password  -> registers a user
 *   POST /api/auth/login      form: email, password        -> issues a session token
 *   POST /api/auth/logout     form: session                -> invalidates the session
 *   GET  /api/auth/me         query: session               -> returns the current user
 *
 * Passwords are hashed server-side (PBKDF2) and never returned to the browser.
 * A session token is only issued after successful credential validation.
 */
public class AuthHandler extends ApiHandler {

    private final FoodDeliveryService service;

    public AuthHandler(FoodDeliveryService service) {
        this.service = service;
    }

    @Override
    protected void handleExchange(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        List<String> segs = apiSegments(exchange);
        // apiSegments strips "/api", so /api/auth/register -> ["auth","register"].
        if (segs.size() < 2 || !"auth".equals(segs.get(0))) {
            sendError(exchange, 404, "Not found.");
            return;
        }
        String action = segs.get(1);

        // Parse a POST body once so the session token and action fields are both
        // available without consuming the input stream more than once.
        Map<String, String> form = new LinkedHashMap<>();
        if ("POST".equals(method)) {
            form = parseForm(readBody(exchange));
        }

        // Session token from ?session= query or, for POST, the form body.
        String sessionId = param(exchange.getRequestURI().getQuery(), "session");
        if ((sessionId == null || sessionId.isBlank()) && form.containsKey("session")) {
            sessionId = form.get("session");
        }

        switch (action) {
            case "register":
                if (!"POST".equals(method)) { sendError(exchange, 405, "Method not allowed."); return; }
                handleRegister(exchange, form);
                return;
            case "login":
                if (!"POST".equals(method)) { sendError(exchange, 405, "Method not allowed."); return; }
                handleLogin(exchange, form);
                return;
            case "logout":
                if (!"POST".equals(method)) { sendError(exchange, 405, "Method not allowed."); return; }
                handleLogout(exchange, sessionId);
                return;
            case "me":
                if (!"GET".equals(method)) { sendError(exchange, 405, "Method not allowed."); return; }
                handleMe(exchange, sessionId);
                return;
            default:
                sendError(exchange, 404, "Not found.");
                return;
        }
    }

    private void handleRegister(HttpExchange exchange, Map<String, String> form) throws IOException {
        String name = form.get("name");
        String email = form.get("email");
        String password = form.get("password");
        try {
            User user = service.register(name, email, password);
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id", user.getId());
            u.put("name", user.getName());
            u.put("email", user.getEmail());
            sendCreated(exchange, Map.of("success", true, "data", u,
                    "message", "Account created."));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange, Map<String, String> form) throws IOException {
        String email = form.get("email");
        String password = form.get("password");
        String oldSessionId = form.get("session"); // Old session from browser
        try {
            String newSessionId = service.login(email, password);
            
            // Transfer cart from old session to new session if old session exists
            if (oldSessionId != null && !oldSessionId.isBlank() && !oldSessionId.equals(newSessionId)) {
                service.transferCart(oldSessionId, newSessionId);
            }
            
            // Also load user's persistent cart from database
            Integer userId = service.getUserId(newSessionId);
            if (userId != null) {
                service.loadUserCartForSession(userId, newSessionId);
            }
            
            // Associate the new session with the user for future persistence
            if (userId != null) {
                service.associateSessionWithUser(newSessionId, userId);
            }
            
            User user = service.getCurrentUser(newSessionId);
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id", user.getId());
            u.put("name", user.getName());
            u.put("email", user.getEmail());
            sendOk(exchange, Map.of("success", true, "data", Map.of(
                    "session", newSessionId,
                    "user", u
            ), "message", "Signed in."));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 401, e.getMessage());
        }
    }

    private void handleLogout(HttpExchange exchange, String sessionId) throws IOException {
        service.logout(sessionId);
        sendOk(exchange, Map.of("success", true, "message", "Signed out."));
    }

    private void handleMe(HttpExchange exchange, String sessionId) throws IOException {
        User user = service.getCurrentUser(sessionId);
        if (user == null) {
            sendError(exchange, 401, "Not authenticated.");
            return;
        }
        Map<String, Object> u = new LinkedHashMap<>();
        u.put("id", user.getId());
        u.put("name", user.getName());
        u.put("email", user.getEmail());
        sendOk(exchange, Map.of("success", true, "data", u));
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
