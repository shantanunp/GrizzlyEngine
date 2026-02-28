package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DictMethods - dict method implementations.
 */
class DictMethodsTest {
    
    // ==================== get() ====================
    
    @Test
    @DisplayName("get() returns value for existing key")
    void getExistingKey() {
        DictValue dict = createDict("name", "John", "age", 30);
        Value result = DictMethods.evaluate(dict, "get", List.of(new StringValue("name")));
        
        assertThat(((StringValue) result).value()).isEqualTo("John");
    }
    
    @Test
    @DisplayName("get() returns None for missing key")
    void getMissingKey() {
        DictValue dict = createDict("name", "John");
        Value result = DictMethods.evaluate(dict, "get", List.of(new StringValue("age")));
        
        assertThat(result).isEqualTo(NullValue.INSTANCE);
    }
    
    @Test
    @DisplayName("get() returns default value for missing key")
    void getMissingKeyWithDefault() {
        DictValue dict = createDict("name", "John");
        Value result = DictMethods.evaluate(dict, "get", 
            List.of(new StringValue("age"), NumberValue.of(0)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(0);
    }
    
    // ==================== keys() ====================
    
    @Test
    @DisplayName("keys() returns list of all keys")
    void keys() {
        DictValue dict = createDict("a", 1, "b", 2, "c", 3);
        Value result = DictMethods.evaluate(dict, "keys", List.of());
        
        assertThat(result).isInstanceOf(ListValue.class);
        ListValue keys = (ListValue) result;
        assertThat(keys.size()).isEqualTo(3);
    }
    
    // ==================== values() ====================
    
    @Test
    @DisplayName("values() returns list of all values")
    void values() {
        DictValue dict = createDict("a", 1, "b", 2, "c", 3);
        Value result = DictMethods.evaluate(dict, "values", List.of());
        
        assertThat(result).isInstanceOf(ListValue.class);
        ListValue values = (ListValue) result;
        assertThat(values.size()).isEqualTo(3);
    }
    
    // ==================== items() ====================
    
    @Test
    @DisplayName("items() returns list of [key, value] pairs")
    void items() {
        DictValue dict = createDict("name", "John", "age", 30);
        Value result = DictMethods.evaluate(dict, "items", List.of());
        
        assertThat(result).isInstanceOf(ListValue.class);
        ListValue items = (ListValue) result;
        assertThat(items.size()).isEqualTo(2);
        
        // Each item should be a [key, value] pair
        ListValue firstItem = (ListValue) items.get(0);
        assertThat(firstItem.size()).isEqualTo(2);
    }
    
    // ==================== pop() ====================
    
    @Test
    @DisplayName("pop() removes and returns value for key")
    void pop() {
        DictValue dict = createDict("a", 1, "b", 2);
        Value result = DictMethods.evaluate(dict, "pop", List.of(new StringValue("a")));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(1);
        assertThat(dict.containsKey("a")).isFalse();
        assertThat(dict.size()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("pop() returns default for missing key")
    void popMissingWithDefault() {
        DictValue dict = createDict("a", 1);
        Value result = DictMethods.evaluate(dict, "pop", 
            List.of(new StringValue("z"), NumberValue.of(-1)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(-1);
    }
    
    @Test
    @DisplayName("pop() throws KeyError for missing key without default")
    void popMissingWithoutDefault() {
        DictValue dict = createDict("a", 1);
        assertThatThrownBy(() -> DictMethods.evaluate(dict, "pop", List.of(new StringValue("z"))))
            .isInstanceOf(GrizzlyExecutionException.class)
            .hasMessageContaining("KeyError");
    }
    
    // ==================== update() ====================
    
    @Test
    @DisplayName("update() merges another dict")
    void update() {
        DictValue dict1 = createDict("a", 1, "b", 2);
        DictValue dict2 = createDict("b", 20, "c", 30);
        
        DictMethods.evaluate(dict1, "update", List.of(dict2));
        
        assertThat(dict1.size()).isEqualTo(3);
        assertThat(((NumberValue) dict1.get("a")).asInt()).isEqualTo(1);
        assertThat(((NumberValue) dict1.get("b")).asInt()).isEqualTo(20); // Updated
        assertThat(((NumberValue) dict1.get("c")).asInt()).isEqualTo(30); // Added
    }
    
    // ==================== clear() ====================
    
    @Test
    @DisplayName("clear() removes all entries")
    void clear() {
        DictValue dict = createDict("a", 1, "b", 2);
        DictMethods.evaluate(dict, "clear", List.of());
        
        assertThat(dict.size()).isEqualTo(0);
    }
    
    // ==================== copy() ====================
    
    @Test
    @DisplayName("copy() creates shallow copy")
    void copy() {
        DictValue original = createDict("a", 1, "b", 2);
        Value result = DictMethods.evaluate(original, "copy", List.of());
        
        assertThat(result).isInstanceOf(DictValue.class);
        DictValue copy = (DictValue) result;
        
        assertThat(copy.size()).isEqualTo(2);
        assertThat(copy).isNotSameAs(original);
        
        // Modifying copy doesn't affect original
        copy.put("c", NumberValue.of(3));
        assertThat(original.size()).isEqualTo(2);
        assertThat(copy.size()).isEqualTo(3);
    }
    
    // ==================== setdefault() ====================
    
    @Test
    @DisplayName("setdefault() returns existing value if key exists")
    void setdefaultExisting() {
        DictValue dict = createDict("a", 1);
        Value result = DictMethods.evaluate(dict, "setdefault", 
            List.of(new StringValue("a"), NumberValue.of(99)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(1);
        assertThat(((NumberValue) dict.get("a")).asInt()).isEqualTo(1); // Unchanged
    }
    
    @Test
    @DisplayName("setdefault() sets and returns default if key missing")
    void setdefaultMissing() {
        DictValue dict = createDict("a", 1);
        Value result = DictMethods.evaluate(dict, "setdefault", 
            List.of(new StringValue("b"), NumberValue.of(99)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(99);
        assertThat(dict.containsKey("b")).isTrue();
        assertThat(((NumberValue) dict.get("b")).asInt()).isEqualTo(99);
    }
    
    @Test
    @DisplayName("setdefault() uses None as default if not provided")
    void setdefaultNoDefault() {
        DictValue dict = createDict("a", 1);
        Value result = DictMethods.evaluate(dict, "setdefault", 
            List.of(new StringValue("b")));
        
        assertThat(result).isEqualTo(NullValue.INSTANCE);
        assertThat(dict.get("b")).isEqualTo(NullValue.INSTANCE);
    }
    
    // ==================== Helper Methods ====================
    
    private DictValue createDict(Object... keyValues) {
        DictValue dict = DictValue.empty();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object val = keyValues[i + 1];
            Value value = val instanceof Integer 
                ? NumberValue.of((Integer) val) 
                : new StringValue((String) val);
            dict.put(key, value);
        }
        return dict;
    }
}
