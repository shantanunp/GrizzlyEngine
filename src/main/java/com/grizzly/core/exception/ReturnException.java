package com.grizzly.core.exception;

import com.grizzly.core.types.Value;

/**
 * Exception thrown to implement return statement control flow.
 * When a return is executed (including inside switch/match, if, or for),
 * this is thrown so the enclosing function can return the value.
 */
public class ReturnException extends RuntimeException {
    private final Value value;

    public ReturnException(Value value) {
        super();
        this.value = value;
    }

    public Value getValue() {
        return value;
    }
}
