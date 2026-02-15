package com.grizzly.interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution context - holds variables during template execution
 * 
 * Think of this like memory during program execution:
 * - INPUT: the input data
 * - OUTPUT: the output data being built
 * - Any other variables created during execution
 * 
 * Example:
 * context.set("INPUT", inputData);
 * context.set("OUTPUT", new HashMap<>());
 * Object value = context.get("INPUT");
 */
public class ExecutionContext {
    
    private final Map<String, Object> variables = new HashMap<>();
    private final ExecutionContext parent; // For nested scopes (future use)
    
    public ExecutionContext() {
        this.parent = null;
    }
    
    public ExecutionContext(ExecutionContext parent) {
        this.parent = parent;
    }
    
    /**
     * Set a variable
     */
    public void set(String name, Object value) {
        variables.put(name, value);
    }
    
    /**
     * Get a variable (searches parent scopes if not found)
     */
    public Object get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        return null;
    }
    
    /**
     * Check if variable exists
     */
    public boolean has(String name) {
        return variables.containsKey(name) || (parent != null && parent.has(name));
    }
    
    /**
     * Create a child context (for nested scopes)
     */
    public ExecutionContext createChild() {
        return new ExecutionContext(this);
    }
    
    /**
     * Get all variables (for debugging)
     */
    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }
}
