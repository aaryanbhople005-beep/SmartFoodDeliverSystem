package web;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tiny dependency-free JSON serializer.
 *
 * The project has no build system and no external JSON library, so responses are
 * serialized here manually from Maps/Lists/primitives. DTO objects should expose
 * their data through a Map&lt;String,Object&gt;-friendly structure.
 */
public final class JsonUtil {

    private JsonUtil() {}

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        appendValue(sb, value);
        return sb.toString();
    }

    private static void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append('"').append(escape((String) value)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map) {
            appendMap(sb, (Map<?, ?>) value);
        } else if (value instanceof List) {
            appendList(sb, (List<?>) value);
        } else if (value.getClass().isArray()) {
            appendList(sb, Arrays.asList((Object[]) value));
        } else {
            sb.append('"').append(escape(value.toString())).append('"');
        }
    }

    private static void appendMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(String.valueOf(entry.getKey()))).append('"');
            sb.append(':');
            appendValue(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void appendList(StringBuilder sb, List<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            first = false;
            appendValue(sb, item);
        }
        sb.append(']');
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}