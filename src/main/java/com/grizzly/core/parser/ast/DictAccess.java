package com.grizzly.core.parser.ast;

/**
 * Dictionary/list access expression: {@code object[key]} or {@code object?[key]}
 * 
 * <p>Represents accessing an element by key (for dictionaries) or by index
 * (for lists). The key can be any expression that evaluates to a string (for dicts)
 * or a number (for lists).
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * INPUT["customer"]           // Dict access with string key
 * items[0]                    // List access with numeric index
 * items[-1]                   // Negative index (from end)
 * INPUT?["customer"]          // Safe access (returns null if INPUT is null)
 * INPUT?["items"]?[0]         // Chained safe access
 * }</pre>
 * 
 * @param object The expression being accessed (e.g., INPUT, items)
 * @param key    The key/index expression
 * @param safe   True if using safe navigation (?[), false for regular access ([)
 */
public record DictAccess(Expression object, Expression key, boolean safe) implements Expression {
    
    /**
     * Create a regular (non-safe) dictionary access.
     * 
     * <p>This constructor maintains backward compatibility with existing code.
     */
    public DictAccess(Expression object, Expression key) {
        this(object, key, false);
    }
    
    @Override
    public String toString() {
        return object + (safe ? "?[" : "[") + key + "]";
    }
}
