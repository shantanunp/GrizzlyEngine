package com.grizzly.format.xml;

/**
 * Configuration options for XML reading and writing.
 * 
 * <p>This configuration defines how XML-specific concepts (attributes,
 * text content, namespaces) are represented in the DictValue model.
 * 
 * <h2>Convention</h2>
 * <p>Since JSON doesn't have attributes or mixed content, we use special
 * keys to represent these XML concepts:
 * 
 * <pre>{@code
 * XML:
 * <person id="123" active="true">
 *     <name>John</name>
 * </person>
 * 
 * DictValue (using defaults):
 * {
 *     "person": {
 *         "_attributes": { "id": "123", "active": "true" },
 *         "name": "John"
 *     }
 * }
 * }</pre>
 * 
 * @param rootElementName Default root element name for output (default: "root")
 * @param attributeKey Key for storing attributes (default: "_attributes")
 * @param textContentKey Key for storing text content (default: "_text")
 * @param preserveNamespaces Whether to preserve namespace prefixes (default: false)
 * @param prettyPrint Whether to format output with indentation (default: true)
 */
public record XmlConfig(
    String rootElementName,
    String attributeKey,
    String textContentKey,
    boolean preserveNamespaces,
    boolean prettyPrint
) {
    
    /**
     * Create default configuration.
     * 
     * <p>Defaults:
     * <ul>
     *   <li>rootElementName: "root"</li>
     *   <li>attributeKey: "_attributes"</li>
     *   <li>textContentKey: "_text"</li>
     *   <li>preserveNamespaces: false</li>
     *   <li>prettyPrint: true</li>
     * </ul>
     */
    public static XmlConfig defaults() {
        return new XmlConfig("root", "_attributes", "_text", false, true);
    }
    
    /**
     * Create a new config with a different root element name.
     * 
     * @param name The root element name
     * @return New config with updated root element
     */
    public XmlConfig withRootElement(String name) {
        return new XmlConfig(name, attributeKey, textContentKey, preserveNamespaces, prettyPrint);
    }
    
    /**
     * Create a new config with a different attribute key.
     * 
     * @param key The attribute key (e.g., "@", "_attr")
     * @return New config with updated attribute key
     */
    public XmlConfig withAttributeKey(String key) {
        return new XmlConfig(rootElementName, key, textContentKey, preserveNamespaces, prettyPrint);
    }
    
    /**
     * Create a new config with a different text content key.
     * 
     * @param key The text content key (e.g., "$", "#text")
     * @return New config with updated text content key
     */
    public XmlConfig withTextContentKey(String key) {
        return new XmlConfig(rootElementName, attributeKey, key, preserveNamespaces, prettyPrint);
    }
    
    /**
     * Create a new config with namespace preservation toggled.
     * 
     * @param preserve Whether to preserve namespaces
     * @return New config with updated namespace setting
     */
    public XmlConfig withPreserveNamespaces(boolean preserve) {
        return new XmlConfig(rootElementName, attributeKey, textContentKey, preserve, prettyPrint);
    }
    
    /**
     * Create a new config with pretty print toggled.
     * 
     * @param pretty Whether to pretty print
     * @return New config with updated pretty print setting
     */
    public XmlConfig withPrettyPrint(boolean pretty) {
        return new XmlConfig(rootElementName, attributeKey, textContentKey, preserveNamespaces, pretty);
    }
}
