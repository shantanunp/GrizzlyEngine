package com.grizzly.format;

import com.grizzly.GrizzlyEngine;
import com.grizzly.GrizzlyTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end transformation tests for different format combinations.
 * 
 * Note: Jackson XmlMapper parses the ROOT element's CONTENT, not the root element itself.
 * This means when reading XML, the root element name is NOT included in INPUT.
 */
class TransformationTest {
    
    private GrizzlyEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }
    
    // ==================== JSON to JSON ====================
    
    @Test
    void jsonToJsonTransformation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
                OUTPUT["years"] = INPUT["age"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String jsonInput = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"age\": 30}";
        String jsonOutput = compiled.transformJson(jsonInput);
        
        assertTrue(jsonOutput.contains("fullName"));
        assertTrue(jsonOutput.contains("John Doe"));
        assertTrue(jsonOutput.contains("years"));
        assertTrue(jsonOutput.contains("30"));
    }
    
    @Test
    void jsonToJsonWithNestedData() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["city"] = INPUT["address"]["city"]
                OUTPUT["country"] = INPUT["address"]["country"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String jsonInput = "{\"address\": {\"city\": \"NYC\", \"country\": \"USA\"}}";
        String jsonOutput = compiled.transformJson(jsonInput);
        
        assertTrue(jsonOutput.contains("NYC"));
        assertTrue(jsonOutput.contains("USA"));
    }
    
    // ==================== JSON to XML ====================
    
    @Test
    void jsonToXmlTransformation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
                OUTPUT["years"] = INPUT["age"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String jsonInput = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"age\": 30}";
        String xmlOutput = compiled.transformJsonToXml(jsonInput);
        
        assertTrue(xmlOutput.contains("<?xml"));
        assertTrue(xmlOutput.contains("<fullName>John Doe</fullName>"));
        assertTrue(xmlOutput.contains("<years>30</years>"));
    }
    
    @Test
    void jsonToXmlWithList() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["items"] = INPUT["products"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String jsonInput = "{\"products\": [\"Apple\", \"Banana\", \"Cherry\"]}";
        String xmlOutput = compiled.transformJsonToXml(jsonInput);
        
        assertTrue(xmlOutput.contains("<items>Apple</items>"));
        assertTrue(xmlOutput.contains("<items>Banana</items>"));
        assertTrue(xmlOutput.contains("<items>Cherry</items>"));
    }
    
    // ==================== XML to JSON ====================
    
    @Test
    void xmlToJsonTransformation() {
        // Note: Jackson parses root element's CONTENT, so "person" is not in INPUT
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["name"] = INPUT["name"]
                OUTPUT["age"] = INPUT["age"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String xmlInput = "<person><name>John</name><age>30</age></person>";
        String jsonOutput = compiled.transformXmlToJson(xmlInput);
        
        assertTrue(jsonOutput.contains("\"name\""));
        assertTrue(jsonOutput.contains("\"John\""));
        assertTrue(jsonOutput.contains("\"age\""));
        assertTrue(jsonOutput.contains("30"));
    }
    
    @Test
    void xmlToJsonWithAttributes() {
        // Jackson flattens attributes to the same level as children
        // XmlReader auto-detects types, so "123" becomes 123 (number)
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["id"] = INPUT["id"]
                OUTPUT["name"] = INPUT["name"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String xmlInput = "<person id=\"123\"><name>John</name></person>";
        String jsonOutput = compiled.transformXmlToJson(xmlInput);
        
        assertTrue(jsonOutput.contains("\"id\""));
        // Note: "123" is auto-detected as number, so JSON output has 123 (no quotes)
        assertTrue(jsonOutput.contains("123"));
        assertTrue(jsonOutput.contains("\"name\""));
        assertTrue(jsonOutput.contains("\"John\""));
    }
    
    // ==================== XML to XML ====================
    
    @Test
    void xmlToXmlTransformation() {
        // Note: Jackson parses root's content, so we access firstName/lastName directly
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["customer"] = {}
                OUTPUT["customer"]["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
                OUTPUT["customer"]["years"] = INPUT["age"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String xmlInput = "<person><firstName>John</firstName><lastName>Doe</lastName><age>30</age></person>";
        String xmlOutput = compiled.transformXml(xmlInput);
        
        assertTrue(xmlOutput.contains("<?xml"));
        assertTrue(xmlOutput.contains("<customer>"));
        assertTrue(xmlOutput.contains("<fullName>John Doe</fullName>"));
        assertTrue(xmlOutput.contains("<years>30</years>"));
        assertTrue(xmlOutput.contains("</customer>"));
    }
    
    @Test
    void xmlToXmlWithAttributeGeneration() {
        // Read attributes (flattened by Jackson), write with explicit _attributes
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result"] = {}
                OUTPUT["result"]["_attributes"] = {}
                OUTPUT["result"]["_attributes"]["id"] = INPUT["id"]
                OUTPUT["result"]["value"] = INPUT["value"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String xmlInput = "<data><id>123</id><value>Hello</value></data>";
        String xmlOutput = compiled.transformXml(xmlInput);
        
        assertTrue(xmlOutput.contains("<result"));
        assertTrue(xmlOutput.contains("id=\"123\""));
        assertTrue(xmlOutput.contains("<value>Hello</value>"));
    }
    
    // ==================== Generic Transform Method ====================
    
    @Test
    void genericTransformMethod() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["processed"] = True
                OUTPUT["data"] = INPUT["value"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String jsonInput = "{\"value\": \"test\"}";
        
        String jsonOutput = compiled.transform(jsonInput, "json", "json");
        assertTrue(jsonOutput.contains("test"));
        
        String xmlOutput = compiled.transform(jsonInput, "json", "xml");
        assertTrue(xmlOutput.contains("<data>test</data>"));
    }
    
    // ==================== Complex Transformations ====================
    
    @Test
    void complexOrderTransformation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["order"] = {}
                OUTPUT["order"]["_attributes"] = {}
                OUTPUT["order"]["_attributes"]["id"] = INPUT["orderId"]
                OUTPUT["order"]["customer"] = INPUT["customerName"]
                OUTPUT["order"]["total"] = INPUT["amount"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String jsonInput = "{\"orderId\": \"ORD-001\", \"customerName\": \"John Doe\", \"amount\": 150}";
        String xmlOutput = compiled.transformJsonToXml(jsonInput);
        
        assertTrue(xmlOutput.contains("id=\"ORD-001\""));
        assertTrue(xmlOutput.contains("<customer>John Doe</customer>"));
        assertTrue(xmlOutput.contains("<total>150</total>"));
    }
    
    @Test
    void loopingTransformation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["items"] = []
                for item in INPUT["products"]:
                    processed = {}
                    processed["name"] = item["name"]
                    processed["price"] = item["price"]
                    OUTPUT["items"].append(processed)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String jsonInput = """
            {
                "products": [
                    {"name": "Apple", "price": 1.50},
                    {"name": "Banana", "price": 0.75}
                ]
            }
            """;
        
        String xmlOutput = compiled.transformJsonToXml(jsonInput);
        
        assertTrue(xmlOutput.contains("<name>Apple</name>"));
        assertTrue(xmlOutput.contains("<name>Banana</name>"));
    }
    
    @Test
    void conditionalTransformation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                if INPUT["age"] >= 18:
                    OUTPUT["status"] = "adult"
                else:
                    OUTPUT["status"] = "minor"
                OUTPUT["name"] = INPUT["name"]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        String adultInput = "{\"name\": \"John\", \"age\": 25}";
        String adultOutput = compiled.transformJson(adultInput);
        assertTrue(adultOutput.contains("adult"));
        
        String minorInput = "{\"name\": \"Jane\", \"age\": 15}";
        String minorOutput = compiled.transformJson(minorInput);
        assertTrue(minorOutput.contains("minor"));
    }
}
