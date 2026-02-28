package com.grizzly.format;

import com.grizzly.format.xml.XmlReader;
import com.grizzly.format.xml.XmlWriter;
import com.grizzly.types.DictValue;
import com.grizzly.types.ListValue;
import com.grizzly.types.NumberValue;
import com.grizzly.types.StringValue;
import com.grizzly.types.BoolValue;
import com.grizzly.types.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XML reader and writer.
 * 
 * Note: Jackson XmlMapper parses the ROOT element's CONTENT, not the root element itself.
 * This means the root element name is NOT included in the result.
 */
class XmlReaderWriterTest {
    
    private final XmlReader reader = new XmlReader();
    private final XmlWriter writer = new XmlWriter();
    
    // ==================== Reader Tests ====================
    
    @Test
    void readSimpleElement() {
        // Jackson parses the content of <person>, not <person> itself
        String xml = "<person><name>John</name><age>30</age></person>";
        DictValue result = reader.read(xml);
        
        // Root "person" is NOT in result - Jackson gives us its content directly
        assertEquals("John", ((StringValue) result.get("name")).value());
        assertEquals(30, ((NumberValue) result.get("age")).asInt());
    }
    
    @Test
    void readNestedElements() {
        String xml = "<order><customer><name>John</name></customer><total>100</total></order>";
        DictValue result = reader.read(xml);
        
        // "order" is root, so we get its content
        DictValue customer = (DictValue) result.get("customer");
        assertEquals("John", ((StringValue) customer.get("name")).value());
        assertEquals(100, ((NumberValue) result.get("total")).asInt());
    }
    
    @Test
    void readElementWithAttributes() {
        // Jackson flattens attributes to the same level as children
        String xml = "<person id=\"123\" active=\"true\"><name>John</name></person>";
        DictValue result = reader.read(xml);
        
        // Attributes are at the same level as child elements
        // Note: XmlReader auto-detects types, so "123" becomes NumberValue
        assertEquals(123, ((NumberValue) result.get("id")).asInt());
        assertTrue(((BoolValue) result.get("active")).value());
        assertEquals("John", ((StringValue) result.get("name")).value());
    }
    
    @Test
    void readRepeatedElements() {
        String xml = "<order><item>Apple</item><item>Banana</item><item>Cherry</item></order>";
        DictValue result = reader.read(xml);
        
        Value itemValue = result.get("item");
        
        // Jackson should make repeated elements into a list
        assertInstanceOf(ListValue.class, itemValue);
        ListValue items = (ListValue) itemValue;
        assertEquals(3, items.size());
    }
    
    @Test
    void readBooleanValues() {
        String xml = "<config><enabled>true</enabled><debug>false</debug></config>";
        DictValue result = reader.read(xml);
        
        assertTrue(((BoolValue) result.get("enabled")).value());
        assertFalse(((BoolValue) result.get("debug")).value());
    }
    
    @Test
    void readDecimalNumbers() {
        String xml = "<product><price>19.99</price><quantity>5</quantity></product>";
        DictValue result = reader.read(xml);
        
        assertEquals(19.99, ((NumberValue) result.get("price")).asDouble(), 0.001);
        assertEquals(5, ((NumberValue) result.get("quantity")).asInt());
    }
    
    @Test
    void readInvalidXmlThrowsException() {
        assertThrows(FormatException.class, () -> reader.read("<invalid><xml>"));
    }
    
    // ==================== Writer Tests ====================
    
    @Test
    void writeSimpleElement() {
        DictValue person = DictValue.empty();
        person.put("name", new StringValue("John"));
        person.put("age", new NumberValue(30));
        
        DictValue root = DictValue.empty();
        root.put("person", person);
        
        String xml = writer.write(root);
        
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<age>30</age>"));
        assertTrue(xml.contains("</person>"));
    }
    
    @Test
    void writeElementWithAttributes() {
        DictValue attrs = DictValue.empty();
        attrs.put("id", new StringValue("123"));
        attrs.put("active", new StringValue("true"));
        
        DictValue person = DictValue.empty();
        person.put("_attributes", attrs);
        person.put("name", new StringValue("John"));
        
        DictValue root = DictValue.empty();
        root.put("person", person);
        
        String xml = writer.write(root);
        
        assertTrue(xml.contains("id=\"123\""));
        assertTrue(xml.contains("active=\"true\""));
        assertTrue(xml.contains("<name>John</name>"));
    }
    
    @Test
    void writeNestedElements() {
        DictValue address = DictValue.empty();
        address.put("city", new StringValue("NYC"));
        
        DictValue person = DictValue.empty();
        person.put("name", new StringValue("John"));
        person.put("address", address);
        
        DictValue root = DictValue.empty();
        root.put("person", person);
        
        String xml = writer.write(root);
        
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("<address>"));
        assertTrue(xml.contains("<city>NYC</city>"));
        assertTrue(xml.contains("</address>"));
        assertTrue(xml.contains("</person>"));
    }
    
    @Test
    void writeListAsRepeatedElements() {
        ListValue items = ListValue.of(
            new StringValue("Apple"),
            new StringValue("Banana"),
            new StringValue("Cherry")
        );
        
        DictValue order = DictValue.empty();
        order.put("item", items);
        
        DictValue root = DictValue.empty();
        root.put("order", order);
        
        String xml = writer.write(root);
        
        assertTrue(xml.contains("<item>Apple</item>"));
        assertTrue(xml.contains("<item>Banana</item>"));
        assertTrue(xml.contains("<item>Cherry</item>"));
    }
    
    @Test
    void writeWithCustomRootElement() {
        DictValue dict = DictValue.empty();
        dict.put("name", new StringValue("John"));
        dict.put("age", new NumberValue(30));
        
        String xml = new XmlWriter().withRootElement("customer").write(dict);
        
        assertTrue(xml.contains("<customer>"));
        assertTrue(xml.contains("</customer>"));
    }
    
    @Test
    void writeCompactOutput() {
        DictValue person = DictValue.empty();
        person.put("name", new StringValue("John"));
        
        DictValue root = DictValue.empty();
        root.put("person", person);
        
        String compactXml = new XmlWriter().compact().write(root);
        
        assertTrue(compactXml.contains("<person><name>John</name></person>"));
    }
    
    @Test
    void escapeSpecialCharacters() {
        DictValue dict = DictValue.empty();
        dict.put("formula", new StringValue("a < b && b > c"));
        
        DictValue root = DictValue.empty();
        root.put("data", dict);
        
        String xml = writer.write(root);
        
        assertTrue(xml.contains("&lt;"));
        assertTrue(xml.contains("&gt;"));
        assertTrue(xml.contains("&amp;"));
    }
}
