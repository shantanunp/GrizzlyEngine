package com.grizzly.core.interpreter;

import com.grizzly.core.validation.NullHandling;

import java.time.Duration;

/**
 * Configuration for the Grizzly interpreter with production safeguards.
 * 
 * <p>Use this to configure resource limits and null handling behavior:
 * <ul>
 *   <li>Max loop iterations - prevents infinite loops</li>
 *   <li>Max recursion depth - prevents stack overflow</li>
 *   <li>Execution timeout - prevents long-running transforms</li>
 *   <li>Null handling - controls behavior when accessing null properties</li>
 *   <li>Access tracking - enables validation reports</li>
 * </ul>
 * 
 * <h2>Null Handling Modes</h2>
 * 
 * <ul>
 *   <li><b>STRICT</b> - Throws exception on null access (use during development)</li>
 *   <li><b>SAFE</b> - Returns null, tracks all accesses (recommended for production)</li>
 *   <li><b>SILENT</b> - Returns null, no tracking (maximum performance)</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * // Default config (SAFE mode with tracking)
 * InterpreterConfig config = InterpreterConfig.defaults();
 * 
 * // Development mode (STRICT - fail fast on null)
 * InterpreterConfig dev = InterpreterConfig.builder()
 *     .nullHandling(NullHandling.STRICT)
 *     .build();
 * 
 * // Production mode with custom limits
 * InterpreterConfig prod = InterpreterConfig.builder()
 *     .nullHandling(NullHandling.SAFE)
 *     .trackAccess(true)
 *     .maxLoopIterations(10_000)
 *     .executionTimeout(Duration.ofSeconds(5))
 *     .build();
 * }</pre>
 */
public record InterpreterConfig(
    int maxLoopIterations,
    int maxRecursionDepth,
    Duration executionTimeout,
    boolean strictMode,
    NullHandling nullHandling,
    boolean trackAccess
) {
    
    /** Default max iterations per loop (1 million) */
    public static final int DEFAULT_MAX_LOOP_ITERATIONS = 1_000_000;
    
    /** Default max recursion depth (1000) */
    public static final int DEFAULT_MAX_RECURSION_DEPTH = 1000;
    
    /** Default execution timeout (30 seconds) */
    public static final Duration DEFAULT_EXECUTION_TIMEOUT = Duration.ofSeconds(30);
    
    /**
     * Create config with default production-safe values.
     * 
     * <p>Defaults:
     * <ul>
     *   <li>Null handling: SAFE (never crashes)</li>
     *   <li>Access tracking: enabled</li>
     *   <li>Max loop iterations: 1,000,000</li>
     *   <li>Max recursion depth: 1,000</li>
     *   <li>Execution timeout: 30 seconds</li>
     * </ul>
     */
    public static InterpreterConfig defaults() {
        return new InterpreterConfig(
            DEFAULT_MAX_LOOP_ITERATIONS,
            DEFAULT_MAX_RECURSION_DEPTH,
            DEFAULT_EXECUTION_TIMEOUT,
            false,
            NullHandling.SAFE,
            true
        );
    }
    
    /**
     * Create config with no limits (for trusted templates only).
     */
    public static InterpreterConfig unlimited() {
        return new InterpreterConfig(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Duration.ofDays(365),
            false,
            NullHandling.SAFE,
            true
        );
    }
    
    /**
     * Create config for development (STRICT mode).
     * 
     * <p>Throws exceptions on null access to catch issues early.
     */
    public static InterpreterConfig development() {
        return new InterpreterConfig(
            DEFAULT_MAX_LOOP_ITERATIONS,
            DEFAULT_MAX_RECURSION_DEPTH,
            DEFAULT_EXECUTION_TIMEOUT,
            true,
            NullHandling.STRICT,
            true
        );
    }
    
    /**
     * Create config for high-performance batch processing.
     * 
     * <p>Uses SILENT mode with no tracking for maximum speed.
     */
    public static InterpreterConfig highPerformance() {
        return new InterpreterConfig(
            DEFAULT_MAX_LOOP_ITERATIONS,
            DEFAULT_MAX_RECURSION_DEPTH,
            DEFAULT_EXECUTION_TIMEOUT,
            false,
            NullHandling.SILENT,
            false
        );
    }
    
    /**
     * Create a builder for custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Check if access tracking is enabled.
     * 
     * <p>Note: In SILENT mode, tracking is always disabled regardless of this setting.
     */
    public boolean isTrackingEnabled() {
        return trackAccess && nullHandling != NullHandling.SILENT;
    }
    
    /**
     * Builder for InterpreterConfig.
     */
    public static class Builder {
        private int maxLoopIterations = DEFAULT_MAX_LOOP_ITERATIONS;
        private int maxRecursionDepth = DEFAULT_MAX_RECURSION_DEPTH;
        private Duration executionTimeout = DEFAULT_EXECUTION_TIMEOUT;
        private boolean strictMode = false;
        private NullHandling nullHandling = NullHandling.SAFE;
        private boolean trackAccess = true;
        
        public Builder maxLoopIterations(int max) {
            if (max <= 0) {
                throw new IllegalArgumentException("maxLoopIterations must be positive");
            }
            this.maxLoopIterations = max;
            return this;
        }
        
        public Builder maxRecursionDepth(int max) {
            if (max <= 0) {
                throw new IllegalArgumentException("maxRecursionDepth must be positive");
            }
            this.maxRecursionDepth = max;
            return this;
        }
        
        public Builder executionTimeout(Duration timeout) {
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("executionTimeout must be positive");
            }
            this.executionTimeout = timeout;
            return this;
        }
        
        /**
         * Set the null handling mode.
         * 
         * @param handling How to handle null values during property access
         */
        public Builder nullHandling(NullHandling handling) {
            if (handling == null) {
                throw new IllegalArgumentException("nullHandling cannot be null");
            }
            this.nullHandling = handling;
            this.strictMode = (handling == NullHandling.STRICT);
            return this;
        }
        
        /**
         * Enable or disable access tracking.
         * 
         * <p>When enabled, all property accesses are recorded and can be
         * retrieved via {@link com.grizzly.core.validation.ValidationReport}.
         * 
         * <p>Note: Tracking is automatically disabled in SILENT mode.
         * 
         * @param track true to enable tracking, false to disable
         */
        public Builder trackAccess(boolean track) {
            this.trackAccess = track;
            return this;
        }
        
        public InterpreterConfig build() {
            return new InterpreterConfig(
                maxLoopIterations,
                maxRecursionDepth,
                executionTimeout,
                strictMode,
                nullHandling,
                trackAccess
            );
        }
    }
}
