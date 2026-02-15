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
 * Java POJO → Map (for Python INPUT)
 * Map → Java POJO (for Python OUTPUT)
 * 
 * Example:
 * 
 * Customer customer = new Customer("C123", "John");
 * Map<String, Object> map = mapper.pojoToMap(customer);
 * // Result: {"customerId": "C123", "firstName": "John"}
 * 
 * CustomerDTO dto = mapper.mapToPojo(map, CustomerDTO.class);
 * // Result: CustomerDTO with id="C123", name="John"
 */
public class PojoMapper {
    
    private final ObjectMapper jackson;
    
    public PojoMapper() {
        this.jackson = new ObjectMapper();
        this.jackson.registerModule(new JavaTimeModule()); // Support for Java 8 date/time
        this.jackson.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Convert a POJO to a Map
     * 
     * @param pojo Any Java object
     * @return Map representation
     */
    public Map<String, Object> pojoToMap(Object pojo) {
        try {
            return jackson.convertValue(pojo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert POJO to Map: " + e.getMessage(), e);
        }
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
