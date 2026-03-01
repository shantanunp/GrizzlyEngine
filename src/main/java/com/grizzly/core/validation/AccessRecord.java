package com.grizzly.core.validation;

import com.grizzly.core.types.Value;

/**
 * Record of a single property access during transformation.
 * 
 * <p>Each time a property is accessed (e.g., {@code INPUT.deal.loan.city}),
 * an AccessRecord is created to track the result. This enables detailed
 * validation reports.
 * 
 * <h2>Example Records</h2>
 * 
 * <pre>{@code
 * // Successful access
 * AccessRecord(
 *     fullPath: "INPUT.customer.name",
 *     status: SUCCESS,
 *     brokenAtSegment: null,
 *     retrievedValue: StringValue("John"),
 *     lineNumber: 5,
 *     expectedNull: false
 * )
 * 
 * // Broken path (loan was null)
 * AccessRecord(
 *     fullPath: "INPUT.deal.loan.city",
 *     status: PATH_BROKEN,
 *     brokenAtSegment: "loan",
 *     retrievedValue: null,
 *     lineNumber: 8,
 *     expectedNull: false
 * )
 * 
 * // Safe navigation (expected null)
 * AccessRecord(
 *     fullPath: "INPUT?.deal?.loan?.city",
 *     status: EXPECTED_NULL,
 *     brokenAtSegment: "loan",
 *     retrievedValue: null,
 *     lineNumber: 8,
 *     expectedNull: true
 * )
 * }</pre>
 * 
 * @param fullPath        Full access path (e.g., "INPUT.deal.loan.city")
 * @param status          Result of the access attempt
 * @param brokenAtSegment Segment where path broke (null if SUCCESS)
 * @param retrievedValue  Value retrieved (null if not SUCCESS)
 * @param lineNumber      Source line number where access occurred
 * @param expectedNull    True if access used ?. operator (null was expected)
 */
public record AccessRecord(
    String fullPath,
    AccessStatus status,
    String brokenAtSegment,
    Value retrievedValue,
    int lineNumber,
    boolean expectedNull
) {
    
    /**
     * Check if this access was successful.
     */
    public boolean isSuccess() {
        return status == AccessStatus.SUCCESS;
    }
    
    /**
     * Check if this access represents an error condition.
     * 
     * <p>Note: EXPECTED_NULL is NOT considered an error because the template
     * author explicitly used {@code ?.} to indicate null was acceptable.
     */
    public boolean isError() {
        return status == AccessStatus.PATH_BROKEN || 
               status == AccessStatus.KEY_NOT_FOUND ||
               status == AccessStatus.INDEX_OUT_OF_BOUNDS;
    }
    
    /**
     * Check if null was encountered (either expected or unexpected).
     */
    public boolean isNull() {
        return status == AccessStatus.PATH_BROKEN ||
               status == AccessStatus.VALUE_NULL ||
               status == AccessStatus.EXPECTED_NULL;
    }
    
    @Override
    public String toString() {
        return String.format("AccessRecord[path=%s, status=%s, brokenAt=%s, line=%d]",
            fullPath, status, brokenAtSegment, lineNumber);
    }
}
