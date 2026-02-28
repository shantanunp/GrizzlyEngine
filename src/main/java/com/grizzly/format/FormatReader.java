package com.grizzly.format;

import com.grizzly.types.DictValue;

import java.io.InputStream;

/**
 * Interface for reading data from a specific format into a DictValue.
 * 
 * <p>Implementations convert external formats (JSON, XML, YAML, etc.) into
 * the internal type-safe {@link DictValue} representation used by the
 * Grizzly interpreter.
 * 
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class JsonReader implements FormatReader {
 *     public DictValue read(String content) {
 *         Map<String, Object> map = objectMapper.readValue(content, ...);
 *         return ValueConverter.fromJavaMap(map);
 *     }
 * }
 * }</pre>
 * 
 * @see FormatWriter For converting DictValue back to external formats
 * @see FormatRegistry For registering and looking up format handlers
 */
public interface FormatReader {
    
    /**
     * Read content from a string and convert to a DictValue.
     * 
     * @param content The content string (JSON, XML, etc.)
     * @return The parsed DictValue
     * @throws FormatException if parsing fails
     */
    DictValue read(String content);
    
    /**
     * Read content from an InputStream and convert to a DictValue.
     * 
     * @param stream The input stream to read from
     * @return The parsed DictValue
     * @throws FormatException if parsing fails
     */
    DictValue read(InputStream stream);
    
    /**
     * Get the name of this format (e.g., "json", "xml").
     * 
     * @return The format name in lowercase
     */
    String formatName();
}
