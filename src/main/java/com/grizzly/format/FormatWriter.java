package com.grizzly.format;

import com.grizzly.core.types.DictValue;

import java.io.OutputStream;

/**
 * Interface for writing a DictValue to a specific format.
 * 
 * <p>Implementations convert the internal type-safe {@link DictValue}
 * representation into external formats (JSON, XML, YAML, etc.).
 * 
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class JsonWriter implements FormatWriter {
 *     public String write(DictValue value) {
 *         Map<String, Object> map = ValueConverter.toJavaMap(value);
 *         return objectMapper.writeValueAsString(map);
 *     }
 * }
 * }</pre>
 * 
 * @see FormatReader For converting external formats to DictValue
 * @see FormatRegistry For registering and looking up format handlers
 */
public interface FormatWriter {
    
    /**
     * Write a DictValue to a string in this format.
     * 
     * @param value The DictValue to write
     * @return The formatted string (JSON, XML, etc.)
     * @throws FormatException if writing fails
     */
    String write(DictValue value);
    
    /**
     * Write a DictValue to an OutputStream in this format.
     * 
     * @param value The DictValue to write
     * @param stream The output stream to write to
     * @throws FormatException if writing fails
     */
    void write(DictValue value, OutputStream stream);
    
    /**
     * Get the name of this format (e.g., "json", "xml").
     * 
     * @return The format name in lowercase
     */
    String formatName();
}
