package com.grizzly.format.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzly.format.FormatException;
import com.grizzly.format.FormatReader;
import com.grizzly.types.DictValue;
import com.grizzly.types.ValueConverter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Reads JSON content into a DictValue.
 * 
 * <p>This reader uses Jackson to parse JSON and converts the result
 * to the internal type-safe {@link DictValue} representation.
 * 
 * <h2>Supported JSON Types</h2>
 * <ul>
 *   <li>Objects → DictValue</li>
 *   <li>Arrays → ListValue</li>
 *   <li>Strings → StringValue</li>
 *   <li>Numbers → NumberValue</li>
 *   <li>Booleans → BoolValue</li>
 *   <li>null → NullValue</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * JsonReader reader = new JsonReader();
 * DictValue data = reader.read("{\"name\": \"John\", \"age\": 30}");
 * 
 * StringValue name = (StringValue) data.get("name");
 * System.out.println(name.value()); // "John"
 * }</pre>
 */
public class JsonReader implements FormatReader {
    
    private static final TypeReference<Map<String, Object>> MAP_TYPE = 
        new TypeReference<>() {};
    
    private final ObjectMapper mapper;
    
    /**
     * Create a JsonReader with a default ObjectMapper.
     */
    public JsonReader() {
        this.mapper = new ObjectMapper();
    }
    
    /**
     * Create a JsonReader with a custom ObjectMapper.
     * 
     * @param mapper The ObjectMapper to use
     */
    public JsonReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public DictValue read(String content) {
        try {
            Map<String, Object> map = mapper.readValue(content, MAP_TYPE);
            return ValueConverter.fromJavaMap(map);
        } catch (IOException e) {
            throw new FormatException("json", "Failed to parse JSON: " + e.getMessage(), e);
        }
    }
    
    @Override
    public DictValue read(InputStream stream) {
        try {
            Map<String, Object> map = mapper.readValue(stream, MAP_TYPE);
            return ValueConverter.fromJavaMap(map);
        } catch (IOException e) {
            throw new FormatException("json", "Failed to parse JSON from stream: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String formatName() {
        return "json";
    }
}
