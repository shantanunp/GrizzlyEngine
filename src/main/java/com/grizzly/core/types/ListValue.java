package com.grizzly.core.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a list/array value in the Grizzly interpreter.
 * 
 * <p>Lists are mutable and can contain any Value type.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * items = [1, 2, 3]           // ListValue with NumberValues
 * mixed = ["a", 1, True]      // ListValue with mixed types
 * empty = []                  // Empty ListValue
 * items.append(4)             // Mutation supported
 * }</pre>
 * 
 * @param items The list of values (mutable)
 */
public record ListValue(List<Value> items) implements Value, Iterable<Value> {
    
    /**
     * Create an empty ListValue.
     */
    public static ListValue empty() {
        return new ListValue(new ArrayList<>());
    }
    
    /**
     * Create a ListValue from varargs.
     */
    public static ListValue of(Value... values) {
        List<Value> list = new ArrayList<>(values.length);
        Collections.addAll(list, values);
        return new ListValue(list);
    }
    
    /**
     * Get the size of the list.
     */
    public int size() {
        return items.size();
    }
    
    /**
     * Check if the list is empty.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    /**
     * Get an element by index (supports negative indices).
     * 
     * @param index The index (-1 for last, -2 for second-to-last, etc.)
     * @return The value at that index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Value get(int index) {
        if (index < 0) {
            index = items.size() + index;
        }
        return items.get(index);
    }
    
    /**
     * Set an element by index (supports negative indices).
     * 
     * @param index The index (-1 for last, -2 for second-to-last, etc.)
     * @param value The value to set
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void set(int index, Value value) {
        if (index < 0) {
            index = items.size() + index;
        }
        items.set(index, value);
    }
    
    /**
     * Append a value to the end of the list.
     */
    public void append(Value value) {
        items.add(value);
    }
    
    /**
     * Extend this list with all items from another list.
     */
    public void extend(ListValue other) {
        items.addAll(other.items);
    }
    
    @Override
    public Iterator<Value> iterator() {
        return items.iterator();
    }
    
    @Override
    public String typeName() {
        return "list";
    }
    
    @Override
    public boolean isTruthy() {
        return !items.isEmpty();
    }
    
    @Override
    public String toString() {
        return items.toString();
    }
}
