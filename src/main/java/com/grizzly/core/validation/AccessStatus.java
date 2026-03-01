package com.grizzly.core.validation;

/**
 * Status of a property access attempt during transformation.
 * 
 * <p>Used in {@link AccessRecord} to indicate the result of accessing
 * a property path like {@code INPUT.deal.loan.city}.
 */
public enum AccessStatus {
    
    /**
     * Path resolved successfully and value was retrieved.
     */
    SUCCESS,
    
    /**
     * Path resolved successfully, but the final value was null.
     * 
     * <p>Example: {@code INPUT.customer.middleName} where middleName exists but is null.
     */
    VALUE_NULL,
    
    /**
     * Path resolved successfully, but value was empty (empty string, list, or dict).
     */
    VALUE_EMPTY,
    
    /**
     * Path could not be resolved because null was encountered in the chain.
     * 
     * <p>Example: {@code INPUT.deal.loan.city} where {@code loan} is null.
     * The access never reached {@code city}.
     */
    PATH_BROKEN,
    
    /**
     * Path resolved to a dictionary, but the requested key doesn't exist.
     * 
     * <p>Example: {@code INPUT["nonexistent"]} where key doesn't exist in INPUT.
     */
    KEY_NOT_FOUND,
    
    /**
     * Path resolved to a list, but the index was out of bounds.
     * 
     * <p>Example: {@code INPUT.items[10]} where items only has 3 elements.
     */
    INDEX_OUT_OF_BOUNDS,
    
    /**
     * Access used safe navigation ({@code ?.}) and encountered null.
     * 
     * <p>This is different from PATH_BROKEN because the null was expected
     * by the template author. Use this to distinguish intentional vs
     * unintentional null results.
     */
    EXPECTED_NULL
}
