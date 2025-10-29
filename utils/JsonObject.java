package p2pblockchain.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON object helper used by the project.
 * <p>
 * Supports building JSON programmatically and parsing JSON produced by
 * this project's serializers. It is intentionally small and permissive.
 */
public class JsonObject {
    private final Map<String, Object> map;

    /** Create an empty JsonObject. */
    public JsonObject() {
        this.map = new LinkedHashMap<>();
    }

    /** Parse a JSON object from a string. */
    public JsonObject(String json) throws Exception {
        this.map = new LinkedHashMap<>();
        Parser parser = new Parser(json);
        JsonObject parsed = parser.parseObject();
        this.map.putAll(parsed.map);
    }

    public void put(String key, String value) { map.put(key, value); }
    public void put(String key, int value) { map.put(key, Integer.valueOf(value)); }
    public void put(String key, long value) { map.put(key, Long.valueOf(value)); }
    public void put(String key, double value) { map.put(key, Double.valueOf(value)); }
    public void put(String key, boolean value) { map.put(key, Boolean.valueOf(value)); }
    public void put(String key, JsonArray value) { map.put(key, value); }
    public void put(String key, JsonObject value) { map.put(key, value); }
    public void put(String key, Object value) { map.put(key, value); }

    public String getString(String key) { return (String) map.get(key); }
    public int getInt(String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number)v).intValue();
        if (v == null) return 0;
        return Integer.parseInt(v.toString());
    }
    public long getLong(String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number)v).longValue();
        if (v == null) return 0L;
        return Long.parseLong(v.toString());
    }
    public double getDouble(String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number)v).doubleValue();
        if (v == null) return 0.0;
        return Double.parseDouble(v.toString());
    }

    public JsonArray getJsonArray(String key) { return (JsonArray) map.get(key); }
    public JsonObject getJsonObject(String key) { return (JsonObject) map.get(key); }
    /**
     * Return a boolean value for a key. Accepts stored Boolean, numeric or
     * string values. Returns false when null or unparsable.
     *
     * @param key JSON key
     * @return boolean value (false if missing or invalid)
     */
    public boolean getBoolean(String key) {
        Object v = map.get(key);
        if (v instanceof Boolean) return ((Boolean) v).booleanValue();
        if (v == null) return false;
        // try to parse numbers and strings
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        try {
            return Boolean.parseBoolean(v.toString());
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            sb.append(valueToString(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    private String valueToString(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return '"' + escape((String)v) + '"';
        if (v instanceof JsonObject) return v.toString();
        if (v instanceof JsonArray) return v.toString();
        if (v instanceof Boolean) return v.toString();
        if (v instanceof Number) return v.toString();
        return '"' + escape(v.toString()) + '"';
    }

    private String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /* ----- Lightweight JSON parser (only what we need) ----- */
    private static final class Parser {
        private final String s;
        private int i;
        Parser(String s) { this.s = s == null ? "" : s.trim(); this.i = 0; }

        JsonObject parseObject() throws Exception {
            skipWhitespace();
            if (peek() != '{') throw new Exception("Expected '{' at start of object");
            i++; // consume '{'
            JsonObject obj = new JsonObject();
            skipWhitespace();
            if (peek() == '}') { i++; return obj; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (peek() != ':') throw new Exception("Expected ':' after key");
                i++;
                skipWhitespace();
                Object val = parseValue();
                obj.put(key, wrapValue(val));
                skipWhitespace();
                char c = peek();
                if (c == ',') { i++; continue; }
                else if (c == '}') { i++; break; }
                else throw new Exception("Expected ',' or '}' in object");
            }
            return obj;
        }

        private Object wrapValue(Object val) {
            // parser returns JsonObject/JsonArray for structured types already
            return val;
        }

        private Object parseValue() throws Exception {
            skipWhitespace();
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') { parseNull(); return null; }
            return parseNumber();
        }

        private JsonArray parseArray() throws Exception {
            i++; // consume '['
            JsonArray arr = new JsonArray();
            skipWhitespace();
            if (peek() == ']') { i++; return arr; }
            while (true) {
                skipWhitespace();
                Object v = parseValue();
                arr.addRaw(v);
                skipWhitespace();
                char c = peek();
                if (c == ',') { i++; continue; }
                else if (c == ']') { i++; break; }
                else throw new Exception("Expected ',' or ']' in array");
            }
            return arr;
        }

        private String parseString() throws Exception {
            if (peek() != '"') throw new Exception("Expected string");
            i++; // consume '"'
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    if (i >= s.length()) break;
                    char esc = s.charAt(i++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (i + 4 <= s.length()) {
                                String hex = s.substring(i, i+4);
                                i += 4;
                                sb.append((char) Integer.parseInt(hex, 16));
                            }
                            break;
                        default:
                            sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() throws Exception {
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            throw new Exception("Invalid boolean");
        }

        private void parseNull() throws Exception {
            if (s.startsWith("null", i)) { i += 4; return; }
            throw new Exception("Invalid null");
        }

        private Number parseNumber() throws Exception {
            int start = i;
            if (peek() == '-') i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            boolean isFloat = false;
            if (i < s.length() && s.charAt(i) == '.') {
                isFloat = true; i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            if (i < s.length()) {
                char c = s.charAt(i);
                if (c == 'e' || c == 'E') {
                    isFloat = true; i++;
                    if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                    while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
                }
            }
            String numStr = s.substring(start, i);
            try {
                if (isFloat) return Double.parseDouble(numStr);
                long lv = Long.parseLong(numStr);
                if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) return Integer.valueOf((int)lv);
                return Long.valueOf(lv);
            } catch (NumberFormatException ex) {
                throw new Exception("Invalid number: " + numStr);
            }
        }

        private void skipWhitespace() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private char peek() {
            if (i >= s.length()) return '\0';
            return s.charAt(i);
        }
    }
}
