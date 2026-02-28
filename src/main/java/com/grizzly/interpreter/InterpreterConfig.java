package com.grizzly.interpreter;

import java.time.Duration;

/**
 * Configuration for the Grizzly interpreter with production safeguards.
 * 
 * <p>Use this to prevent runaway templates from consuming resources:
 * <ul>
 *   <li>Max loop iterations - prevents infinite loops</li>
 *   <li>Max recursion depth - prevents stack overflow</li>
 *   <li>Execution timeout - prevents long-running transforms</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * // Use defaults (recommended for most cases)
 * InterpreterConfig config = InterpreterConfig.defaults();
 * 
 * // Strict limits for untrusted templates
 * InterpreterConfig strict = InterpreterConfig.builder()
 *     .maxLoopIterations(10_000)
 *     .maxRecursionDepth(50)
 *     .executionTimeout(Duration.ofSeconds(5))
 *     .build();
 * }</pre>
 */
public record InterpreterConfig(
    int maxLoopIterations,
    int maxRecursionDepth,
    Duration executionTimeout,
    boolean strictMode
) {
    
    /** Default max iterations per loop (1 million) */
    public static final int DEFAULT_MAX_LOOP_ITERATIONS = 1_000_000;
    
    /** Default max recursion depth (1000) */
    public static final int DEFAULT_MAX_RECURSION_DEPTH = 1000;
    
    /** Default execution timeout (30 seconds) */
    public static final Duration DEFAULT_EXECUTION_TIMEOUT = Duration.ofSeconds(30);
    
    /**
     * Create config with default production-safe values.
     */
    public static InterpreterConfig defaults() {
        return new InterpreterConfig(
            DEFAULT_MAX_LOOP_ITERATIONS,
            DEFAULT_MAX_RECURSION_DEPTH,
            DEFAULT_EXECUTION_TIMEOUT,
            false
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
     * Builder for InterpreterConfig.
     */
    public static class Builder {
        private int maxLoopIterations = DEFAULT_MAX_LOOP_ITERATIONS;
        private int maxRecursionDepth = DEFAULT_MAX_RECURSION_DEPTH;
        private Duration executionTimeout = DEFAULT_EXECUTION_TIMEOUT;
        private boolean strictMode = false;
        
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
        
        public Builder strictMode(boolean strict) {
            this.strictMode = strict;
            return this;
        }
        
        public InterpreterConfig build() {
            return new InterpreterConfig(
                maxLoopIterations,
                maxRecursionDepth,
                executionTimeout,
                strictMode
            );
        }
    }
}
