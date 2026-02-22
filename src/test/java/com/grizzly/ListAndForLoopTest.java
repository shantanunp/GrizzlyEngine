package com.grizzly;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test list operations and for loops
 */
public class ListAndForLoopTest {
    
    @Test
    public void testEmptyList() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["items"] = []
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        Map<String, Object> input = Map.of();
        
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiledTemplate.execute(input, Map.class);
        
        assertTrue(result.containsKey("items"));
        assertTrue(result.get("items") instanceof List);
        assertEquals(0, ((List<?>) result.get("items")).size());
    }
    
    @Test
    public void testListLiteralWithValues() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["numbers"] = [1, 2, 3]
                OUTPUT["names"] = ["Alice", "Bob"]
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        Map<String, Object> input = Map.of();
        
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiledTemplate.execute(input, Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> numbers = (List<Object>) result.get("numbers");
        assertEquals(3, numbers.size());
        
        @SuppressWarnings("unchecked")
        List<Object> names = (List<Object>) result.get("names");
        assertEquals(2, names.size());
    }
    
    @Test
    public void testListAppend() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["items"] = []
                OUTPUT["items"].append("first")
                OUTPUT["items"].append("second")
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        Map<String, Object> input = Map.of();
        
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiledTemplate.execute(input, Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) result.get("items");
        assertEquals(2, items.size());
        assertEquals("first", items.get(0));
        assertEquals("second", items.get(1));
    }
    
    @Test
    public void testSimpleForLoop() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["names"] = []
                
                for customer in INPUT.customers:
                    OUTPUT["names"].append(customer.name)
                
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        Map<String, Object> input = Map.of(
            "customers", List.of(
                Map.of("name", "Alice"),
                Map.of("name", "Bob"),
                Map.of("name", "Charlie")
            )
        );
        
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiledTemplate.execute(input, Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> names = (List<Object>) result.get("names");
        assertEquals(3, names.size());
        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
        assertEquals("Charlie", names.get(2));
    }
    
    @Test
    public void testForLoopWithMapping() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["users"] = []
                
                for customer in INPUT.customers:
                    user = {}
                    user["id"] = customer.customerId
                    user["fullName"] = customer.firstName + " " + customer.lastName
                    OUTPUT["users"].append(user)
                
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        Map<String, Object> input = Map.of(
            "customers", List.of(
                Map.of("customerId", "C001", "firstName", "John", "lastName", "Doe"),
                Map.of("customerId", "C002", "firstName", "Jane", "lastName", "Smith")
            )
        );
        
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiledTemplate.execute(input, Map.class);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        
        assertEquals(2, users.size());
        assertEquals("C001", users.get(0).get("id"));
        assertEquals("John Doe", users.get(0).get("fullName"));
        assertEquals("C002", users.get(1).get("id"));
        assertEquals("Jane Smith", users.get(1).get("fullName"));
    }
    
    @Test
    public void testForLoopWithConditional() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["adults"] = []
                
                for person in INPUT.people:
                    if person.age >= 18:
                        OUTPUT["adults"].append(person.name)
                
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        Map<String, Object> input = Map.of(
            "people", List.of(
                Map.of("name", "Alice", "age", 25),
                Map.of("name", "Bob", "age", 16),
                Map.of("name", "Charlie", "age", 30)
            )
        );
        
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiledTemplate.execute(input, Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> adults = (List<Object>) result.get("adults");
        
        assertEquals(2, adults.size());
        assertEquals("Alice", adults.get(0));
        assertEquals("Charlie", adults.get(1));
    }
    
    @Test
    public void testLenFunction() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["customerCount"] = len(INPUT.customers)
                OUTPUT["nameLength"] = len(INPUT.name)
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        Map<String, Object> input = Map.of(
            "customers", List.of("C1", "C2", "C3"),
            "name", "Grizzly"
        );
        
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiledTemplate.execute(input, Map.class);
        
        assertEquals(3, result.get("customerCount"));
        assertEquals(7, result.get("nameLength"));
    }
}
