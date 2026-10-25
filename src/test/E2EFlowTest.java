import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * End-to-end browser-flow test against the running server (localhost:8081).
 * Exercises: auth, restaurants, menu, cart dedup, multi-item order, payment,
 * order retrieval, and cross-user order protection.
 */
public class E2EFlowTest {

    static final String BASE = "http://localhost:8082/api";
    static int failures = 0;

    public static void main(String[] args) throws Exception {
        // unique emails so repeated runs don't collide
        String suffix = Long.toString(System.currentTimeMillis());
        String email = "e2e-" + suffix + "@example.com";

        // 1. Register
        Map<String,Object> reg = post("/auth/register", map("name", "E2E User", "email", email, "password", "password123"));
        check(reg != null && (boolean) reg.get("success"), "register user");
        @SuppressWarnings("unchecked")
        Map<String,Object> regUser = (Map<String,Object>) reg.get("data");
        int userId = ((Number) regUser.get("id")).intValue();

        // 2. Login -> session
        Map<String,Object> login = post("/auth/login", map("email", email, "password", "password123"));
        check(login != null && (boolean) login.get("success"), "login returns success");
        @SuppressWarnings("unchecked")
        Map<String,Object> logindata = (Map<String,Object>) login.get("data");
        String session = (String) logindata.get("session");
        check(session != null && !session.isEmpty(), "session token issued");

        // wrong password rejected
        boolean bad = false;
        try { post("/auth/login", map("email", email, "password", "wrong")); } catch (HttpStatusException e) { bad = e.status == 401; }
        check(bad, "wrong password rejected with 401");

        // 3. Restaurants
        Map<String,Object> rests = get("/restaurants?session=" + session);
        check(rests != null && (boolean) rests.get("success"), "restaurants list");
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rlist = (List<Map<String,Object>>) rests.get("data");
        check(rlist.size() == 2, "two restaurants (got " + rlist.size() + ")");

        // pick pizza hub id
        String pizzaId = null;
        for (Map<String,Object> r : rlist) {
            if ("Pizza Hub".equals(r.get("name"))) pizzaId = String.valueOf(r.get("id"));
        }
        check(pizzaId != null, "Pizza Hub id found");

        // 4. Menu VEG
        Map<String,Object> menu = get("/restaurants/" + pizzaId + "/menu?category=VEG&session=" + session);
        check(menu != null && (boolean) menu.get("success"), "menu loads");
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> items = (List<Map<String,Object>>) ((Map<String,Object>) menu.get("data")).get("items");
        check(items.size() > 0, "menu has items");

        // 5. Cart: add P101 twice -> dedup quantity 2
        String p101 = null;
        for (Map<String,Object> it : items) {
            if ("P101".equals(it.get("itemId"))) p101 = "P101";
        }
        if (p101 == null) p101 = (String) items.get(0).get("itemId");
        post("/cart/add", map("session", session, "itemId", p101, "quantity", String.valueOf(1)));
        post("/cart/add", map("session", session, "itemId", p101, "quantity", String.valueOf(1)));
        Map<String,Object> cart = get("/cart?session=" + session);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> citems = (List<Map<String,Object>>) ((Map<String,Object>) cart.get("data")).get("items");
        check(citems.size() == 1, "cart dedup: one row (rows=" + citems.size() + ")");
        check(((Number) citems.get(0).get("quantity")).intValue() == 2, "cart dedup: quantity 2");

        // 6. Add a second item for multi-item checkout
        String secondId = null;
        for (Map<String,Object> it : items) {
            if (!it.get("itemId").equals(p101)) { secondId = (String) it.get("itemId"); break; }
        }
        if (secondId == null) {
            // fetch NON-VEG menu to get a second item
            Map<String,Object> m2 = get("/restaurants/" + pizzaId + "/menu?category=NON-VEG&session=" + session);
            List<Map<String,Object>> i2 = (List<Map<String,Object>>) ((Map<String,Object>) m2.get("data")).get("items");
            secondId = (String) i2.get(0).get("itemId");
        }
        post("/cart/add", map("session", session, "itemId", secondId, "quantity", String.valueOf(1)));

        cart = get("/cart?session=" + session);
        citems = (List<Map<String,Object>>) ((Map<String,Object>) cart.get("data")).get("items");
        double cartTotal = ((Number) ((Map<String,Object>) cart.get("data")).get("total")).doubleValue();
        check(citems.size() == 2, "cart has 2 items after adding second");

        // 7. Create multi-item order
        Map<String,Object> created = post("/orders", map("session", session, "customerName", "E2E User"));
        check(created != null && (boolean) created.get("success"), "create orders success");
        @SuppressWarnings("unchecked")
        Map<String,Object> cdata = (Map<String,Object>) created.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> orders = (List<Map<String,Object>>) cdata.get("orders");
        check(orders.size() == 2, "2 orders for 2 cart items (got " + orders.size() + ")");
        double total = ((Number) cdata.get("total")).doubleValue();
        check(Math.abs(total - cartTotal) < 0.01, "checkout total " + total + " equals cart total " + cartTotal);

        // correct order types: P101 VEG, second NON-VEG
        final String p101Id = p101;
        boolean hasVeg = orders.stream().anyMatch(o -> p101Id.equals(o.get("itemId")) && "Veg".equals(o.get("orderType")));
        check(hasVeg, "P101 created as Veg order");

        int firstId = ((Number) orders.get(0).get("orderId")).intValue();

        // cart cleared after checkout
        cart = get("/cart?session=" + session);
        citems = (List<Map<String,Object>>) ((Map<String,Object>) cart.get("data")).get("items");
        check(citems.isEmpty(), "cart cleared after checkout");

        // 8. Pay for every order
        boolean allPaid = true;
        for (Map<String,Object> o : orders) {
            int oid = ((Number) o.get("orderId")).intValue();
            Map<String,Object> pay = post("/orders/" + oid + "/pay", map("session", session, "method", "UPI"));
            allPaid = allPaid && (pay != null && (boolean) pay.get("success"));
        }
        check(allPaid, "payment processed for every order");

        // 9. Retrieve order (owner)
        Map<String,Object> order = get("/orders/" + firstId + "?session=" + session);
        check(order != null && (boolean) order.get("success"), "order retrievable by owner");
        @SuppressWarnings("unchecked")
        Map<String,Object> odata = (Map<String,Object>) order.get("data");
        check(((Number) odata.get("orderId")).intValue() == firstId, "order id matches");

        // 10. Cross-user protection: register user2, try to read user1's order -> 403
        String email2 = "e2e2-" + suffix + "@example.com";
        post("/auth/register", map("name", "E2E User2", "email", email2, "password", "password123"));
        Map<String,Object> login2 = null;
        try {
            login2 = post2("/auth/login", map("email", email2, "password", "password123"));
        } catch (HttpStatusException e) { check(false, "user2 login"); }
        String session2 = (String) ((Map<String,Object>) login2.get("data")).get("session");
        boolean forbidden = false;
        try { get("/orders/" + firstId + "?session=" + session2); } catch (HttpStatusException e) { forbidden = e.status == 403; }
        check(forbidden, "other user cannot read order (403)");

        if (failures == 0) {
            System.out.println("ALL E2E FLOW TESTS PASSED");
        } else {
            System.out.println("E2E TESTS FAILED: " + failures);
            System.exit(1);
        }
    }

