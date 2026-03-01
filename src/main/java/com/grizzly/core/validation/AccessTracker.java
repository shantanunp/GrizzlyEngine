package com.grizzly.core.validation;

import com.grizzly.core.types.Value;
import com.grizzly.core.types.NullValue;
import com.grizzly.core.types.StringValue;
import com.grizzly.core.types.ListValue;
import com.grizzly.core.types.DictValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks all property accesses during template execution.
 * 
 * <p>This class maintains a log of every property access, recording whether
 * it succeeded, failed due to null, or failed due to missing keys. The log
 * can be used to generate a {@link ValidationReport}.
 * 
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * AccessTracker tracker = new AccessTracker(true);
 * // ... execution happens, tracker records accesses ...
 * ValidationReport report = tracker.generateReport();
 * }</pre>
 */
public class AccessTracker {
    
    private final List<AccessRecord> records = new ArrayList<>();
    private final boolean enabled;
    
    /**
     * Create a new AccessTracker.
     * 
     * @param enabled If false, all record methods are no-ops (for performance)
     */
    public AccessTracker(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Create a disabled tracker (no-op).
     */
    public static AccessTracker disabled() {
        return new AccessTracker(false);
    }
    
    /**
     * Record a successful property access.
     * 
     * @param path       Full path accessed
     * @param value      Value that was retrieved
     * @param lineNumber Source line number
     */
    public void recordSuccess(String path, Value value, int lineNumber) {
        if (!enabled) return;
        
        AccessStatus status = AccessStatus.SUCCESS;
        
        if (value instanceof NullValue) {
            status = AccessStatus.VALUE_NULL;
        } else if (isEmpty(value)) {
            status = AccessStatus.VALUE_EMPTY;
        }
        
        records.add(new AccessRecord(path, status, null, value, lineNumber, false));
    }
    
    /**
     * Record a broken path (null encountered in chain).
     * 
     * @param path         Full path that was attempted
     * @param brokenAt     Segment where null was encountered
     * @param lineNumber   Source line number
     * @param expectedNull True if ?. was used (null was expected)
     */
    public void recordPathBroken(String path, String brokenAt, int lineNumber, boolean expectedNull) {
        if (!enabled) return;
        
        AccessStatus status = expectedNull ? AccessStatus.EXPECTED_NULL : AccessStatus.PATH_BROKEN;
        records.add(new AccessRecord(path, status, brokenAt, null, lineNumber, expectedNull));
    }
    
    /**
     * Record a key not found in dictionary.
     * 
     * @param path         Full path that was attempted
     * @param key          Key that was not found
     * @param lineNumber   Source line number
     * @param expectedNull True if ?. was used
     */
    public void recordKeyNotFound(String path, String key, int lineNumber, boolean expectedNull) {
        if (!enabled) return;
        
        AccessStatus status = expectedNull ? AccessStatus.EXPECTED_NULL : AccessStatus.KEY_NOT_FOUND;
        records.add(new AccessRecord(path, status, key, null, lineNumber, expectedNull));
    }
    
    /**
     * Record an index out of bounds in list.
     * 
     * @param path         Full path that was attempted
     * @param index        Index that was out of bounds
     * @param listSize     Actual size of the list
     * @param lineNumber   Source line number
     * @param expectedNull True if ?. was used
     */
    public void recordIndexOutOfBounds(String path, int index, int listSize, int lineNumber, boolean expectedNull) {
        if (!enabled) return;
        
        AccessStatus status = expectedNull ? AccessStatus.EXPECTED_NULL : AccessStatus.INDEX_OUT_OF_BOUNDS;
        String brokenAt = "index " + index + " (size: " + listSize + ")";
        records.add(new AccessRecord(path, status, brokenAt, null, lineNumber, expectedNull));
    }
    
    /**
     * Get all recorded accesses.
     */
    public List<AccessRecord> getRecords() {
        return new ArrayList<>(records);
    }
    
    /**
     * Generate a validation report from all recorded accesses.
     */
    public ValidationReport generateReport() {
        return new ValidationReport(records);
    }
    
    /**
     * Check if tracking is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get the number of recorded accesses.
     */
    public int size() {
        return records.size();
    }
    
    /**
     * Clear all recorded accesses.
     */
    public void clear() {
        records.clear();
    }
    
    private boolean isEmpty(Value value) {
        if (value instanceof StringValue sv) {
            return sv.value().isEmpty();
        }
        if (value instanceof ListValue lv) {
            return lv.isEmpty();
        }
        if (value instanceof DictValue dv) {
            return dv.isEmpty();
        }
        return false;
    }
}
