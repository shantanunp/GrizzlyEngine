package com.grizzly.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;
import java.util.Objects;

/**
 * Converts between Java POJOs and Maps using Jackson.
 * 
 * <p>This is the bridge between Java objects and Python dictionaries:
 * <ul>
 *   <li>Java POJO → Map (for Python INPUT)</li>
 *   <li>Map → Java POJO (for Python OUTPUT)</li>
 * </ul>
 * 
 * <h2>Example:</h2>
 * <pre>{@code
 * PojoMapper mapper = new PojoMapper();
 * 
 * // POJO to Map
 * Customer customer = new Customer("C123", "John");
 * Map<String, Object> map = mapper.pojoToMap(customer);
 * // Result: {"customerId": "C123", "firstName": "John"}
 * 
 * // Map to POJO
 * CustomerDTO dto = mapper.mapToPojo(map, CustomerDTO.class);
 * // Result: CustomerDTO with id="C123", name="John"
 * }</pre>
 * 
 * <h2>Note on Object Usage:</h2>
 * <p>This class uses {@code Map<String, Object>} because JSON is inherently
 * untyped. The Object type is necessary here to represent any JSON value
 * (strings, numbers, arrays, nested objects, etc.). Type safety is restored
 * when converting back to a typed POJO.
 */
public class PojoMapper {
    
    private final ObjectMapper jackson;
    
    /**
     * Create a new PojoMapper with default Jackson configuration.
     * 
     * <p>Configuration includes:
     * <ul>
     *   <li>Java 8 date/time support (JavaTimeModule)</li>
     *   <li>ISO-8601 date format (not timestamps)</li>
     * </ul>
     */
    public PojoMapper() {
        this.jackson = new ObjectMapper();
        this.jackson.registerModule(new JavaTimeModule());
        this.jackson.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Convert a POJO to a Map, preserving special types like DateTimeValue.
     * 
     * <p>The resulting Map can be passed to the Grizzly interpreter as INPUT.
     * 
     * @param <T> The type of the POJO
     * @param pojo Any Java object (not null)
     * @return Map representation suitable for Grizzly INPUT
     * @throws NullPointerException if pojo is null
     * @throws RuntimeException if conversion fails
     */
    public <T> Map<String, Object> pojoToMap(T pojo) {
        Objects.requireNonNull(pojo, "pojo cannot be null");
        
        try {
            // Special handling for DateTimeValue - don't convert it
            if (pojo instanceof com.grizzly.core.types.DateTimeValue) {
                return java.util.Collections.singletonMap("value", pojo);
            }
            
            Map<String, Object> result = jackson.convertValue(
                pojo, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            // Recursively preserve DateTimeValue in nested maps
            return preserveDateTimeValues(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert POJO to Map: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recursively preserve DateTimeValue objects in maps
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> preserveDateTimeValues(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                
                // Check if this is a serialized DateTimeValue
                if (nestedMap.containsKey("value") && 
                    nestedMap.get("value") instanceof String &&
                    nestedMap.size() <= 10) { // DateTimeValue has specific fields
                    // Leave as-is (might be DateTimeValue)
                } else {
                    entry.setValue(preserveDateTimeValues(nestedMap));
                }
            } else if (value instanceof java.util.List) {
                // Preserve in lists too
                java.util.List<Object> list = (java.util.List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Map) {
                        list.set(i, preserveDateTimeValues((Map<String, Object>) list.get(i)));
                    }
                }
            }
        }
        return map;
    }
    
    /**
     * Convert a Map to a POJO.
     * 
     * <p>The Map typically comes from Grizzly interpreter OUTPUT.
     * 
     * @param <T> The target POJO type
     * @param map Map with data (not null)
     * @param clazz Target class (not null)
     * @return Instance of the target class populated with map data
     * @throws NullPointerException if map or clazz is null
     * @throws RuntimeException if conversion fails (e.g., type mismatch)
     */
    public <T> T mapToPojo(Map<String, Object> map, Class<T> clazz) {
        Objects.requireNonNull(map, "map cannot be null");
        Objects.requireNonNull(clazz, "clazz cannot be null");
        
        try {
            return jackson.convertValue(map, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Map to POJO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the underlying Jackson ObjectMapper (for advanced usage)
     */
    public ObjectMapper getJackson() {
        return jackson;
    }
}