    static Map<String,Object> post(String path, Map<String,String> form) throws Exception {
        return post2(path, form);
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> post2(String path, Map<String,String> form) throws Exception {
        URL url = new URL(BASE + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        String body = form.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .reduce((a,b) -> a + "&" + b).orElse("");
        try (OutputStream os = con.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        return read(con);
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> get(String path) throws Exception {
        URL url = new URL(BASE + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        return read(con);
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> read(HttpURLConnection con) throws Exception {
        int status = con.getResponseCode();
        java.io.InputStream is = status >= 400 ? con.getErrorStream() : con.getInputStream();
        String body = is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        con.disconnect();
        if (status >= 400) throw new HttpStatusException(status, body);
        return parseJson(body);
    }

    static Map<String,String> map(String... kv) {
        java.util.LinkedHashMap<String,String> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i+1]);
        return m;
    }

    static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> parseJson(String body) {
        // Minimal JSON parse sufficient for our flat/nested responses.
        return (Map<String,Object>) parseValue(body.trim(), new int[]{0});
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    static Object parseValue(String s, int[] i) {
        skipWs(s, i);
        char c = s.charAt(i[0]);
        if (c == '{') return parseObject(s, i);
        if (c == '[') return parseArray(s, i);
        if (c == '"') return parseString(s, i);
        return parseLiteral(s, i);
    }

    @SuppressWarnings("unchecked")
    static Map parseObject(String s, int[] i) {
        Map m = new java.util.LinkedHashMap();
        i[0]++; // skip {
        skipWs(s, i);
        if (s.charAt(i[0]) == '}') { i[0]++; return m; }
        while (true) {
            skipWs(s, i);
            String key = parseString(s, i);
            skipWs(s, i);
            i[0]++; // skip :
            Object val = parseValue(s, i);
            m.put(key, val);
            skipWs(s, i);
            if (s.charAt(i[0]) == '}') { i[0]++; break; }
            i[0]++; // skip ,
        }
        return m;
    }

    @SuppressWarnings("rawtypes")
    static List parseArray(String s, int[] i) {
        List list = new java.util.ArrayList();
        i[0]++; // skip [
        skipWs(s, i);
        if (s.charAt(i[0]) == ']') { i[0]++; return list; }
        while (true) {
            Object val = parseValue(s, i);
            list.add(val);
            skipWs(s, i);
            if (s.charAt(i[0]) == ']') { i[0]++; break; }
            i[0]++; // skip ,
        }
        return list;
    }

    static String parseString(String s, int[] i) {
        i[0]++; // skip "
        StringBuilder sb = new StringBuilder();
        while (s.charAt(i[0]) != '"') {
            char c = s.charAt(i[0]++);
            if (c == '\\') {
                char e = s.charAt(i[0]++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(e);
                }
            } else sb.append(c);
        }
        i[0]++; // skip closing "
        return sb.toString();
    }

    static Object parseLiteral(String s, int[] i) {
        int start = i[0];
        while (i[0] < s.length() && s.charAt(i[0]) != ',' && s.charAt(i[0]) != '}' && s.charAt(i[0]) != ']' && !Character.isWhitespace(s.charAt(i[0]))) i[0]++;
        String lit = s.substring(start, i[0]);
        if ("true".equals(lit)) return Boolean.TRUE;
        if ("false".equals(lit)) return Boolean.FALSE;
        if ("null".equals(lit)) return null;
        if (lit.contains(".")) return Double.parseDouble(lit);
        return Long.parseLong(lit);
    }

    static void skipWs(String s, int[] i) {
        while (i[0] < s.length() && Character.isWhitespace(s.charAt(i[0]))) i[0]++;
    }

    static void check(boolean cond, String label) {
        if (cond) System.out.println("PASS: " + label);
        else { System.out.println("FAIL: " + label); failures++; }
    }

    static class HttpStatusException extends Exception {
        final int status;
        HttpStatusException(int status, String body) { super("HTTP " + status + ": " + body); this.status = status; }
    }
}


