package com.grizzly.format.xml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.grizzly.format.FormatException;
import com.grizzly.format.FormatReader;
import com.grizzly.types.DictValue;
import com.grizzly.types.ListValue;
import com.grizzly.types.NullValue;
import com.grizzly.types.NumberValue;
import com.grizzly.types.StringValue;
import com.grizzly.types.Value;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads XML content into a DictValue.
 * 
 * <p>This reader uses Jackson's XmlMapper to parse XML and converts the result
 * to the internal type-safe {@link DictValue} representation.
 * 
 * <h2>Important: Jackson XML Parsing Behavior</h2>
 * 
 * <p>Jackson's XmlMapper parses the ROOT element's content, not the root element itself.
 * This means the root element name is NOT included in the result. For example:
 * 
 * <pre>{@code
 * XML: <person><name>John</name><age>30</age></person>
 * Result: {name: "John", age: 30}  // "person" root is NOT in result
 * }</pre>
 * 
 * <p>Attributes are also flattened to the same level as child elements:
 * 
 * <pre>{@code
 * XML: <person id="123"><name>John</name></person>
 * Result: {id: "123", name: "John"}  // attribute "id" is at same level
 * }</pre>
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * String xml = "<person><name>John</name><age>30</age></person>";
 * 
 * XmlReader reader = new XmlReader();
 * DictValue data = reader.read(xml);
 * 
 * // Access: data["name"] = "John"
 * // Access: data["age"] = 30
 * }</pre>
 * 
 * @see XmlConfig For configuration options
 */
public class XmlReader implements FormatReader {
    
    private static final TypeReference<Map<String, Object>> MAP_TYPE = 
        new TypeReference<>() {};
    
    private final XmlMapper xmlMapper;
    private final XmlConfig config;
    
    /**
     * Create an XmlReader with default configuration.
     */
    public XmlReader() {
        this(XmlConfig.defaults());
    }
    
    /**
     * Create an XmlReader with custom configuration.
     * 
     * @param config The XML configuration
     */
    public XmlReader(XmlConfig config) {
        this.xmlMapper = new XmlMapper();
        this.config = config;
    }
    
    @Override
    public DictValue read(String content) {
        try {
            Map<String, Object> rawMap = xmlMapper.readValue(content, MAP_TYPE);
            return convertToDictValue(rawMap);
        } catch (IOException e) {
            throw new FormatException("xml", "Failed to parse XML: " + e.getMessage(), e);
        }
    }
    
    @Override
    public DictValue read(InputStream stream) {
        try {
            Map<String, Object> rawMap = xmlMapper.readValue(stream, MAP_TYPE);
            return convertToDictValue(rawMap);
        } catch (IOException e) {
            throw new FormatException("xml", "Failed to parse XML from stream: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String formatName() {
        return "xml";
    }
    
    /**
     * Convert a raw Map from Jackson XML parsing to a normalized DictValue.
     * 
     * <p>Handles Jackson's XML conventions and normalizes them to our standard format.
     */
    private DictValue convertToDictValue(Map<String, Object> rawMap) {
        Map<String, Value> result = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            String key = normalizeKey(entry.getKey());
            Value value = convertValue(entry.getValue());
            result.put(key, value);
        }
        
        return new DictValue(result);
    }
    
    /**
     * Convert a single value from the raw map to a Value.
     */
    @SuppressWarnings("unchecked")
    private Value convertValue(Object obj) {
        if (obj == null) {
            return NullValue.INSTANCE;
        }
        
        if (obj instanceof String s) {
            return parseStringValue(s);
        }
        
        if (obj instanceof Number n) {
            return new NumberValue(n);
        }
        
        if (obj instanceof Boolean b) {
            return com.grizzly.types.BoolValue.of(b);
        }
        
        if (obj instanceof List<?> list) {
            List<Value> items = new ArrayList<>(list.size());
            for (Object item : list) {
                items.add(convertValue(item));
            }
            return new ListValue(items);
        }
        
        if (obj instanceof Map<?, ?> map) {
            return convertMapValue((Map<String, Object>) map);
        }
        
        return new StringValue(obj.toString());
    }
    
    /**
     * Convert a Map value, handling attributes and text content.
     */
    private DictValue convertMapValue(Map<String, Object> map) {
        Map<String, Value> result = new HashMap<>();
        Map<String, Value> attributes = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key.startsWith("@")) {
                // Jackson XML uses @ prefix for attributes
                String attrName = key.substring(1);
                attributes.put(attrName, convertValue(value));
            } else if (key.isEmpty() || key.equals("$") || key.equals("#text")) {
                // Text content (Jackson uses empty string or $ for text)
                result.put(config.textContentKey(), convertValue(value));
            } else {
                // Regular element
                String normalizedKey = normalizeKey(key);
                result.put(normalizedKey, convertValue(value));
            }
        }
        
        // Add attributes if any
        if (!attributes.isEmpty()) {
            result.put(config.attributeKey(), new DictValue(attributes));
        }
        
        return new DictValue(result);
    }
    
    /**
     * Normalize a key name (strip namespace if not preserving).
     */
    private String normalizeKey(String key) {
        if (!config.preserveNamespaces() && key.contains(":")) {
            return key.substring(key.indexOf(':') + 1);
        }
        return key;
    }
    
    /**
     * Parse a string value, attempting to detect numbers and booleans.
     */
    private Value parseStringValue(String s) {
        if (s == null || s.isEmpty()) {
            return new StringValue("");
        }
        
        // Try to detect boolean
        if (s.equalsIgnoreCase("true")) {
            return com.grizzly.types.BoolValue.TRUE;
        }
        if (s.equalsIgnoreCase("false")) {
            return com.grizzly.types.BoolValue.FALSE;
        }
        
        // Try to detect integer
        try {
            if (s.matches("-?\\d+")) {
                long value = Long.parseLong(s);
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    return new NumberValue((int) value);
                }
                return new NumberValue(value);
            }
        } catch (NumberFormatException ignored) {}
        
        // Try to detect decimal
        try {
            if (s.matches("-?\\d+\\.\\d+")) {
                return new NumberValue(Double.parseDouble(s));
            }
        } catch (NumberFormatException ignored) {}
        
        // Default to string
        return new StringValue(s);
    }
}
