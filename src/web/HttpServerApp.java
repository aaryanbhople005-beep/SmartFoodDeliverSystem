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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Browser entry point for the SmartFoodDeliverySystem web application.
 *
 * Starts a lightweight JDK HTTP server (com.sun.net.httpserver, no external
 * dependencies) that:
 *   1. Serves static frontend files from the frontend/ directory.
 *   2. Exposes a JSON REST API under /api.
 *
 * Static file security:
 *   - The requested path is normalized and resolved against the canonical
 *     frontend root.
 *   - The resolved file's canonical path must be inside the frontend root;
 *     path traversal (..) and absolute-path escapes are rejected with 404.
 *   - Only safe frontend extensions are served; Java source, database files,
 *     configuration and other project files can never be served through the
 *     static server.
 *   - Missing resources return 404 instead of silently falling back to
 *     index.html.
 *
 * Usage:
 *   javac -d out -cp lib/sqlite-jdbc-3.46.1.3.jar -sourcepath src $(Get-ChildItem -Recurse src -Filter *.java | Select-Object -ExpandProperty FullName)
 *   java -cp out;lib/sqlite-jdbc-3.46.1.3.jar web.HttpServerApp
 *
 * Then open http://localhost:8080 in a browser.
 *
 * The port is configurable via the SMARTFOOD_PORT environment variable or the
 * smartfood.port system property (default 8080).
 *
 * Note: src/Main.java remains the original console entry point and is left
 * unchanged. HttpServerApp.java is the separate browser entry point.
 */
public class HttpServerApp {

    private static final int DEFAULT_PORT = 8080;

    /** Canonical frontend root; all static responses must resolve inside it. */
    private static final File FRONTEND_ROOT;

    /** Safe, frontend-only file extensions served by the static handler. */
    private static final String[] SAFE_EXTENSIONS = {
            ".html", ".css", ".js", ".json", ".png", ".jpg", ".jpeg",
            ".svg", ".ico", ".woff", ".woff2", ".ttf", ".otf"
    };

    static {
        File root = new File("frontend");
        try {
            root = root.getCanonicalFile();
        } catch (IOException ignored) {
            // Fall back to the absolute path if canonicalization fails.
        }
        FRONTEND_ROOT = root;
    }

    public static void main(String[] args) throws IOException {
        int port = parsePort();
        FoodDeliveryService appService = new FoodDeliveryService();
        HttpServer server = createServer(port, appService);
        server.start();
        System.out.println("SmartFoodDeliverySystem server started on http://localhost:" + port);
        System.out.println("Frontend: http://localhost:" + port + "/index.html");
        System.out.println("API base: http://localhost:" + port + "/api");
        System.out.println("Press Ctrl+C to stop.");
    }

    /**
     * Builds (but does not start) the HTTP server on the given port. Exposed
     * publicly so tests can bind an ephemeral port and exercise the handlers.
     */
    public static HttpServer createServer(int port, FoodDeliveryService service) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // API routes
        server.createContext("/api/restaurants", new RestaurantHandler(service));
        server.createContext("/api/cart", new CartHandler(service));
        server.createContext("/api/orders", new OrderHandler(service));
        server.createContext("/api/auth", new AuthHandler(service));
        server.createContext("/api/payment", new OrderHandler(service)); // payment methods

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

        // Static frontend files (must be registered after /api contexts so the
        // most specific context wins).
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            serveStatic(exchange, path);
        });

        return server;
    }

    private static int parsePort() {
        String env = System.getenv("SMARTFOOD_PORT");
        String prop = System.getProperty("smartfood.port");
        String value = (prop != null && !prop.isBlank()) ? prop : env;
        if (value != null && !value.isBlank()) {
            try {
                int p = Integer.parseInt(value.trim());
                if (p > 0 && p < 65536) return p;
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_PORT;
    }

    private static void serveStatic(HttpExchange exchange, String path) throws IOException {
        String clean = normalizePath(path);
        if (clean == null) {
            sendText(exchange, 404, "text/plain", "Not found.");
            return;
        }

        // Resolve inside the canonical frontend root only.
        File requested = new File(FRONTEND_ROOT, clean);
        File resolved;
        try {
            resolved = requested.getCanonicalFile();
        } catch (IOException e) {
            sendText(exchange, 404, "text/plain", "Not found.");
            return;
        }

        String rootPath = FRONTEND_ROOT.getCanonicalPath();
        String resolvedPath = resolved.getCanonicalPath();
        boolean insideRoot = resolvedPath.equals(rootPath)
                || resolvedPath.startsWith(rootPath + File.separator);

        if (!insideRoot) {
            // Path traversal attempt: reject without revealing the target.
            sendText(exchange, 404, "text/plain", "Not found.");
            return;
        }

        // Defense in depth: serve only safe frontend extensions.
        String name = resolved.getName().toLowerCase();
        boolean safe = false;
        for (String ext : SAFE_EXTENSIONS) {
            if (name.endsWith(ext)) {
                safe = true;
                break;
            }
        }
        if (!safe) {
            sendText(exchange, 404, "text/plain", "Not found.");
            return;
        }

        if (!resolved.exists() || resolved.isDirectory()) {
            sendText(exchange, 404, "text/plain", "Not found.");
            return;
        }

        byte[] data = Files.readAllBytes(resolved.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentTypeFor(resolved) + "; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    /**
     * Normalizes a request path into a safe, frontend-root-relative path
     * (e.g. "css/style.css", "index.html"). Returns null for traversal or
     * otherwise unsafe paths.
     */
    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) return null;
        if (path.indexOf('\u0000') >= 0) return null;

        String trimmed = path.trim();
        if ("/".equals(trimmed) || trimmed.isEmpty()) return "index.html";

        // Strip leading slashes so the path is relative to the frontend root.
        String noLead = trimmed.replaceFirst("^/+", "");
        if (noLead.isEmpty()) return "index.html";

        String[] parts = noLead.split("/");
        List<String> stack = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                } else {
                    // Attempted to escape above the frontend root.
                    return null;
                }
                continue;
            }
            stack.add(part);
        }

        if (stack.isEmpty()) return "index.html";

        StringBuilder sb = new StringBuilder();
        for (String p : stack) {
            sb.append(p).append('/');
        }
        return sb.substring(0, sb.length() - 1);
    }

    private static void sendText(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
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
        if (name.endsWith(".ico")) return "image/x-icon";
        if (name.endsWith(".woff")) return "font/woff";
        if (name.endsWith(".woff2")) return "font/woff2";
        if (name.endsWith(".ttf")) return "font/ttf";
        if (name.endsWith(".otf")) return "font/otf";
        return "application/octet-stream";
    }
}
