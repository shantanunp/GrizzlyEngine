package com.grizzly.format;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for format readers and writers.
 * 
 * <p>This class provides a central place to register and lookup format handlers,
 * enabling pluggable format support for the Grizzly engine.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get the default registry (JSON pre-registered)
 * FormatRegistry registry = FormatRegistry.defaultRegistry();
 * 
 * // Read JSON
 * DictValue data = registry.getReader("json").read(jsonString);
 * 
 * // Write JSON
 * String json = registry.getWriter("json").write(data);
 * }</pre>
 * 
 * <h2>Registering Custom Formats</h2>
 * <pre>{@code
 * FormatRegistry registry = FormatRegistry.defaultRegistry();
 * registry.registerReader("yaml", new YamlReader());
 * registry.registerWriter("yaml", new YamlWriter());
 * }</pre>
 * 
 * @see FormatReader For reading formats
 * @see FormatWriter For writing formats
 */
public class FormatRegistry {
    
    private final Map<String, FormatReader> readers = new ConcurrentHashMap<>();
    private final Map<String, FormatWriter> writers = new ConcurrentHashMap<>();
    
    private static volatile FormatRegistry defaultInstance;
    
    /**
     * Create an empty registry.
     */
    public FormatRegistry() {
    }
    
    /**
     * Get the default registry with JSON format pre-registered.
     * 
     * <p>This method is thread-safe and returns a singleton instance.
     * 
     * @return The default registry
     */
    public static FormatRegistry defaultRegistry() {
        if (defaultInstance == null) {
            synchronized (FormatRegistry.class) {
                if (defaultInstance == null) {
                    defaultInstance = createDefaultRegistry();
                }
            }
        }
        return defaultInstance;
    }
    
    /**
     * Create a new default registry (for testing or custom instances).
     * 
     * @return A new registry with JSON format registered
     */
    public static FormatRegistry createDefaultRegistry() {
        FormatRegistry registry = new FormatRegistry();
        
        // Register JSON handlers
        registry.registerReader("json", new com.grizzly.format.json.JsonReader());
        registry.registerWriter("json", new com.grizzly.format.json.JsonWriter());
        
        return registry;
    }
    
    /**
     * Register a format reader.
     * 
     * @param format The format name (case-insensitive, stored lowercase)
     * @param reader The reader implementation
     * @throws NullPointerException if format or reader is null
     */
    public void registerReader(String format, FormatReader reader) {
        Objects.requireNonNull(format, "format cannot be null");
        Objects.requireNonNull(reader, "reader cannot be null");
        readers.put(format.toLowerCase(), reader);
    }
    
    /**
     * Register a format writer.
     * 
     * @param format The format name (case-insensitive, stored lowercase)
     * @param writer The writer implementation
     * @throws NullPointerException if format or writer is null
     */
    public void registerWriter(String format, FormatWriter writer) {
        Objects.requireNonNull(format, "format cannot be null");
        Objects.requireNonNull(writer, "writer cannot be null");
        writers.put(format.toLowerCase(), writer);
    }
    
    /**
     * Get a reader for the specified format.
     * 
     * @param format The format name (case-insensitive)
     * @return The reader
     * @throws FormatException if no reader is registered for this format
     */
    public FormatReader getReader(String format) {
        FormatReader reader = readers.get(format.toLowerCase());
        if (reader == null) {
            throw new FormatException(format, "No reader registered for format: " + format + 
                ". Available formats: " + readers.keySet());
        }
        return reader;
    }
    
    /**
     * Get a writer for the specified format.
     * 
     * @param format The format name (case-insensitive)
     * @return The writer
     * @throws FormatException if no writer is registered for this format
     */
    public FormatWriter getWriter(String format) {
        FormatWriter writer = writers.get(format.toLowerCase());
        if (writer == null) {
            throw new FormatException(format, "No writer registered for format: " + format + 
                ". Available formats: " + writers.keySet());
        }
        return writer;
    }
    
    /**
     * Check if a reader is registered for the specified format.
     * 
     * @param format The format name (case-insensitive)
     * @return true if a reader is registered
     */
    public boolean hasReader(String format) {
        return readers.containsKey(format.toLowerCase());
    }
    
    /**
     * Check if a writer is registered for the specified format.
     * 
     * @param format The format name (case-insensitive)
     * @return true if a writer is registered
     */
    public boolean hasWriter(String format) {
        return writers.containsKey(format.toLowerCase());
    }
    
    /**
     * Get all registered reader format names.
     * 
     * @return Set of format names
     */
    public Set<String> getReaderFormats() {
        return Set.copyOf(readers.keySet());
    }
    
    /**
     * Get all registered writer format names.
     * 
     * @return Set of format names
     */
    public Set<String> getWriterFormats() {
        return Set.copyOf(writers.keySet());
    }
}
