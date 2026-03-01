package com.grizzly.core.parser.ast;

/**
 * Attribute access expression: {@code object.attr} or {@code object?.attr}
 * 
 * <p>Represents accessing a named attribute (field) of an object.
 * In Grizzly templates, this is typically used to access dictionary keys
 * using dot notation.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * INPUT.customer              // Regular access (may throw if INPUT is null)
 * INPUT.customer.name         // Chained access
 * INPUT?.customer?.name       // Safe access (returns null if any part is null)
 * }</pre>
 * 
 * @param object The expression being accessed (e.g., INPUT)
 * @param attr   The attribute name (e.g., "customer")
 * @param safe   True if using safe navigation (?.), false for regular access (.)
 */
public record AttrAccess(Expression object, String attr, boolean safe) implements Expression {
    
    /**
     * Create a regular (non-safe) attribute access.
     * 
     * <p>This constructor maintains backward compatibility with existing code.
     */
    public AttrAccess(Expression object, String attr) {
        this(object, attr, false);
    }
    
    @Override
    public String toString() {
        return object + (safe ? "?." : ".") + attr;
    }
}
