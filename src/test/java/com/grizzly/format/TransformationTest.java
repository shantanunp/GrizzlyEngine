package com.grizzly.format;

import com.grizzly.format.json.JsonTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end transformation tests using JsonTemplate.
 */
class TransformationTest {
    
    @Test
    void jsonToJsonTransformation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
                OUTPUT["years"] = INPUT["age"]
                return OUTPUT
            """;
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template);
        
        String jsonInput = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"age\": 30}";
        String jsonOutput = jsonTemplate.transform(jsonInput);
        
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
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template);
        
        String jsonInput = "{\"address\": {\"city\": \"NYC\", \"country\": \"USA\"}}";
        String jsonOutput = jsonTemplate.transform(jsonInput);
        
        assertTrue(jsonOutput.contains("NYC"));
        assertTrue(jsonOutput.contains("USA"));
    }
    
    @Test
    void transformToMapReturnsJavaMap() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["message"] = "Hello, " + INPUT["name"]
                return OUTPUT
            """;
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template);
        
        String jsonInput = "{\"name\": \"World\"}";
        Map<String, Object> output = jsonTemplate.transformToMap(jsonInput);
        
        assertEquals("Hello, World", output.get("message"));
    }
    
    @Test
    void transformFromMapAcceptsJavaMap() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["doubled"] = INPUT["value"] * 2
                return OUTPUT
            """;
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template);
        
        Map<String, Object> inputMap = Map.of("value", 21);
        String jsonOutput = jsonTemplate.transformFromMap(inputMap);
        
        assertTrue(jsonOutput.contains("42"));
    }
    
    @Test
    void compactOutputRemovesWhitespace() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["a"] = 1
                OUTPUT["b"] = 2
                return OUTPUT
            """;
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template).compact();
        
        String jsonInput = "{}";
        String jsonOutput = jsonTemplate.transform(jsonInput);
        
        assertFalse(jsonOutput.contains("\n"));
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
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template);
        
        String jsonInput = """
            {
                "products": [
                    {"name": "Apple", "price": 1.50},
                    {"name": "Banana", "price": 0.75}
                ]
            }
            """;
        
        String jsonOutput = jsonTemplate.transform(jsonInput);
        
        assertTrue(jsonOutput.contains("Apple"));
        assertTrue(jsonOutput.contains("Banana"));
        assertTrue(jsonOutput.contains("1.5"));
        assertTrue(jsonOutput.contains("0.75"));
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
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template);
        
        String adultInput = "{\"name\": \"John\", \"age\": 25}";
        String adultOutput = jsonTemplate.transform(adultInput);
        assertTrue(adultOutput.contains("adult"));
        
        String minorInput = "{\"name\": \"Jane\", \"age\": 15}";
        String minorOutput = jsonTemplate.transform(minorInput);
        assertTrue(minorOutput.contains("minor"));
    }
    
    @Test
    void complexOrderTransformation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["order"] = {}
                OUTPUT["order"]["id"] = INPUT["orderId"]
                OUTPUT["order"]["customer"] = INPUT["customerName"]
                OUTPUT["order"]["total"] = INPUT["amount"]
                return OUTPUT
            """;
        
        JsonTemplate jsonTemplate = JsonTemplate.compile(template);
        
        String jsonInput = "{\"orderId\": \"ORD-001\", \"customerName\": \"John Doe\", \"amount\": 150}";
        String jsonOutput = jsonTemplate.transform(jsonInput);
        
        assertTrue(jsonOutput.contains("ORD-001"));
        assertTrue(jsonOutput.contains("John Doe"));
        assertTrue(jsonOutput.contains("150"));
    }
}
