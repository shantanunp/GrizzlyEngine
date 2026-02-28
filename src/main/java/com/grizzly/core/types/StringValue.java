package com.grizzly.core.types;

/**
 * Represents a string value in the Grizzly interpreter.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * name = "Alice"           // StringValue("Alice")
 * greeting = "Hello, " + name  // StringValue("Hello, Alice")
 * }</pre>
 * 
 * @param value The string content
 */
public record StringValue(String value) implements Value {
    
    public StringValue {
        if (value == null) {
            throw new IllegalArgumentException("StringValue cannot contain null - use NullValue instead");
        }
    }
    
    @Override
    public String typeName() {
        return "string";
    }
    
    @Override
    public boolean isTruthy() {
        return !value.isEmpty();
    }
    
    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
