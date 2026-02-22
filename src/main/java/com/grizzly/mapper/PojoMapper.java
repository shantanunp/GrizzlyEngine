package com.grizzly.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * Converts between Java POJOs and Maps using Jackson
 * 
 * This is the bridge between Java objects and Python dictionaries:
 * 
 * Java POJO to Map (for Python INPUT)
 * Map to Java POJO (for Python OUTPUT)
 * 
 * Example:
 * {@code
 * Customer customer = new Customer("C123", "John");
 * Map<String, Object> map = mapper.pojoToMap(customer);
 * // Result: {"customerId": "C123", "firstName": "John"}
 * 
 * CustomerDTO dto = mapper.mapToPojo(map, CustomerDTO.class);
 * // Result: CustomerDTO with id="C123", name="John"
 * }
 */
public class PojoMapper {
    
    private final ObjectMapper jackson;
    
    public PojoMapper() {
        this.jackson = new ObjectMapper();
        this.jackson.registerModule(new JavaTimeModule()); // Support for Java 8 date/time
        this.jackson.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Convert a POJO to a Map, preserving special types like DateTimeValue
     * 
     * @param pojo Any Java object
     * @return Map representation
     */
    public Map<String, Object> pojoToMap(Object pojo) {
        try {
            // Special handling for DateTimeValue - don't convert it
            if (pojo instanceof com.grizzly.types.DateTimeValue) {
                return java.util.Collections.singletonMap("value", pojo);
            }
            
            Map<String, Object> result = jackson.convertValue(pojo, new TypeReference<Map<String, Object>>() {});
            
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
     * Convert a Map to a POJO
     * 
     * @param map Map with data
     * @param clazz Target class
     * @return Instance of the target class
     */
    public <T> T mapToPojo(Map<String, Object> map, Class<T> clazz) {
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
