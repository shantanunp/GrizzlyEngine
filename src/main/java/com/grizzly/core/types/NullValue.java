package com.grizzly.core.types;

/**
 * Represents a null/None value in the Grizzly interpreter.
 * 
 * <p>Uses singleton pattern since all null values are equivalent.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * result = None              // NullValue
 * missing = INPUT.nonexistent  // NullValue if key doesn't exist
 * }</pre>
 */
public record NullValue() implements Value {
    
    /** Singleton instance - use this instead of creating new instances */
    public static final NullValue INSTANCE = new NullValue();
    
    @Override
    public String typeName() {
        return "null";
    }
    
    @Override
    public boolean isTruthy() {
        return false;
    }
    
    @Override
    public String toString() {
        return "None";
    }
}
