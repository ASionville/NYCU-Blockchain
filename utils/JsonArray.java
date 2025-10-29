package p2pblockchain.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal JSON array helper used by the project.
 */
public class JsonArray {
    private final List<Object> list;

    public JsonArray() { this.list = new ArrayList<>(); }

    public void add(JsonObject o) { list.add(o); }
    public void add(String s) { list.add(s); }
    public void add(int v) { list.add(Integer.valueOf(v)); }
    public void add(long v) { list.add(Long.valueOf(v)); }
    public void add(double v) { list.add(Double.valueOf(v)); }
    public void add(boolean v) { list.add(Boolean.valueOf(v)); }

    /**
     * Internal: add a raw parsed value from the parser (JsonObject, JsonArray, Number, String, Boolean, null)
     */
    void addRaw(Object v) { list.add(v); }

    public int size() { return list.size(); }

    public JsonObject getJsonObject(int index) { return (JsonObject) list.get(index); }
    public String getString(int index) { return (String) list.get(index); }
    public int getInt(int index) { return ((Number)list.get(index)).intValue(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Object v : list) {
            if (!first) sb.append(',');
            first = false;
            sb.append(valueToString(v));
        }
        sb.append(']');
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
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int)c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
