/**
 * XML format support for Grizzly Engine.
 * 
 * <p>This package provides XML reading and writing capabilities using Jackson XML.
 * 
 * <h2>XML to DictValue Conventions</h2>
 * <p>Since XML has concepts not present in JSON (attributes, mixed content),
 * we use special keys to represent them:
 * 
 * <pre>{@code
 * XML:
 * <person id="123" active="true">
 *     <name>John</name>
 *     <age>30</age>
 * </person>
 * 
 * DictValue:
 * {
 *     "person": {
 *         "_attributes": { "id": "123", "active": "true" },
 *         "name": "John",
 *         "age": 30
 *     }
 * }
 * }</pre>
 * 
 * <h2>Reading XML</h2>
 * <pre>{@code
 * XmlReader reader = new XmlReader();
 * DictValue data = reader.read(xmlString);
 * 
 * // Access element
 * String name = ((StringValue) data.get("person").get("name")).value();
 * 
 * // Access attribute
 * DictValue attrs = (DictValue) data.get("person").get("_attributes");
 * String id = ((StringValue) attrs.get("id")).value();
 * }</pre>
 * 
 * <h2>Writing XML</h2>
 * <pre>{@code
 * XmlWriter writer = new XmlWriter();
 * String xml = writer.write(dictValue);
 * 
 * // Specify root element
 * String xml = new XmlWriter().withRootElement("order").write(dictValue);
 * 
 * // Compact output
 * String compact = new XmlWriter().compact().write(dictValue);
 * }</pre>
 * 
 * <h2>Configuration</h2>
 * <pre>{@code
 * XmlConfig config = XmlConfig.defaults()
 *     .withRootElement("order")
 *     .withAttributeKey("@")       // Use @ instead of _attributes
 *     .withTextContentKey("#text") // Use #text instead of _text
 *     .withPreserveNamespaces(true);
 * 
 * XmlReader reader = new XmlReader(config);
 * }</pre>
 * 
 * @see com.grizzly.format.xml.XmlReader For parsing XML
 * @see com.grizzly.format.xml.XmlWriter For generating XML
 * @see com.grizzly.format.xml.XmlConfig For configuration options
 */
package com.grizzly.format.xml;
