package com.grizzly.format.xml;

import com.grizzly.format.FormatException;
import com.grizzly.format.FormatWriter;
import com.grizzly.types.BoolValue;
import com.grizzly.types.DateTimeValue;
import com.grizzly.types.DecimalValue;
import com.grizzly.types.DictValue;
import com.grizzly.types.ListValue;
import com.grizzly.types.NullValue;
import com.grizzly.types.NumberValue;
import com.grizzly.types.StringValue;
import com.grizzly.types.Value;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Writes a DictValue to XML format.
 * 
 * <p>This writer converts the internal type-safe DictValue representation
 * to XML, handling the special conventions for attributes and text content.
 * 
 * <h2>DictValue to XML Mapping</h2>
 * 
 * <table border="1">
 *   <tr><th>DictValue Concept</th><th>XML Output</th></tr>
 *   <tr><td>Dict keys</td><td>Elements</td></tr>
 *   <tr><td>"_attributes" dict</td><td>Element attributes</td></tr>
 *   <tr><td>"_text" value</td><td>Text content</td></tr>
 *   <tr><td>ListValue</td><td>Repeated elements</td></tr>
 * </table>
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * DictValue person = DictValue.empty();
 * DictValue attrs = DictValue.empty();
 * attrs.put("id", new StringValue("123"));
 * person.put("_attributes", attrs);
 * person.put("name", new StringValue("John"));
 * person.put("age", new NumberValue(30));
 * 
 * DictValue root = DictValue.empty();
 * root.put("person", person);
 * 
 * XmlWriter writer = new XmlWriter();
 * String xml = writer.write(root);
 * // <person id="123">
 * //   <name>John</name>
 * //   <age>30</age>
 * // </person>
 * }</pre>
 * 
 * @see XmlConfig For configuration options
 */
public class XmlWriter implements FormatWriter {
    
    private XmlConfig config;
    
    /**
     * Create an XmlWriter with default configuration.
     */
    public XmlWriter() {
        this.config = XmlConfig.defaults();
    }
    
    /**
     * Create an XmlWriter with custom configuration.
     * 
     * @param config The XML configuration
     */
    public XmlWriter(XmlConfig config) {
        this.config = config;
    }
    
    /**
     * Configure the root element name.
     * 
     * @param name The root element name
     * @return this writer for chaining
     */
    public XmlWriter withRootElement(String name) {
        this.config = config.withRootElement(name);
        return this;
    }
    
    /**
     * Configure for compact (no indentation) output.
     * 
     * @return this writer for chaining
     */
    public XmlWriter compact() {
        this.config = config.withPrettyPrint(false);
        return this;
    }
    
    /**
     * Configure for pretty-printed output.
     * 
     * @return this writer for chaining
     */
    public XmlWriter prettyPrint() {
        this.config = config.withPrettyPrint(true);
        return this;
    }
    
    @Override
    public String write(DictValue value) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        
        // If the dict has a single key, use that as root
        // Otherwise, wrap in the configured root element
        if (value.entries().size() == 1) {
            Map.Entry<String, Value> entry = value.entries().entrySet().iterator().next();
            writeElement(sb, entry.getKey(), entry.getValue(), 0);
        } else {
            writeElement(sb, config.rootElementName(), value, 0);
        }
        
