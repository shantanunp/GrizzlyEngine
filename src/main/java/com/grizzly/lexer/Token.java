package com.grizzly.lexer;

import java.util.Objects;

/**
 * Represents a single token in the Python template
 * Using Java Record (JDK 16+) for immutability and conciseness
 */
public record Token(TokenType type, String value, int line, int column) {

    public Token {
        Objects.requireNonNull(type, "type cannot be null");
    }

    public Token(TokenType type, int line, int column) {
        this(type, null, line, column);
    }

    @Override
    public String toString() {
        if (value != null) {
            return String.format("%s(%s) at %d:%d", type, value, line, column);
        }
        return String.format("%s at %d:%d", type, line, column);
    }
}
