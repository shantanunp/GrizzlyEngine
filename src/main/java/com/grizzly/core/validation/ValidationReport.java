package com.grizzly.core.validation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validation report summarizing all property accesses during transformation.
 * 
 * <p>This report helps answer questions like:
 * <ul>
 *   <li>Why is a field null in the output?</li>
 *   <li>Which paths failed due to null in the input?</li>
 *   <li>Which null values were expected vs unexpected?</li>
 *   <li>How many accesses succeeded vs failed?</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * ValidationReport report = result.validationReport();
 * 
 * // Check for errors
 * if (report.hasPathErrors()) {
 *     for (AccessRecord error : report.getPathErrors()) {
 *         log.warn("Line {}: {} broken at '{}'",
 *             error.lineNumber(), error.fullPath(), error.brokenAtSegment());
 *     }
 * }
 * 
 * // Get JSON summary for logging
 * log.info("Validation: {}", report.toJson());
 * }</pre>
 */
public class ValidationReport {
    
    private final List<AccessRecord> allRecords;
    
    /**
     * Create a validation report from access records.
     * 
     * @param records The list of access records (must not be null)
     * @throws NullPointerException if records is null
     */
    public ValidationReport(List<AccessRecord> records) {
        java.util.Objects.requireNonNull(records, "records cannot be null");
        this.allRecords = new ArrayList<>(records);
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Get all accesses where the path was broken (null in chain).
     * 
     * <p>These are unexpected nulls - the template did not use {@code ?.}
     */
    public List<AccessRecord> getPathErrors() {
        return filter(AccessStatus.PATH_BROKEN);
    }
    
    /**
     * Get all accesses where a key was not found in a dictionary.
     */
    public List<AccessRecord> getKeyNotFoundErrors() {
        return filter(AccessStatus.KEY_NOT_FOUND);
    }
    
    /**
     * Get all accesses where an index was out of bounds.
     */
    public List<AccessRecord> getIndexErrors() {
        return filter(AccessStatus.INDEX_OUT_OF_BOUNDS);
    }
    
    /**
     * Get all accesses where the path resolved but value was null.
     */
    public List<AccessRecord> getNullValues() {
        return filter(AccessStatus.VALUE_NULL);
    }
    
    /**
     * Get all accesses where the path resolved but value was empty.
     */
    public List<AccessRecord> getEmptyValues() {
        return filter(AccessStatus.VALUE_EMPTY);
    }
    
    /**
     * Get all accesses that used safe navigation ({@code ?.}) and encountered null.
     * 
     * <p>These are expected nulls - the template author anticipated them.
     */
    public List<AccessRecord> getExpectedNulls() {
        return filter(AccessStatus.EXPECTED_NULL);
    }
    
    /**
     * Get all successful accesses.
     */
    public List<AccessRecord> getSuccessful() {
        return filter(AccessStatus.SUCCESS);
    }
    
    /**
     * Get all error accesses (PATH_BROKEN, KEY_NOT_FOUND, INDEX_OUT_OF_BOUNDS).
     * 
     * <p>Note: EXPECTED_NULL is not included as it's not an error.
     */
    public List<AccessRecord> getAllErrors() {
        return allRecords.stream()
            .filter(AccessRecord::isError)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all records.
     */
    public List<AccessRecord> getAllRecords() {
        return new ArrayList<>(allRecords);
    }
    
    // ==================== Predicates ====================
    
    /**
     * Check if there are any path errors (broken paths without safe navigation).
     */
    public boolean hasPathErrors() {
        return !getPathErrors().isEmpty();
    }
    
    /**
     * Check if there are any key-not-found errors.
     */
    public boolean hasKeyNotFoundErrors() {
        return !getKeyNotFoundErrors().isEmpty();
    }
    
    /**
     * Check if there are any errors of any type.
     */
    public boolean hasAnyErrors() {
        return allRecords.stream().anyMatch(AccessRecord::isError);
    }
    
    /**
     * Check if all accesses were successful (no errors).
     */
    public boolean isClean() {
        return !hasAnyErrors();
    }
    
    /**
     * Check if any null values were encountered (expected or unexpected).
     */
    public boolean hasAnyNulls() {
        return allRecords.stream().anyMatch(AccessRecord::isNull);
    }
    
    // ==================== Summary ====================
    
    /**
     * Get a summary of all access results.
     * 
     * @return Map with counts for total, successful, pathErrors, etc.
     */
    public Map<String, Integer> getSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("total", allRecords.size());
        summary.put("successful", getSuccessful().size());
        summary.put("pathErrors", getPathErrors().size());
        summary.put("keyNotFound", getKeyNotFoundErrors().size());
        summary.put("indexErrors", getIndexErrors().size());
        summary.put("nullValues", getNullValues().size());
        summary.put("emptyValues", getEmptyValues().size());
        summary.put("expectedNulls", getExpectedNulls().size());
        return summary;
    }
    
    // ==================== Grouping ====================
    
    /**
     * Group path errors by the segment where they broke.
     * 
     * <p>Useful for identifying which null fields in the input caused the most issues.
     * 
     * @return Map from segment name to list of errors
     */
    public Map<String, List<AccessRecord>> groupByBrokenSegment() {
        return getPathErrors().stream()
            .filter(r -> r.brokenAtSegment() != null)
            .collect(Collectors.groupingBy(AccessRecord::brokenAtSegment));
    }
    
    /**
     * Group all errors by line number.
     * 
     * @return Map from line number to list of errors
     */
    public Map<Integer, List<AccessRecord>> groupByLineNumber() {
        return getAllErrors().stream()
            .collect(Collectors.groupingBy(AccessRecord::lineNumber));
    }
    
    // ==================== Output ====================
    
    /**
     * Generate a JSON representation of the validation report.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"summary\": ").append(summaryToJson()).append(",\n");
        sb.append("  \"pathErrors\": ").append(recordsToJson(getPathErrors())).append(",\n");
        sb.append("  \"keyNotFound\": ").append(recordsToJson(getKeyNotFoundErrors())).append(",\n");
        sb.append("  \"indexErrors\": ").append(recordsToJson(getIndexErrors())).append(",\n");
        sb.append("  \"nullValues\": ").append(recordsToJson(getNullValues())).append(",\n");
        sb.append("  \"expectedNulls\": ").append(recordsToJson(getExpectedNulls())).append("\n");
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Generate a concise string representation.
     */
    @Override
    public String toString() {
        Map<String, Integer> summary = getSummary();
        return String.format("ValidationReport[total=%d, successful=%d, errors=%d, expectedNulls=%d]",
            summary.get("total"),
            summary.get("successful"),
            getAllErrors().size(),
            summary.get("expectedNulls"));
    }
    
    // ==================== Private Helpers ====================
    
    private List<AccessRecord> filter(AccessStatus status) {
        return allRecords.stream()
            .filter(r -> r.status() == status)
            .collect(Collectors.toList());
    }
    
    private String summaryToJson() {
        Map<String, Integer> summary = getSummary();
        return summary.entrySet().stream()
            .map(e -> "\"" + e.getKey() + "\": " + e.getValue())
            .collect(Collectors.joining(", ", "{", "}"));
    }
    
    private String recordsToJson(List<AccessRecord> records) {
        if (records.isEmpty()) return "[]";
        return records.stream()
            .map(this::recordToJson)
            .collect(Collectors.joining(",\n    ", "[\n    ", "\n  ]"));
    }
    
    private String recordToJson(AccessRecord r) {
        return String.format("{\"path\": \"%s\", \"brokenAt\": %s, \"line\": %d, \"expected\": %s}",
            escapeJson(r.fullPath()),
            r.brokenAtSegment() != null ? "\"" + escapeJson(r.brokenAtSegment()) + "\"" : "null",
            r.lineNumber(),
            r.expectedNull());
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
