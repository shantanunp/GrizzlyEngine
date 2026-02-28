package com.grizzly.core.interpreter;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import com.grizzly.core.types.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Python-style literals: True, False, None, and dictionary literals.
 */
class PythonLiteralsTest {
    
    private GrizzlyEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }
    
    // ==================== Boolean Literals ====================
    
    @Test
    @DisplayName("True literal is parsed and evaluates correctly")
    void trueLiteral() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["flag"] = True
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("flag")).isEqualTo(true);
    }
    
    @Test
    @DisplayName("False literal is parsed and evaluates correctly")
    void falseLiteral() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["flag"] = False
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("flag")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("Boolean literals can be used in conditions")
    void booleansInConditions() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                if True:
                    OUTPUT["always"] = "yes"
                if False:
                    OUTPUT["never"] = "yes"
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("always")).isEqualTo("yes");
        assertThat(result.containsKey("never")).isFalse();
    }
    
    // ==================== None Literal ====================
    
    @Test
    @DisplayName("None literal is parsed and evaluates correctly")
    void noneLiteral() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["nothing"] = None
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("nothing")).isNull();
    }
    
    @Test
    @DisplayName("None comparison works correctly")
    void noneComparison() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                value = None
                if value == None:
                    OUTPUT["isNone"] = True
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("isNone")).isEqualTo(true);
    }
    
    @Test
    @DisplayName("None is falsy")
    void noneIsFalsy() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                value = None
                isFalsy = not value
                OUTPUT["noneFalsy"] = isFalsy
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("noneFalsy")).isEqualTo(true);
    }
    
    // ==================== Dictionary Literals ====================
    
    @Test
    @DisplayName("Empty dict literal creates empty dictionary")
    void emptyDictLiteral() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["empty"] = {}
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("empty")).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) result.get("empty")).isEmpty();
    }
    
    @Test
    @DisplayName("Dict literal with key-value pairs")
    void dictLiteralWithPairs() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                person = {"name": "John", "age": 30}
                OUTPUT["person"] = person
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> person = (Map<String, Object>) result.get("person");
        assertThat(person.get("name")).isEqualTo("John");
        assertThat(person.get("age")).isEqualTo(30);
    }
    
    @Test
    @DisplayName("Nested dict literals")
    void nestedDictLiterals() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["nested"] = {"outer": {"inner": "value"}}
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) result.get("nested");
        @SuppressWarnings("unchecked")
        Map<String, Object> outer = (Map<String, Object>) nested.get("outer");
        assertThat(outer.get("inner")).isEqualTo("value");
    }
    
    @Test
    @DisplayName("Dict literal with expressions as values")
    void dictLiteralWithExpressions() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                x = 10
                y = 20
                OUTPUT["sums"] = {"a": x + 5, "b": y * 2}
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sums = (Map<String, Object>) result.get("sums");
        assertThat(sums.get("a")).isEqualTo(15);
        assertThat(sums.get("b")).isEqualTo(40);
    }
    
    @Test
    @DisplayName("Dict literal can access dict methods")
    void dictLiteralMethods() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                person = {"name": "John", "age": 30}
                OUTPUT["keys"] = person.keys()
                OUTPUT["hasName"] = person.get("name")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("keys")).isInstanceOf(List.class);
        assertThat(result.get("hasName")).isEqualTo("John");
    }
}
