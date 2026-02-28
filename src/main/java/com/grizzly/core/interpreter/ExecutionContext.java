package com.grizzly.core.interpreter;

import com.grizzly.core.types.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution context - holds variables during template execution.
 * 
 * <p>Think of this like memory during program execution:
 * <ul>
 *   <li>INPUT: the input data (DictValue)</li>
 *   <li>OUTPUT: the output data being built (DictValue)</li>
 *   <li>Any other variables created during execution</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>{@code
 * context.set("INPUT", inputData);
 * context.set("OUTPUT", DictValue.empty());
 * Value value = context.get("INPUT");
 * }</pre>
 */
public class ExecutionContext {
    
    private final Map<String, Value> variables = new HashMap<>();
    private final ExecutionContext parent;
    
    public ExecutionContext() {
        this.parent = null;
    }
    
    public ExecutionContext(ExecutionContext parent) {
        this.parent = parent;
    }
    
    /**
     * Set a variable.
     * 
     * @param name Variable name
     * @param value The value to store
     */
    public void set(String name, Value value) {
        variables.put(name, value);
    }
    
    /**
     * Get a variable (searches parent scopes if not found).
     * 
     * @param name Variable name
     * @return The value, or NullValue if not found
     */
    public Value get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        return NullValue.INSTANCE;
    }
    
    /**
     * Get a variable, returning null if not found (for internal checks).
     */
    public Value getOrNull(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.getOrNull(name);
        }
        return null;
    }
    
    /**
     * Check if variable exists.
     */
    public boolean has(String name) {
        return variables.containsKey(name) || (parent != null && parent.has(name));
    }
    
    /**
     * Create a child context (for nested scopes).
     */
    public ExecutionContext createChild() {
        return new ExecutionContext(this);
    }
    
    /**
     * Get all variables (for debugging).
     */
    public Map<String, Value> getVariables() {
        return new HashMap<>(variables);
    }
}
