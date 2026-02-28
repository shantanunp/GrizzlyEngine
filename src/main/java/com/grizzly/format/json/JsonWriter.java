package com.grizzly.format.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.grizzly.format.FormatException;
import com.grizzly.format.FormatWriter;
import com.grizzly.types.DictValue;
import com.grizzly.types.ValueConverter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Writes a DictValue to JSON format.
 * 
 * <p>This writer uses Jackson to serialize DictValue to JSON,
 * converting from the internal type-safe representation to
 * standard Java types first.
 * 
 * <h2>Output Formatting</h2>
 * <p>By default, output is pretty-printed for readability.
 * Use {@link #compact()} for minified output.
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * JsonWriter writer = new JsonWriter();
 * 
 * DictValue data = DictValue.empty();
 * data.put("name", new StringValue("John"));
 * data.put("age", new NumberValue(30));
 * 
 * String json = writer.write(data);
 * // {
 * //   "name": "John",
 * //   "age": 30
 * // }
 * }</pre>
 */
public class JsonWriter implements FormatWriter {
    
    private final ObjectMapper mapper;
    private boolean prettyPrint = true;
    
    /**
     * Create a JsonWriter with default settings (pretty-printed output).
     */
    public JsonWriter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Create a JsonWriter with a custom ObjectMapper.
     * 
     * @param mapper The ObjectMapper to use
     */
    public JsonWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    
    /**
     * Configure for compact (minified) output.
     * 
     * @return this writer for chaining
     */
    public JsonWriter compact() {
        this.prettyPrint = false;
        this.mapper.disable(SerializationFeature.INDENT_OUTPUT);
        return this;
    }
    
    /**
     * Configure for pretty-printed output.
     * 
     * @return this writer for chaining
     */
    public JsonWriter prettyPrint() {
        this.prettyPrint = true;
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return this;
    }
    
    @Override
    public String write(DictValue value) {
        try {
            Map<String, Object> map = ValueConverter.toJavaMap(value);
            return mapper.writeValueAsString(map);
        } catch (IOException e) {
            throw new FormatException("json", "Failed to write JSON: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void write(DictValue value, OutputStream stream) {
        try {
            Map<String, Object> map = ValueConverter.toJavaMap(value);
            mapper.writeValue(stream, map);
        } catch (IOException e) {
            throw new FormatException("json", "Failed to write JSON to stream: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String formatName() {
        return "json";
    }
}
