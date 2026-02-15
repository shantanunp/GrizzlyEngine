package com.grizzly.exception;

/**
 * Thrown when template parsing fails
 */
public class GrizzlyParseException extends RuntimeException {
    private final Integer line;
    private final Integer column;
    
    public GrizzlyParseException(String message) {
        super(message);
        this.line = null;
        this.column = null;
    }
    
    public GrizzlyParseException(String message, int line, int column) {
        super(String.format("%s at line %d, column %d", message, line, column));
        this.line = line;
        this.column = column;
    }
    
    public GrizzlyParseException(String message, Throwable cause) {
        super(message, cause);
        this.line = null;
        this.column = null;
    }
    
    public Integer getLine() {
        return line;
    }
    
    public Integer getColumn() {
        return column;
    }
}
