package com.grizzly.core.interpreter;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for for-loop iteration over different types:
 * lists, strings (characters), and dicts (keys).
 */
class ForLoopIterationTest {
    
    private GrizzlyEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }
    
    // ==================== Iterate Over List ====================
    
    @Test
    @DisplayName("for loop iterates over list items")
    void iterateList() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = [10, 20, 30]
                total = 0
                for item in items:
                    total = total + item
                OUTPUT["total"] = total
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("total")).isEqualTo(60);
    }
    
    // ==================== Iterate Over String (Characters) ====================
    
    @Test
    @DisplayName("for loop iterates over string characters")
    void iterateString() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                text = "ABC"
                chars = []
                for char in text:
                    chars.append(char.lower())
                OUTPUT["chars"] = chars
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<String> chars = (List<String>) result.get("chars");
        assertThat(chars).containsExactly("a", "b", "c");
    }
    
    @Test
    @DisplayName("for loop over empty string produces no iterations")
    void iterateEmptyString() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                count = 0
                for char in "":
                    count = count + 1
                OUTPUT["count"] = count
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("count")).isEqualTo(0);
    }
    
    // ==================== Iterate Over Dict (Keys) ====================
    
    @Test
    @DisplayName("for k, v in items: tuple unpacking in for loop")
    void tupleUnpackingInForLoop() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = [["a", 1], ["b", 2], ["c", 3]]
                result = {}
                for k, v in items:
                    result[k] = v
                OUTPUT["result"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> out = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) out.get("result");
        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            Map.of("a", 1, "b", 2, "c", 3)
        );
    }
    
    @Test
    @DisplayName("for k, v in dict.items(): unpack dict items")
    void tupleUnpackingDictItems() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                d = {"x": 10, "y": 20}
                result = {}
                for k, v in d.items():
                    result[k] = v
                OUTPUT["result"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> out = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) out.get("result");
        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            Map.of("x", 10, "y", 20)
        );
    }
    
    @Test
    @DisplayName("for loop iterates over dict keys")
    void iterateDictKeys() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                person = {"name": "John", "age": 30}
                keys = []
                for key in person:
                    keys.append(key)
                OUTPUT["keys"] = keys
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) result.get("keys");
        assertThat(keys).containsExactlyInAnyOrder("name", "age");
    }
    
    @Test
    @DisplayName("for loop can access dict values using keys")
    void iterateDictAccess() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                prices = {"apple": 1.50, "banana": 0.75}
                total = 0
                for fruit in prices:
                    total = total + prices[fruit]
                OUTPUT["total"] = total
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat((Double) result.get("total")).isCloseTo(2.25, within(0.001));
    }
    
    // ==================== Iterate Over Dict Items ====================
    
    @Test
    @DisplayName("for loop iterates over dict.items()")
    void iterateDictItems() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                prices = {"apple": 100, "banana": 75}
                items_list = prices.items()
                result = []
                for pair in items_list:
                    result.append(pair[0] + "=" + str(pair[1]))
                OUTPUT["items"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) result.get("items");
        assertThat(items).containsExactlyInAnyOrder("apple=100", "banana=75");
    }
    
    // ==================== Nested Iteration ====================
    
    @Test
    @DisplayName("nested for loops work correctly")
    void nestedForLoops() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                matrix = [[1, 2], [3, 4]]
                total = 0
                for row in matrix:
                    for cell in row:
                        total = total + cell
                OUTPUT["total"] = total
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("total")).isEqualTo(10);
    }
    
    // ==================== Practical Mapping Use Cases ====================
    
    @Test
    @DisplayName("Transform list of dicts from INPUT")
    void transformListOfDicts() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["users"] = []
                for user in INPUT["users"]:
                    transformed = {}
                    transformed["fullName"] = user["firstName"] + " " + user["lastName"]
                    transformed["email"] = user["email"].lower()
                    OUTPUT["users"].append(transformed)
                return OUTPUT
            """;
        
        Map<String, Object> input = Map.of(
            "users", List.of(
                Map.of("firstName", "John", "lastName", "Doe", "email", "JOHN@EXAMPLE.COM"),
                Map.of("firstName", "Jane", "lastName", "Smith", "email", "JANE@EXAMPLE.COM")
            )
        );
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(input);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        
        assertThat(users).hasSize(2);
        assertThat(users.get(0).get("fullName")).isEqualTo("John Doe");
        assertThat(users.get(0).get("email")).isEqualTo("john@example.com");
    }
    
    @Test
    @DisplayName("Filter and transform with conditions")
    void filterAndTransform() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["adults"] = []
                for person in INPUT["people"]:
                    if person["age"] >= 18:
                        OUTPUT["adults"].append(person["name"])
                return OUTPUT
            """;
        
        Map<String, Object> input = Map.of(
            "people", List.of(
                Map.of("name", "Alice", "age", 25),
                Map.of("name", "Bob", "age", 15),
                Map.of("name", "Charlie", "age", 30)
            )
        );
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(input);
        
        @SuppressWarnings("unchecked")
        List<String> adults = (List<String>) result.get("adults");
        
        assertThat(adults).containsExactly("Alice", "Charlie");
    }
    
    @Test
    @DisplayName("Pass OUTPUT to module functions (mutable reference)")
    void mutableOutputReference() throws Exception {
        String template = """
            def add_metadata(INPUT, OUTPUT):
                OUTPUT["meta"] = {}
                OUTPUT["meta"]["source"] = "grizzly"
                OUTPUT["meta"]["version"] = "1.0"
            
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["data"] = INPUT["value"]
                add_metadata(INPUT, OUTPUT)
                return OUTPUT
            """;
        
        Map<String, Object> input = Map.of("value", 42);
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(input);
        
        assertThat(result.get("data")).isEqualTo(42);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) result.get("meta");
        assertThat(meta.get("source")).isEqualTo("grizzly");
        assertThat(meta.get("version")).isEqualTo("1.0");
    }
}
