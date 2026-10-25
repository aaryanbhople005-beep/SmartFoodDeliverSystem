package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import service.FoodDeliveryService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;

/**
 * Browser entry point for the SmartFoodDeliverySystem web application.
 *
 * Starts a lightweight JDK HTTP server (com.sun.net.httpserver, no external
 * dependencies) that:
 *   1. Serves static frontend files from the frontend/ directory.
 *   2. Exposes a JSON REST API under /api.
 *
 * Usage:
 *   javac -d out -sourcepath src src/web/HttpServerApp.java <all other sources>
 *   java -cp out web.HttpServerApp
 *
 * Then open http://localhost:8080 in a browser.
 *
 * Note: src/Main.java remains the original console entry point and is left
 * unchanged. HttpServerApp.java is the separate browser entry point.
 */
public class HttpServerApp {

    private static final int PORT = 8080;
    private static final File FRONTEND_ROOT = new File("frontend");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Shared application service (single instance so cart/order state is shared
        // across all API handlers).
        FoodDeliveryService appService = new FoodDeliveryService();

        // API routes
        server.createContext("/api/restaurants", new RestaurantHandler(appService));
        server.createContext("/api/cart", new CartHandler(appService));
        server.createContext("/api/orders", new OrderHandler(appService));
        server.createContext("/api/payment", new OrderHandler(appService)); // payment methods

        // OPTIONS preflight for CORS
        server.createContext("/api", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods",
                        "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                        "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.getResponseBody().close();
            }
        });

        // Static frontend files
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";
            serveStatic(exchange, path);
        });

        server.start();
        System.out.println("SmartFoodDeliverySystem server started on http://localhost:" + PORT);
        System.out.println("Frontend: http://localhost:" + PORT + "/index.html");
        System.out.println("API base: http://localhost:" + PORT + "/api");
        System.out.println("Press Ctrl+C to stop.");
    }

    private static void serveStatic(HttpExchange exchange, String path) throws IOException {
        File file = new File(FRONTEND_ROOT, path);
        if (!file.exists() || file.isDirectory()) {
            file = new File(FRONTEND_ROOT, "index.html");
        }
        String contentType = contentTypeFor(file);
        byte[] data = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private static String contentTypeFor(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}