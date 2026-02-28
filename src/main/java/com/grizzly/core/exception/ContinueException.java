package com.grizzly.core.exception;

/**
 * Exception thrown to implement continue statement control flow.
 * Caught by the enclosing loop.
 */
public class ContinueException extends RuntimeException {
    public ContinueException() {
        super();
    }
}
