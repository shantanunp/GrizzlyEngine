package com.grizzly.core.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a dictionary/map value in the Grizzly interpreter.
 * 
 * <p>Dictionaries are mutable and map string keys to any Value type.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * OUTPUT = {}                      // Empty DictValue
 * OUTPUT["name"] = "Alice"         // Set key
 * OUTPUT["age"] = 30               // Set key
 * name = INPUT.firstName           // Get via attribute access
 * city = INPUT["address"]["city"]  // Nested access
 * }</pre>
 * 
 * @param entries The map of key-value pairs (mutable)
 */
public record DictValue(Map<String, Value> entries) implements Value {
    
    /**
     * Create an empty DictValue.
     */
    public static DictValue empty() {
        return new DictValue(new HashMap<>());
    }
    
    /**
     * Create a DictValue from a Java Map.
     */
    public static DictValue of(Map<String, Value> map) {
        return new DictValue(new HashMap<>(map));
    }
    
    /**
     * Get the number of entries.
     */
    public int size() {
        return entries.size();
    }
    
    /**
     * Check if the dictionary is empty.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    /**
     * Get a value by key.
     * 
     * @param key The key to look up
     * @return The value, or NullValue if key doesn't exist
     */
    public Value get(String key) {
        Value value = entries.get(key);
        return value != null ? value : NullValue.INSTANCE;
    }
    
    /**
     * Get a value by key, returning null if not found (for internal use).
     */
    public Value getOrNull(String key) {
        return entries.get(key);
    }
    
    /**
     * Set a key-value pair.
     */
    public void put(String key, Value value) {
        entries.put(key, value);
    }
    
    /**
     * Check if a key exists.
     */
    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }
    
    /**
     * Get all keys.
     */
    public Set<String> keys() {
        return entries.keySet();
    }
    
    @Override
    public String typeName() {
        return "dict";
    }
    
    @Override
    public boolean isTruthy() {
        return !entries.isEmpty();
    }
    
    @Override
    public String toString() {
        return entries.toString();
    }
}
