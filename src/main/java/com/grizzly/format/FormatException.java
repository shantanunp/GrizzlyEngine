package com.grizzly.format;

/**
 * Exception thrown when format reading or writing fails.
 * 
 * <p>This exception wraps underlying parsing or serialization errors
 * from format-specific libraries (Jackson, etc.) into a common type.
 */
public class FormatException extends RuntimeException {
    
    private final String format;
    
    /**
     * Create a new FormatException.
     * 
     * @param format The format that failed (e.g., "json", "xml")
     * @param message The error message
     */
    public FormatException(String format, String message) {
        super(String.format("[%s] %s", format.toUpperCase(), message));
        this.format = format;
    }
    
    /**
     * Create a new FormatException with a cause.
     * 
     * @param format The format that failed (e.g., "json", "xml")
     * @param message The error message
     * @param cause The underlying cause
     */
    public FormatException(String format, String message, Throwable cause) {
        super(String.format("[%s] %s", format.toUpperCase(), message), cause);
        this.format = format;
    }
    
    /**
     * Get the format that failed.
     * 
     * @return The format name
     */
    public String getFormat() {
        return format;
    }
}
