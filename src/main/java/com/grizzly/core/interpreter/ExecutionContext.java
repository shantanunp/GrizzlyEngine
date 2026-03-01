package com.grizzly.core.interpreter;

import com.grizzly.core.types.*;
import com.grizzly.core.validation.AccessTracker;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution context - holds variables and access tracking during template execution.
 * 
 * <p>Think of this like memory during program execution:
 * <ul>
 *   <li>INPUT: the input data (DictValue)</li>
 *   <li>OUTPUT: the output data being built (DictValue)</li>
 *   <li>Any other variables created during execution</li>
 *   <li>AccessTracker for recording property accesses</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>{@code
 * AccessTracker tracker = new AccessTracker(true);
 * ExecutionContext context = new ExecutionContext(tracker);
 * context.set("INPUT", inputData);
 * context.set("OUTPUT", DictValue.empty());
 * Value value = context.get("INPUT");
 * }</pre>
 */
public class ExecutionContext {
    
    private final Map<String, Value> variables = new HashMap<>();
    private final ExecutionContext parent;
    private final AccessTracker accessTracker;
    
    /**
     * Create a root context with access tracking.
     * 
     * @param accessTracker Tracker for recording property accesses
     */
    public ExecutionContext(AccessTracker accessTracker) {
        this.parent = null;
        this.accessTracker = accessTracker;
    }
    
    /**
     * Create a root context without tracking (backward compatible).
     */
    public ExecutionContext() {
        this(AccessTracker.disabled());
    }
    
    /**
     * Create a child context (inherits tracker from parent).
     * 
     * @param parent The parent context
     */
    public ExecutionContext(ExecutionContext parent) {
        this.parent = parent;
        this.accessTracker = parent.accessTracker;
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
     * 
     * <p>The child context inherits the AccessTracker from this context,
     * so all property accesses in child scopes are still tracked.
     */
    public ExecutionContext createChild() {
        return new ExecutionContext(this);
    }
    
    /**
     * Get the access tracker.
     * 
     * @return The AccessTracker for this context
     */
    public AccessTracker getAccessTracker() {
        return accessTracker;
    }
    
    /**
     * Get all variables (for debugging).
     */
    public Map<String, Value> getVariables() {
        return new HashMap<>(variables);
    }
}
