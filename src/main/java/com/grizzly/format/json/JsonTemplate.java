package com.grizzly.format.json;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import com.grizzly.core.types.DictValue;
import com.grizzly.core.types.ValueConverter;

import java.util.Map;
import java.util.Objects;

/**
 * JSON convenience wrapper for GrizzlyTemplate.
 * 
 * <p>This class wraps a core {@link GrizzlyTemplate} and provides
 * JSON string input/output methods for convenience.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Option 1: Create from engine
 * GrizzlyEngine engine = new GrizzlyEngine();
 * GrizzlyTemplate coreTemplate = engine.compile(templateCode);
 * JsonTemplate jsonTemplate = new JsonTemplate(coreTemplate);
 * 
 * // Option 2: Create directly
 * JsonTemplate jsonTemplate = JsonTemplate.compile(templateCode);
 * 
 * // Transform JSON
 * String jsonOutput = jsonTemplate.transform(jsonInput);
 * }</pre>
 * 
 * <h2>API Methods</h2>
 * <ul>
 *   <li>{@link #transform(String)} - JSON string → JSON string</li>
 *   <li>{@link #transformToMap(String)} - JSON string → Java Map</li>
 *   <li>{@link #transformFromMap(Map)} - Java Map → JSON string</li>
 * </ul>
 * 
 * @see GrizzlyTemplate The core format-agnostic template
 * @see JsonReader For JSON parsing
 * @see JsonWriter For JSON generation
 */
public class JsonTemplate {
    
    private final GrizzlyTemplate coreTemplate;
    private final JsonReader reader;
    private final JsonWriter writer;
    
    /**
     * Create a JsonTemplate wrapping a core template.
     * 
     * @param coreTemplate The core template to wrap
     * @throws NullPointerException if coreTemplate is null
     */
    public JsonTemplate(GrizzlyTemplate coreTemplate) {
        this.coreTemplate = Objects.requireNonNull(coreTemplate, "coreTemplate cannot be null");
        this.reader = new JsonReader();
        this.writer = new JsonWriter();
    }
    
    /**
     * Create a JsonTemplate wrapping a core template with custom reader/writer.
     * 
     * @param coreTemplate The core template to wrap
     * @param reader Custom JSON reader
     * @param writer Custom JSON writer
     */
    public JsonTemplate(GrizzlyTemplate coreTemplate, JsonReader reader, JsonWriter writer) {
        this.coreTemplate = Objects.requireNonNull(coreTemplate, "coreTemplate cannot be null");
        this.reader = Objects.requireNonNull(reader, "reader cannot be null");
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
    }
    
    /**
     * Compile template code and create a JsonTemplate directly.
     * 
     * <p>This is a convenience factory method that creates the engine,
     * compiles the template, and wraps it in a JsonTemplate.
     * 
     * @param templateCode The Python-like template code
     * @return A ready-to-use JsonTemplate
     */
    public static JsonTemplate compile(String templateCode) {
        GrizzlyEngine engine = new GrizzlyEngine(false);
        GrizzlyTemplate coreTemplate = engine.compile(templateCode);
        return new JsonTemplate(coreTemplate);
    }
    
    /**
     * Transform JSON input to JSON output.
     * 
     * <p>This is the primary method for JSON-to-JSON transformations.
     * 
     * @param jsonInput JSON input string
     * @return JSON output string (pretty-printed)
     * @throws NullPointerException if jsonInput is null
     * @throws com.grizzly.format.FormatException if JSON parsing fails
     * @throws com.grizzly.core.exception.GrizzlyExecutionException if transformation fails
     */
    public String transform(String jsonInput) {
        Objects.requireNonNull(jsonInput, "jsonInput cannot be null");
        DictValue input = reader.read(jsonInput);
        DictValue output = coreTemplate.execute(input);
        return writer.write(output);
    }
    
    /**
     * Transform JSON input to Java Map.
     * 
     * <p>Useful when you need to work with the result as a Map
     * before further processing.
     * 
     * @param jsonInput JSON input string
     * @return Output as a Java Map
     */
    public Map<String, Object> transformToMap(String jsonInput) {
        Objects.requireNonNull(jsonInput, "jsonInput cannot be null");
        DictValue input = reader.read(jsonInput);
        DictValue output = coreTemplate.execute(input);
        return ValueConverter.toJavaMap(output);
    }
    
    /**
     * Transform Java Map input to JSON output.
     * 
     * <p>Useful when you already have a Map (e.g., from another source).
     * 
     * @param inputMap Input data as a Map
     * @return JSON output string
     */
    public String transformFromMap(Map<String, Object> inputMap) {
        Objects.requireNonNull(inputMap, "inputMap cannot be null");
        DictValue input = ValueConverter.fromJavaMap(inputMap);
        DictValue output = coreTemplate.execute(input);
        return writer.write(output);
    }
    
    /**
     * Get the underlying core template.
     * 
     * @return The core GrizzlyTemplate
     */
    public GrizzlyTemplate getCoreTemplate() {
        return coreTemplate;
    }
    
    /**
     * Configure for compact (non-pretty-printed) JSON output.
     * 
     * @return A new JsonTemplate with compact output
     */
    public JsonTemplate compact() {
        return new JsonTemplate(coreTemplate, reader, new JsonWriter().compact());
    }
}
