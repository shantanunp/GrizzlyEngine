package com.grizzly.core.exception;

/**
 * Exception thrown to implement break statement control flow.
 * Caught by the enclosing loop.
 */
public class BreakException extends RuntimeException {
    public BreakException() {
        super();
    }
}
