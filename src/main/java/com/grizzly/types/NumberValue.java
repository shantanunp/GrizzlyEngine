package com.grizzly.types;

/**
 * Represents a numeric value (integer or floating-point) in the Grizzly interpreter.
 * 
 * <p>Internally stores as a Java {@link Number}, supporting both integers and doubles.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * count = 42          // NumberValue(42)
 * price = 19.99       // NumberValue(19.99)
 * result = 10 / 3     // NumberValue(3.333...)
 * }</pre>
 * 
 * @param value The numeric value (Integer, Long, Double, etc.)
 */
public record NumberValue(Number value) implements Value {
    
    public NumberValue {
        if (value == null) {
            throw new IllegalArgumentException("NumberValue cannot contain null - use NullValue instead");
        }
    }
    
    /**
     * Create a NumberValue from an int.
     */
    public static NumberValue of(int value) {
        return new NumberValue(value);
    }
    
    /**
     * Create a NumberValue from a double.
     */
    public static NumberValue of(double value) {
        return new NumberValue(value);
    }
    
    /**
     * Create a NumberValue from a long.
     */
    public static NumberValue of(long value) {
        return new NumberValue(value);
    }
    
    /**
     * Get the value as a double.
     */
    public double asDouble() {
        return value.doubleValue();
    }
    
    /**
     * Get the value as an int (truncates decimal part).
     */
    public int asInt() {
        return value.intValue();
    }
    
    /**
     * Get the value as a long (truncates decimal part).
     */
    public long asLong() {
        return value.longValue();
    }
    
    /**
     * Check if this number is an integer (no decimal part).
     */
    public boolean isInteger() {
        double d = value.doubleValue();
        return d == Math.floor(d) && !Double.isInfinite(d);
    }
    
    @Override
    public String typeName() {
        return "number";
    }
    
    @Override
    public boolean isTruthy() {
        return value.doubleValue() != 0;
    }
    
    @Override
    public String toString() {
        if (isInteger()) {
            return String.valueOf(value.longValue());
        }
        return value.toString();
    }
}
