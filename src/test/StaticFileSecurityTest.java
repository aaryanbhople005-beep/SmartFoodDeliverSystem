import com.sun.net.httpserver.HttpServer;
import service.FoodDeliveryService;
import web.HttpServerApp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Tests static file serving security: path traversal must be denied and
 * missing resources must return 404 (not index.html).
 */
public class StaticFileSecurityTest {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServerApp.createServer(0, new FoodDeliveryService());
        server.start();
        int port = server.getAddress().getPort();
        String base = "http://localhost:" + port;
        int failures = 0;

        try {
            // Valid frontend routes must work.
            failures += check("GET / -> 200", base + "/", 200);
            failures += check("GET /index.html -> 200", base + "/index.html", 200);
            failures += check("GET /css/style.css -> 200", base + "/css/style.css", 200);
            failures += check("GET /js/app.js -> 200", base + "/js/app.js", 200);

            // Path traversal attempts must be denied (404).
            failures += check("GET /../../../README.md -> 404", base + "/../../../README.md", 404);
            failures += check("GET /../src/Main.java -> 404", base + "/../src/Main.java", 404);
            failures += check("GET /..%2f..%2fREADME.md -> 404", base + "/..%2f..%2fREADME.md", 404);
            failures += check("GET /frontend/../README.md -> 404", base + "/frontend/../README.md", 404);

            // Missing resources return 404 (not index.html).
            failures += check("GET /nonexistent-xyz.html -> 404", base + "/nonexistent-xyz.html", 404);
            failures += check("GET /api/nope -> 404", base + "/api/nope", 404);

            // Project files outside frontend must not be served.
            failures += check("GET /README.md -> 404", base + "/README.md", 404);
            failures += check("GET /pom.xml -> 404", base + "/pom.xml", 404);

            if (failures == 0) {
                System.out.println("ALL STATIC FILE SECURITY TESTS PASSED");
            } else {
                System.out.println("STATIC FILE SECURITY TESTS FAILED: " + failures);
                System.exit(1);
            }
        } finally {
            server.stop(0);
        }
    }

    private static int check(String label, String urlSpec, int expectedStatus) {
        try {
            URL url = new URL(urlSpec);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(false);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            int status = con.getResponseCode();
            InputStream is = status >= 400 ? con.getErrorStream() : con.getInputStream();
            if (is != null) is.readAllBytes();
            is = null;
            con.disconnect();
            if (status == expectedStatus) {
                System.out.println("PASS: " + label + " (" + status + ")");
                return 0;
            }
            System.out.println("FAIL: " + label + " expected " + expectedStatus + " got " + status);
            return 1;
        } catch (Exception e) {
            System.out.println("FAIL: " + label + " threw " + e);
            return 1;
        }
    }
}
