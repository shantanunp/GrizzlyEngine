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
 * Tests for logical operators: and, or, not
 * and membership operators: in, not in
 */
class LogicalOperatorsTest {
    
    private GrizzlyEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }
    
    // ==================== 'and' Operator ====================
    
    @Test
    @DisplayName("'and' returns first falsy value")
    void andWithFalsy() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result"] = False and "never reached"
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("result")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("'and' returns last value if all truthy")
    void andWithTruthy() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result"] = True and "success"
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("result")).isEqualTo("success");
    }
    
    @Test
    @DisplayName("'and' with numbers")
    void andWithNumbers() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["both"] = 5 and 10
                OUTPUT["firstZero"] = 0 and 10
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("both")).isEqualTo(10);
        assertThat(result.get("firstZero")).isEqualTo(0);
    }
    
    // ==================== 'or' Operator ====================
    
    @Test
    @DisplayName("'or' returns first truthy value")
    void orWithTruthy() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result"] = "" or "default"
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("result")).isEqualTo("default");
    }
    
    @Test
    @DisplayName("'or' returns first value if truthy")
    void orFirstTruthy() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result"] = "first" or "second"
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("result")).isEqualTo("first");
    }
    
    @Test
    @DisplayName("'or' as default value pattern")
    void orAsDefault() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                name = INPUT.get("name") or "Unknown"
                OUTPUT["name"] = name
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("name")).isEqualTo("Unknown");
    }
    
    // ==================== 'not' Operator ====================
    
    @Test
    @DisplayName("'not' negates boolean")
    void notBoolean() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["notTrue"] = not True
                OUTPUT["notFalse"] = not False
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("notTrue")).isEqualTo(false);
        assertThat(result.get("notFalse")).isEqualTo(true);
    }
    
    @Test
    @DisplayName("'not' with falsy values")
    void notFalsyValues() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["notEmpty"] = not ""
                OUTPUT["notZero"] = not 0
                OUTPUT["notNone"] = not None
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("notEmpty")).isEqualTo(true);
        assertThat(result.get("notZero")).isEqualTo(true);
        assertThat(result.get("notNone")).isEqualTo(true);
    }
    
    // ==================== Combined Logical Operators ====================
    
    @Test
    @DisplayName("Combined logical operators with correct precedence")
    void combinedLogical() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result1"] = True or False and False
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        // 'and' has higher precedence than 'or'
        // True or (False and False) = True or False = True
        assertThat(result.get("result1")).isEqualTo(true);
    }
    
    // ==================== 'in' Operator ====================
    
    @Test
    @DisplayName("'in' checks membership in list")
    void inList() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = [1, 2, 3, 4, 5]
                OUTPUT["found"] = 3 in items
                OUTPUT["notFound"] = 10 in items
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("found")).isEqualTo(true);
        assertThat(result.get("notFound")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("'in' checks substring in string")
    void inString() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                text = "hello world"
                OUTPUT["found"] = "world" in text
                OUTPUT["notFound"] = "xyz" in text
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("found")).isEqualTo(true);
        assertThat(result.get("notFound")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("'in' checks key in dict")
    void inDict() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                person = {"name": "John", "age": 30}
                OUTPUT["hasName"] = "name" in person
                OUTPUT["hasEmail"] = "email" in person
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("hasName")).isEqualTo(true);
        assertThat(result.get("hasEmail")).isEqualTo(false);
    }
    
    // ==================== 'not in' Operator ====================
    
    @Test
    @DisplayName("'not in' is opposite of 'in'")
    void notIn() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = [1, 2, 3]
                OUTPUT["notInList"] = 5 not in items
                OUTPUT["inList"] = 2 not in items
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("notInList")).isEqualTo(true);
        assertThat(result.get("inList")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("'not in' in conditional")
    void notInConditional() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                allowed = ["admin", "user"]
                role = "guest"
                if role not in allowed:
                    OUTPUT["denied"] = True
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("denied")).isEqualTo(true);
    }
    
    // ==================== Practical Use Cases ====================
    
    @Test
    @DisplayName("Guard clause pattern using 'or' default")
    void guardClause() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                value = INPUT.get("value") or 0
                if value > 0:
                    OUTPUT["positive"] = True
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        
        // With positive value
        Map<String, Object> input1 = Map.of("value", 10);
        Map<String, Object> result1 = compiled.executeRaw(input1);
        assertThat(result1.get("positive")).isEqualTo(true);
        
        // With missing value - defaults to 0
        Map<String, Object> result2 = compiled.executeRaw(new HashMap<>());
        assertThat(result2.containsKey("positive")).isFalse();
    }
    
    // ==================== 'and'/'or' in if conditions ====================
    
    @Test
    @DisplayName("'and' works in if condition")
    void andInIfCondition() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                items = INPUT.get("items")
                if items and len(items) > 0:
                    OUTPUT["hasItems"] = True
                    OUTPUT["first"] = items[0]
                else:
                    OUTPUT["hasItems"] = False
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        
        // With items
        Map<String, Object> input1 = Map.of("items", List.of("a", "b", "c"));
        Map<String, Object> result1 = compiled.executeRaw(input1);
        assertThat(result1.get("hasItems")).isEqualTo(true);
        assertThat(result1.get("first")).isEqualTo("a");
        
        // Without items (null)
        Map<String, Object> result2 = compiled.executeRaw(new HashMap<>());
        assertThat(result2.get("hasItems")).isEqualTo(false);
        
        // With empty list
        Map<String, Object> input3 = Map.of("items", List.of());
        Map<String, Object> result3 = compiled.executeRaw(input3);
        assertThat(result3.get("hasItems")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("'or' works in if condition")
    void orInIfCondition() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                a = INPUT.get("a")
                b = INPUT.get("b")
                if a or b:
                    OUTPUT["hasValue"] = True
                else:
                    OUTPUT["hasValue"] = False
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        
        // Only a
        Map<String, Object> result1 = compiled.executeRaw(Map.of("a", "yes"));
        assertThat(result1.get("hasValue")).isEqualTo(true);
        
        // Only b
        Map<String, Object> result2 = compiled.executeRaw(Map.of("b", "yes"));
        assertThat(result2.get("hasValue")).isEqualTo(true);
        
        // Neither
        Map<String, Object> result3 = compiled.executeRaw(new HashMap<>());
        assertThat(result3.get("hasValue")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("'not' works in if condition")
    void notInIfCondition() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                disabled = INPUT.get("disabled")
                if not disabled:
                    OUTPUT["enabled"] = True
                else:
                    OUTPUT["enabled"] = False
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        
        // Not disabled (null/missing)
        Map<String, Object> result1 = compiled.executeRaw(new HashMap<>());
        assertThat(result1.get("enabled")).isEqualTo(true);
        
        // Explicitly disabled
        Map<String, Object> result2 = compiled.executeRaw(Map.of("disabled", true));
        assertThat(result2.get("enabled")).isEqualTo(false);
    }
    
    @Test
    @DisplayName("Complex condition with and/or/not")
    void complexCondition() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                active = INPUT.get("active")
                admin = INPUT.get("admin")
                banned = INPUT.get("banned")
                if active and admin or not banned:
                    OUTPUT["allowed"] = True
                else:
                    OUTPUT["allowed"] = False
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        
        // active and admin -> allowed
        Map<String, Object> result1 = compiled.executeRaw(Map.of("active", true, "admin", true, "banned", true));
        assertThat(result1.get("allowed")).isEqualTo(true);
        
        // not banned -> allowed
        Map<String, Object> result2 = compiled.executeRaw(Map.of("active", false, "admin", false, "banned", false));
        assertThat(result2.get("allowed")).isEqualTo(true);
        
        // banned and not (active and admin) -> not allowed
        Map<String, Object> result3 = compiled.executeRaw(Map.of("active", false, "admin", false, "banned", true));
        assertThat(result3.get("allowed")).isEqualTo(false);
    }
}
