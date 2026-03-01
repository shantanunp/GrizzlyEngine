package com.grizzly.core.validation;

import com.grizzly.core.types.DictValue;

import java.util.Objects;

/**
 * Result of a transformation with validation information.
 * 
 * <p>This record contains:
 * <ul>
 *   <li>The transformed output data</li>
 *   <li>A validation report with all property access information</li>
 *   <li>Execution time for performance monitoring</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * TransformationResult result = template.executeWithValidation(input);
 * 
 * // Get output
 * DictValue output = result.output();
 * 
 * // Check for errors
 * if (result.hasErrors()) {
 *     ValidationReport report = result.validationReport();
 *     // Handle errors...
 * }
 * 
 * // Performance info
 * log.info("Transformation took {} ms", result.executionTimeMs());
 * }</pre>
 * 
 * @param output           The transformed output data
 * @param validationReport Report of all property accesses
 * @param executionTimeMs  Time taken to execute (milliseconds)
 */
public record TransformationResult(
    DictValue output,
    ValidationReport validationReport,
    long executionTimeMs
) {
    
    public TransformationResult {
        Objects.requireNonNull(output, "output cannot be null");
        Objects.requireNonNull(validationReport, "validationReport cannot be null");
    }
    
    /**
     * Check if there are any errors in the validation report.
     */
    public boolean hasErrors() {
        return validationReport.hasAnyErrors();
    }
    
    /**
     * Check if the transformation was clean (no errors).
     */
    public boolean isClean() {
        return validationReport.isClean();
    }
    
    /**
     * Check if any path errors occurred.
     */
    public boolean hasPathErrors() {
        return validationReport.hasPathErrors();
    }
    
    /**
     * Get the number of successful accesses.
     */
    public int successCount() {
        return validationReport.getSuccessful().size();
    }
    
    /**
     * Get the total number of errors.
     */
    public int errorCount() {
        return validationReport.getAllErrors().size();
    }
}
