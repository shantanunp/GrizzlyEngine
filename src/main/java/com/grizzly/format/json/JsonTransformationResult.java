package com.grizzly.format.json;

import com.grizzly.core.validation.ValidationReport;
import com.grizzly.core.types.DictValue;
import com.grizzly.core.types.ValueConverter;

import java.util.Map;
import java.util.Objects;

/**
 * Result of a JSON transformation with validation information.
 * 
 * <p>This record provides both the JSON output and a validation report
 * showing all property accesses during the transformation.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * JsonTransformationResult result = jsonTemplate.transformWithValidation(inputJson);
 * 
 * // Get output
 * String outputJson = result.outputJson();
 * 
 * // Check for errors
 * if (result.hasErrors()) {
 *     ValidationReport report = result.validationReport();
 *     
 *     for (AccessRecord error : report.getPathErrors()) {
 *         log.warn("Line {}: {} broken at '{}'",
 *             error.lineNumber(), error.fullPath(), error.brokenAtSegment());
 *     }
 * }
 * 
 * // Performance info
 * log.info("Transform took {} ms", result.executionTimeMs());
 * }</pre>
 * 
 * @param outputJson       The transformed JSON output string
 * @param outputDict       The transformed output as DictValue
 * @param validationReport Report of all property accesses
 * @param executionTimeMs  Time taken to execute (milliseconds)
 */
public record JsonTransformationResult(
    String outputJson,
    DictValue outputDict,
    ValidationReport validationReport,
    long executionTimeMs
) {
    
    public JsonTransformationResult {
        Objects.requireNonNull(outputJson, "outputJson cannot be null");
        Objects.requireNonNull(outputDict, "outputDict cannot be null");
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
     * Get the output as a Java Map.
     */
    public Map<String, Object> outputMap() {
        return ValueConverter.toJavaMap(outputDict);
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
