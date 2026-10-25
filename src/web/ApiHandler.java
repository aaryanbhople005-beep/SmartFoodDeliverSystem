package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base API handler.
 *
 * Provides JSON response helpers and a tiny form-body parser. Concrete handlers
 * extend this and implement handleExchange().
 */
public abstract class ApiHandler implements HttpHandler {

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        String json = JsonUtil.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendOk(HttpExchange exchange, Object body) throws IOException {
        sendJson(exchange, 200, body);
    }

    protected void sendCreated(HttpExchange exchange, Object body) throws IOException {
        sendJson(exchange, 201, body);
    }

    protected void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", message);
        sendJson(exchange, status, body);
    }

    protected String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        byte[] buf = new byte[Math.max(is.available(), 8192)];
        int read = is.read(buf);
        return new String(buf, 0, read, StandardCharsets.UTF_8);
    }

    protected Map<String, String> parseForm(String body) {
        Map<String, String> params = new LinkedHashMap<>();
        if (body == null || body.isBlank()) {
            return params;
        }
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(decode(kv[0]), decode(kv[1]));
            } else if (kv.length == 1) {
                params.put(decode(kv[0]), "");
            }
        }
        return params;
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    protected abstract void handleExchange(HttpExchange exchange) throws IOException;

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        handleExchange(exchange);
    }

    protected String firstSegment(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                return parts[i];
            }
        }
        return "";
    }

    protected List<String> pathSegments(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        java.util.List<String> segs = new java.util.ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) segs.add(p);
        }
        return segs;
    }

    /**
     * Returns path segments after the "/api" prefix, e.g. "/api/cart/add" -> ["cart","add"].
     */
    protected List<String> apiSegments(HttpExchange exchange) {
        List<String> all = pathSegments(exchange);
        if (!all.isEmpty() && "api".equals(all.get(0))) {
            return all.subList(1, all.size());
        }
        return all;
    }
}