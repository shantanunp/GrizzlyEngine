package com.grizzly.core.types;

/**
 * Represents a boolean value (True/False) in the Grizzly interpreter.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * is_active = True       // BoolValue(true)
 * is_empty = len(items) == 0  // BoolValue depends on condition
 * }</pre>
 * 
 * @param value The boolean value
 */
public record BoolValue(boolean value) implements Value {
    
    /** Singleton for True */
    public static final BoolValue TRUE = new BoolValue(true);
    
    /** Singleton for False */
    public static final BoolValue FALSE = new BoolValue(false);
    
    /**
     * Get a BoolValue for the given boolean.
     * Uses singletons to reduce allocations.
     */
    public static BoolValue of(boolean value) {
        return value ? TRUE : FALSE;
    }
    
    @Override
    public String typeName() {
        return "bool";
    }
    
    @Override
    public boolean isTruthy() {
        return value;
    }
    
    @Override
    public String toString() {
        return value ? "True" : "False";
    }
}
