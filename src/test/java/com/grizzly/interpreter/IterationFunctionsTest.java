package com.grizzly.interpreter;

import com.grizzly.GrizzlyEngine;
import com.grizzly.GrizzlyTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for new iteration functions: enumerate, zip, sorted, reversed, any, all
 * and new built-ins: list, dict, type, isinstance, hasattr, getattr
 */
class IterationFunctionsTest {
    
    private GrizzlyEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }
    
    // ==================== enumerate() ====================
    
    @Test
    @DisplayName("enumerate() provides index and value pairs")
    void enumerate() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = ["a", "b", "c"]
                enumerated = enumerate(items)
                result = []
                for pair in enumerated:
                    result.append(str(pair[0]) + ":" + pair[1])
                OUTPUT["result"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) result.get("result");
        assertThat(items).containsExactly("0:a", "1:b", "2:c");
    }
    
    @Test
    @DisplayName("enumerate() with start parameter")
    void enumerateWithStart() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = ["x", "y"]
                enumerated = enumerate(items, 1)
                result = []
                for pair in enumerated:
                    result.append(pair[0])
                OUTPUT["indices"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Integer> indices = (List<Integer>) result.get("indices");
        assertThat(indices).containsExactly(1, 2);
    }
    
    // ==================== zip() ====================
    
    @Test
    @DisplayName("zip() combines two lists")
    void zip() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                names = ["Alice", "Bob"]
                ages = [25, 30]
                zipped = zip(names, ages)
                result = []
                for pair in zipped:
                    result.append(pair[0] + ":" + str(pair[1]))
                OUTPUT["result"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) result.get("result");
        assertThat(items).containsExactly("Alice:25", "Bob:30");
    }
    
    @Test
    @DisplayName("zip() stops at shortest list")
    void zipUnequalLengths() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                a = [1, 2, 3, 4, 5]
                b = ["x", "y"]
                zipped = zip(a, b)
                OUTPUT["count"] = len(zipped)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        assertThat(result.get("count")).isEqualTo(2);
    }
    
    // ==================== sorted() ====================
    
    @Test
    @DisplayName("sorted() returns new sorted list")
    void sorted() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = [3, 1, 4, 1, 5]
                OUTPUT["sorted"] = sorted(items)
                OUTPUT["original"] = items
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Integer> sortedList = (List<Integer>) result.get("sorted");
        @SuppressWarnings("unchecked")
        List<Integer> original = (List<Integer>) result.get("original");
        
        assertThat(sortedList).containsExactly(1, 1, 3, 4, 5);
        assertThat(original).containsExactly(3, 1, 4, 1, 5); // Unchanged
    }
    
    @Test
    @DisplayName("sorted() with reverse=True")
    void sortedReverse() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = [3, 1, 4, 1, 5]
                OUTPUT["sorted"] = sorted(items, True)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Integer> sortedList = (List<Integer>) result.get("sorted");
        assertThat(sortedList).containsExactly(5, 4, 3, 1, 1);
    }
    
    // ==================== reversed() ====================
    
    @Test
    @DisplayName("reversed() returns new reversed list")
    void reversed() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = [1, 2, 3]
                OUTPUT["reversed"] = reversed(items)
                OUTPUT["original"] = items
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Integer> reversedList = (List<Integer>) result.get("reversed");
        @SuppressWarnings("unchecked")
        List<Integer> original = (List<Integer>) result.get("original");
        
        assertThat(reversedList).containsExactly(3, 2, 1);
        assertThat(original).containsExactly(1, 2, 3); // Unchanged
    }
    
    // ==================== any() ====================
    
    @Test
    @DisplayName("any() returns True if any element is truthy")
    void any() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["anyTruthy"] = any([False, 0, "", "hello"])
                OUTPUT["allFalsy"] = any([False, 0, "", None])
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        assertThat(result.get("anyTruthy")).isEqualTo(true);
        assertThat(result.get("allFalsy")).isEqualTo(false);
    }
    
    // ==================== all() ====================
    
    @Test
    @DisplayName("all() returns True if all elements are truthy")
    void all() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["allTruthy"] = all([True, 1, "hello"])
                OUTPUT["someFalsy"] = all([True, 0, "hello"])
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        assertThat(result.get("allTruthy")).isEqualTo(true);
        assertThat(result.get("someFalsy")).isEqualTo(false);
    }
    
    // ==================== list() ====================
    
    @Test
    @DisplayName("list() converts string to list of characters")
    void listFromString() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                chars = list("abc")
                OUTPUT["chars"] = chars
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<String> chars = (List<String>) result.get("chars");
        assertThat(chars).containsExactly("a", "b", "c");
    }
    
    @Test
    @DisplayName("list() creates copy of list")
    void listCopy() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                original = [1, 2, 3]
                copy = list(original)
                copy.append(4)
                OUTPUT["original"] = original
                OUTPUT["copy"] = copy
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Integer> original = (List<Integer>) result.get("original");
        @SuppressWarnings("unchecked")
        List<Integer> copy = (List<Integer>) result.get("copy");
        
        assertThat(original).containsExactly(1, 2, 3);
        assertThat(copy).containsExactly(1, 2, 3, 4);
    }
    
    // ==================== dict() ====================
    
    @Test
    @DisplayName("dict() creates empty dictionary")
    void dictEmpty() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                d = dict()
                d["key"] = "value"
                OUTPUT["dict"] = d
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> d = (Map<String, Object>) result.get("dict");
        assertThat(d.get("key")).isEqualTo("value");
    }
    
    // ==================== type() ====================
    
    @Test
    @DisplayName("type() returns type name")
    void type() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["strType"] = type("hello")
                OUTPUT["intType"] = type(42)
                OUTPUT["listType"] = type([1, 2, 3])
                OUTPUT["dictType"] = type({"a": 1})
                OUTPUT["boolType"] = type(True)
                OUTPUT["noneType"] = type(None)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        assertThat(result.get("strType")).isEqualTo("string");
        assertThat(result.get("intType")).isEqualTo("number");
        assertThat(result.get("listType")).isEqualTo("list");
        assertThat(result.get("dictType")).isEqualTo("dict");
        assertThat(result.get("boolType")).isEqualTo("bool");
        assertThat(result.get("noneType")).isEqualTo("null");
    }
    
    // ==================== isinstance() ====================
    
    @Test
    @DisplayName("isinstance() checks type")
    void isinstance() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["isStr"] = isinstance("hello", "string")
                OUTPUT["isNum"] = isinstance(42, "number")
                OUTPUT["notStr"] = isinstance(42, "string")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        assertThat(result.get("isStr")).isEqualTo(true);
        assertThat(result.get("isNum")).isEqualTo(true);
        assertThat(result.get("notStr")).isEqualTo(false);
    }
    
    // ==================== hasattr() ====================
    
    @Test
    @DisplayName("hasattr() checks if dict has key")
    void hasattr() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                person = {"name": "John", "age": 30}
                OUTPUT["hasName"] = hasattr(person, "name")
                OUTPUT["hasEmail"] = hasattr(person, "email")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        assertThat(result.get("hasName")).isEqualTo(true);
        assertThat(result.get("hasEmail")).isEqualTo(false);
    }
    
    // ==================== getattr() ====================
    
    @Test
    @DisplayName("getattr() gets dict value with default")
    void getattr() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                person = {"name": "John"}
                OUTPUT["name"] = getattr(person, "name", "Unknown")
                OUTPUT["email"] = getattr(person, "email", "no-email")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(new HashMap<>(), Map.class);
        
        assertThat(result.get("name")).isEqualTo("John");
        assertThat(result.get("email")).isEqualTo("no-email");
    }
}
