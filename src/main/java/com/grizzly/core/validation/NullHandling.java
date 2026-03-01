package com.grizzly.core.validation;

/**
 * Strategies for handling null values during property access.
 * 
 * <p>Choose the appropriate mode based on your use case:
 * 
 * <table border="1">
 *   <tr><th>Mode</th><th>On Null</th><th>Tracking</th><th>Use Case</th></tr>
 *   <tr><td>STRICT</td><td>Throws exception</td><td>Optional</td><td>Development</td></tr>
 *   <tr><td>SAFE</td><td>Returns null</td><td>Yes</td><td>Production</td></tr>
 *   <tr><td>SILENT</td><td>Returns null</td><td>No</td><td>High-volume</td></tr>
 * </table>
 */
public enum NullHandling {
    
    /**
     * STRICT mode - Throw exception on null access.
     * 
     * <p>Use during development to catch data issues early.
     * The safe navigation operator ({@code ?.}) can still be used for
     * explicit opt-in to safe access.
     * 
     * <pre>{@code
     * INPUT.deal.loan.city      // throws if 'deal' or 'loan' is null
     * INPUT?.deal?.loan?.city   // returns null safely (explicit opt-in)
     * }</pre>
     */
    STRICT,
    
    /**
     * SAFE mode - Never crash, track all accesses.
     * 
     * <p>Recommended for production. All property accesses are safe by default.
     * The safe navigation operator marks accesses as "expected nullable" in reports.
     * 
     * <pre>{@code
     * INPUT.deal.loan.city      // returns null if broken, logs PATH_BROKEN
     * INPUT?.deal?.loan?.city   // returns null if broken, logs EXPECTED_NULL
     * }</pre>
     */
    SAFE,
    
    /**
     * SILENT mode - Never crash, no tracking.
     * 
     * <p>Maximum performance for high-volume processing where validation
     * reports are not needed.
     * 
     * <pre>{@code
     * INPUT.deal.loan.city      // returns null if broken, no logging
     * }</pre>
     */
    SILENT
}
