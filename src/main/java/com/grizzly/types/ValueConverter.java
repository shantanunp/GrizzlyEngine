package com.grizzly.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting between Grizzly Value types and Java objects.
 * 
 * <h2>Purpose</h2>
 * <p>This class is the <b>bridge</b> between:
 * <ul>
 *   <li>The external world (JSON, Java Maps) where data is untyped</li>
 *   <li>The internal Grizzly world where all values are type-safe</li>
 * </ul>
 * 
 * <h2>Why Object is Used Here</h2>
 * <p>JSON is inherently untyped - a JSON value can be a string, number, boolean,
 * null, array, or object. Java represents this with {@code Object}. This class
 * is the <b>only place</b> where raw Objects should appear; internally, Grizzly
 * uses the type-safe {@link Value} hierarchy.
 * 
 * <pre>{@code
 * External World (untyped)     │     Internal World (type-safe)
 * ─────────────────────────────│─────────────────────────────────
 * Map<String, Object>    ──────┼────▶  DictValue
 * List<Object>           ──────┼────▶  ListValue
 * String                 ──────┼────▶  StringValue
 * Number                 ──────┼────▶  NumberValue
 * Boolean                ──────┼────▶  BoolValue
 * null                   ──────┼────▶  NullValue
 *                              │
 *         fromJava()           │           toJava()
 *         fromJavaMap()        │           toJavaMap()
 * }</pre>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Convert JSON input to type-safe Values
 * Map<String, Object> jsonInput = mapper.readValue(json, 
 *     new TypeReference<Map<String, Object>>() {});
 * DictValue input = ValueConverter.fromJavaMap(jsonInput);
 * 
 * // Now work with type-safe Values internally...
 * StringValue name = (StringValue) input.get("name");
 * 
 * // Convert Values back to JSON-compatible objects
 * Map<String, Object> jsonOutput = ValueConverter.toJavaMap(output);
 * String json = mapper.writeValueAsString(jsonOutput);
 * }</pre>
 */
public final class ValueConverter {
    
    private ValueConverter() {}
    
    /**
     * Convert a Java object (from JSON parsing) to a Value.
     * 
     * <p>Handles:
     * <ul>
     *   <li>null → NullValue</li>
     *   <li>String → StringValue</li>
     *   <li>Number (Integer, Long, Double) → NumberValue</li>
     *   <li>Boolean → BoolValue</li>
     *   <li>List → ListValue (recursive)</li>
     *   <li>Map → DictValue (recursive)</li>
     *   <li>Value subtype → returned as-is</li>
     * </ul>
     * 
     * @param obj The Java object to convert
     * @return The corresponding Value
     * @throws IllegalArgumentException if the type is not supported
     */
    @SuppressWarnings("unchecked")
    public static Value fromJava(Object obj) {
        if (obj == null) {
            return NullValue.INSTANCE;
        }
        if (obj instanceof Value v) {
            return v;
        }
        if (obj instanceof String s) {
            return new StringValue(s);
        }
        if (obj instanceof Number n) {
            return new NumberValue(n);
        }
        if (obj instanceof Boolean b) {
            return BoolValue.of(b);
        }
        if (obj instanceof List<?> list) {
            List<Value> items = new ArrayList<>(list.size());
            for (Object item : list) {
                items.add(fromJava(item));
            }
            return new ListValue(items);
        }
        if (obj instanceof Map<?, ?> map) {
            Map<String, Value> entries = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                entries.put(key, fromJava(entry.getValue()));
            }
            return new DictValue(entries);
        }
        throw new IllegalArgumentException(
            "Cannot convert " + obj.getClass().getName() + " to Value"
        );
    }
    
    /**
     * Convert a Java Map to a DictValue.
     * 
     * @param map The Java Map (typically from JSON parsing)
     * @return The corresponding DictValue
     */
    @SuppressWarnings("unchecked")
    public static DictValue fromJavaMap(Map<String, Object> map) {
        if (map == null) {
            return DictValue.empty();
        }
        Map<String, Value> entries = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            entries.put(entry.getKey(), fromJava(entry.getValue()));
        }
        return new DictValue(entries);
    }
    
    /**
     * Convert a Value to a Java object (for JSON serialization).
     * 
     * <p>Handles:
     * <ul>
     *   <li>NullValue → null</li>
     *   <li>StringValue → String</li>
     *   <li>NumberValue → Number (Integer or Double)</li>
     *   <li>BoolValue → Boolean</li>
     *   <li>ListValue → List (recursive)</li>
     *   <li>DictValue → Map (recursive)</li>
     *   <li>DateTimeValue → String (ISO format)</li>
     *   <li>DecimalValue → String (to preserve precision)</li>
     * </ul>
     * 
     * @param value The Value to convert
     * @return The corresponding Java object
     */
    public static Object toJava(Value value) {
        return switch (value) {
            case NullValue ignored -> null;
            case StringValue s -> s.value();
            case NumberValue n -> {
                if (n.isInteger()) {
                    long l = n.asLong();
                    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                        yield (int) l;
                    }
                    yield l;
                }
                yield n.asDouble();
            }
            case BoolValue b -> b.value();
            case ListValue l -> {
                List<Object> list = new ArrayList<>(l.size());
                for (Value item : l.items()) {
                    list.add(toJava(item));
                }
                yield list;
            }
            case DictValue d -> {
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<String, Value> entry : d.entries().entrySet()) {
                    map.put(entry.getKey(), toJava(entry.getValue()));
                }
                yield map;
            }
            case DateTimeValue dt -> dt.toString();
            case DecimalValue dec -> dec.toString();
        };
    }
    
    /**
     * Convert a DictValue to a Java Map.
     * 
     * @param dict The DictValue to convert
     * @return The corresponding Java Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toJavaMap(DictValue dict) {
        return (Map<String, Object>) toJava(dict);
    }
}