        return sb.toString();
    }
    
    @Override
    public void write(DictValue value, OutputStream stream) {
        try {
            Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            writer.write(write(value));
            writer.flush();
        } catch (IOException e) {
            throw new FormatException("xml", "Failed to write XML to stream: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String formatName() {
        return "xml";
    }
    
    /**
     * Write an element with its value.
     */
    private void writeElement(StringBuilder sb, String name, Value value, int indent) {
        String indentStr = config.prettyPrint() ? "  ".repeat(indent) : "";
        String newline = config.prettyPrint() ? "\n" : "";
        
        switch (value) {
            case NullValue ignored -> {
                sb.append(indentStr).append("<").append(name).append("/>").append(newline);
            }
            
            case StringValue s -> {
                sb.append(indentStr).append("<").append(name).append(">")
                  .append(escapeXml(s.value()))
                  .append("</").append(name).append(">").append(newline);
            }
            
            case NumberValue n -> {
                String numStr = n.isInteger() ? String.valueOf(n.asLong()) : String.valueOf(n.asDouble());
                sb.append(indentStr).append("<").append(name).append(">")
                  .append(numStr)
                  .append("</").append(name).append(">").append(newline);
            }
            
            case BoolValue b -> {
                sb.append(indentStr).append("<").append(name).append(">")
                  .append(b.value())
                  .append("</").append(name).append(">").append(newline);
            }
            
            case DateTimeValue dt -> {
                sb.append(indentStr).append("<").append(name).append(">")
                  .append(escapeXml(dt.toString()))
                  .append("</").append(name).append(">").append(newline);
            }
            
            case DecimalValue dec -> {
                sb.append(indentStr).append("<").append(name).append(">")
                  .append(dec.toString())
                  .append("</").append(name).append(">").append(newline);
            }
            
            case ListValue list -> {
                // Write each item as a repeated element
                for (Value item : list) {
                    writeElement(sb, name, item, indent);
                }
            }
            
            case DictValue dict -> {
                writeDictElement(sb, name, dict, indent);
            }
        }
    }
    
    /**
     * Write a dict as an element with attributes and child elements.
     */
    private void writeDictElement(StringBuilder sb, String name, DictValue dict, int indent) {
        String indentStr = config.prettyPrint() ? "  ".repeat(indent) : "";
        String newline = config.prettyPrint() ? "\n" : "";
        
        // Extract attributes if present
        Value attrValue = dict.getOrNull(config.attributeKey());
        DictValue attributes = (attrValue instanceof DictValue d) ? d : null;
        
        // Extract text content if present
        Value textContent = dict.getOrNull(config.textContentKey());
        
        // Start element with attributes
        sb.append(indentStr).append("<").append(name);
        if (attributes != null) {
            for (Map.Entry<String, Value> attr : attributes.entries().entrySet()) {
                sb.append(" ").append(attr.getKey()).append("=\"")
                  .append(escapeXmlAttribute(valueToString(attr.getValue())))
                  .append("\"");
            }
        }
        
        // Check if element is empty (only has _attributes and/or _text)
        boolean hasChildElements = dict.entries().entrySet().stream()
            .anyMatch(e -> !e.getKey().equals(config.attributeKey()) && 
                          !e.getKey().equals(config.textContentKey()));
        
        if (!hasChildElements && textContent == null) {
            // Self-closing empty element
            sb.append("/>").append(newline);
            return;
        }
        
        sb.append(">");
        
        // Write text content if present
        if (textContent != null) {
            sb.append(escapeXml(valueToString(textContent)));
        }
        
        if (hasChildElements) {
            sb.append(newline);
            
            // Write child elements
            for (Map.Entry<String, Value> entry : dict.entries().entrySet()) {
                String key = entry.getKey();
                if (!key.equals(config.attributeKey()) && !key.equals(config.textContentKey())) {
                    writeElement(sb, key, entry.getValue(), indent + 1);
                }
            }
            
            sb.append(indentStr);
        }
        
        sb.append("</").append(name).append(">").append(newline);
    }
    
    /**
     * Convert a value to its string representation.
     */
    private String valueToString(Value value) {
        return switch (value) {
            case NullValue ignored -> "";
            case StringValue s -> s.value();
            case NumberValue n -> n.isInteger() ? String.valueOf(n.asLong()) : String.valueOf(n.asDouble());
            case BoolValue b -> String.valueOf(b.value());
            case DateTimeValue dt -> dt.toString();
            case DecimalValue dec -> dec.toString();
            case ListValue l -> l.toString();
            case DictValue d -> d.toString();
        };
    }
    
    /**
     * Escape special characters for XML text content.
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
    
    /**
     * Escape special characters for XML attribute values.
     */
    private String escapeXmlAttribute(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
