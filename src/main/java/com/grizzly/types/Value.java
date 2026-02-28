package com.grizzly.types;

/**
 * Sealed interface representing all runtime values in the Grizzly interpreter.
 * 
 * <p>This provides type safety and enables exhaustive pattern matching
 * in switch expressions. Every value in a Grizzly program is one of these types.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * Value result = evaluate(expression);
 * 
 * return switch (result) {
 *     case StringValue s -> handleString(s.value());
 *     case NumberValue n -> handleNumber(n.value());
 *     case BoolValue b -> handleBool(b.value());
 *     case ListValue l -> handleList(l.items());
 *     case DictValue d -> handleDict(d.entries());
 *     case NullValue ignored -> handleNull();
 *     case DateTimeValue dt -> handleDateTime(dt);
 *     case DecimalValue dec -> handleDecimal(dec);
 * };
 * }</pre>
 */
public sealed interface Value permits 
    StringValue, 
    NumberValue, 
    BoolValue, 
    ListValue, 
    DictValue, 
    NullValue,
    DateTimeValue, 
    DecimalValue {
    
    /**
     * Returns the type name for error messages and debugging.
     * 
     * @return Human-readable type name (e.g., "string", "number", "list")
     */
    String typeName();
    
    /**
     * Check if this value is truthy (for conditionals).
     * 
     * <p>Truthiness rules:
     * <ul>
     *   <li>NullValue → false</li>
     *   <li>BoolValue → its value</li>
     *   <li>NumberValue → false if 0, true otherwise</li>
     *   <li>StringValue → false if empty, true otherwise</li>
     *   <li>ListValue → false if empty, true otherwise</li>
     *   <li>DictValue → false if empty, true otherwise</li>
     *   <li>DateTimeValue → true</li>
     *   <li>DecimalValue → false if 0, true otherwise</li>
     * </ul>
     * 
     * @return true if the value is truthy
     */
    boolean isTruthy();
}
