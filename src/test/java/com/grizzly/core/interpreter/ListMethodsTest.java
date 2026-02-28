package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ListMethods - new list method implementations.
 */
class ListMethodsTest {
    
    // ==================== remove() ====================
    
    @Test
    @DisplayName("remove() removes first occurrence of value")
    void remove() {
        ListValue list = createList(1, 2, 3, 2, 4);
        ListMethods.evaluate(list, "remove", List.of(NumberValue.of(2)));
        
        assertThat(list.size()).isEqualTo(4);
        assertThat(((NumberValue) list.get(0)).asInt()).isEqualTo(1);
        assertThat(((NumberValue) list.get(1)).asInt()).isEqualTo(3);
        assertThat(((NumberValue) list.get(2)).asInt()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("remove() throws when value not found")
    void removeNotFound() {
        ListValue list = createList(1, 2, 3);
        assertThatThrownBy(() -> ListMethods.evaluate(list, "remove", List.of(NumberValue.of(99))))
            .isInstanceOf(GrizzlyExecutionException.class)
            .hasMessageContaining("not in list");
    }
    
    // ==================== clear() ====================
    
    @Test
    @DisplayName("clear() removes all elements")
    void clear() {
        ListValue list = createList(1, 2, 3);
        ListMethods.evaluate(list, "clear", List.of());
        
        assertThat(list.size()).isEqualTo(0);
        assertThat(list.isEmpty()).isTrue();
    }
    
    // ==================== copy() ====================
    
    @Test
    @DisplayName("copy() creates shallow copy")
    void copy() {
        ListValue original = createList(1, 2, 3);
        Value result = ListMethods.evaluate(original, "copy", List.of());
        
        assertThat(result).isInstanceOf(ListValue.class);
        ListValue copy = (ListValue) result;
        
        assertThat(copy.size()).isEqualTo(3);
        assertThat(copy).isNotSameAs(original);
        
        // Modifying copy doesn't affect original
        copy.append(NumberValue.of(4));
        assertThat(original.size()).isEqualTo(3);
        assertThat(copy.size()).isEqualTo(4);
    }
    
    // ==================== sort() with reverse ====================
    
    @Test
    @DisplayName("sort() sorts in ascending order by default")
    void sortAscending() {
        ListValue list = createList(3, 1, 4, 1, 5);
        ListMethods.evaluate(list, "sort", List.of());
        
        assertThat(((NumberValue) list.get(0)).asInt()).isEqualTo(1);
        assertThat(((NumberValue) list.get(1)).asInt()).isEqualTo(1);
        assertThat(((NumberValue) list.get(2)).asInt()).isEqualTo(3);
        assertThat(((NumberValue) list.get(3)).asInt()).isEqualTo(4);
        assertThat(((NumberValue) list.get(4)).asInt()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("sort() with reverse=True sorts descending")
    void sortDescending() {
        ListValue list = createList(3, 1, 4, 1, 5);
        ListMethods.evaluate(list, "sort", List.of(BoolValue.TRUE));
        
        assertThat(((NumberValue) list.get(0)).asInt()).isEqualTo(5);
        assertThat(((NumberValue) list.get(1)).asInt()).isEqualTo(4);
        assertThat(((NumberValue) list.get(2)).asInt()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("sort() sorts strings alphabetically")
    void sortStrings() {
        List<Value> items = new ArrayList<>();
        items.add(new StringValue("banana"));
        items.add(new StringValue("apple"));
        items.add(new StringValue("cherry"));
        ListValue list = new ListValue(items);
        
        ListMethods.evaluate(list, "sort", List.of());
        
        assertThat(((StringValue) list.get(0)).value()).isEqualTo("apple");
        assertThat(((StringValue) list.get(1)).value()).isEqualTo("banana");
        assertThat(((StringValue) list.get(2)).value()).isEqualTo("cherry");
    }
    
    // ==================== index() with start/end ====================
    
    @Test
    @DisplayName("index() finds first occurrence")
    void index() {
        ListValue list = createList(1, 2, 3, 2, 4);
        Value result = ListMethods.evaluate(list, "index", List.of(NumberValue.of(2)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("index() with start parameter")
    void indexWithStart() {
        ListValue list = createList(1, 2, 3, 2, 4);
        Value result = ListMethods.evaluate(list, "index", 
            List.of(NumberValue.of(2), NumberValue.of(2)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("index() with start and end parameters")
    void indexWithStartEnd() {
        ListValue list = createList(1, 2, 3, 2, 4);
        
        // Should find 2 at index 1, not the one at index 3
        Value result = ListMethods.evaluate(list, "index", 
            List.of(NumberValue.of(2), NumberValue.of(0), NumberValue.of(2)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("index() throws when not found")
    void indexNotFound() {
        ListValue list = createList(1, 2, 3);
        assertThatThrownBy(() -> ListMethods.evaluate(list, "index", List.of(NumberValue.of(99))))
            .isInstanceOf(GrizzlyExecutionException.class)
            .hasMessageContaining("not found");
    }
    
    // ==================== count() ====================
    
    @Test
    @DisplayName("count() counts occurrences")
    void count() {
        ListValue list = createList(1, 2, 2, 3, 2, 4);
        Value result = ListMethods.evaluate(list, "count", List.of(NumberValue.of(2)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("count() returns 0 for missing value")
    void countMissing() {
        ListValue list = createList(1, 2, 3);
        Value result = ListMethods.evaluate(list, "count", List.of(NumberValue.of(99)));
        
        assertThat(((NumberValue) result).asInt()).isEqualTo(0);
    }
    
    // ==================== Helper Methods ====================
    
    private ListValue createList(int... values) {
        List<Value> items = new ArrayList<>();
        for (int v : values) {
            items.add(NumberValue.of(v));
        }
        return new ListValue(items);
    }
}
