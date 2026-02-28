package com.grizzly.core.exception;

/**
 * Thrown when template execution fails
 */
public class GrizzlyExecutionException extends RuntimeException {
    private final String pythonStack;
    private final Integer line;
    
    public GrizzlyExecutionException(String message) {
        super(message);
        this.pythonStack = null;
        this.line = null;
    }
    
    public GrizzlyExecutionException(String message, int line) {
        super(String.format("%s at line %d", message, line));
        this.line = line;
        this.pythonStack = null;
    }
    
    public GrizzlyExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.pythonStack = null;
        this.line = null;
    }
    
    public GrizzlyExecutionException(String message, String pythonStack, int line) {
        super(String.format("%s at line %d\nPython stack:\n%s", message, line, pythonStack));
        this.line = line;
        this.pythonStack = pythonStack;
    }
    
    public String getPythonStack() {
        return pythonStack;
    }
    
    public Integer getLine() {
        return line;
    }
}
