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
 * Tests for lambda expressions - anonymous functions.
 * 
 * <p>Python-compatible lambda syntax: {@code lambda params: expr}
 */
class LambdaTest {
    
    private GrizzlyEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }
    
    @Test
    @DisplayName("lambda returns lambda - closure captures n")
    void lambdaClosure() throws Exception {
        // Use lambda instead of nested def (nested def not yet supported)
        String template = """
            def transform(INPUT):
                make_incrementor = lambda n: (lambda x: x + n)
                f = make_incrementor(42)
                OUTPUT = {}
                OUTPUT["f0"] = f(0)
                OUTPUT["f1"] = f(1)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("f0")).isEqualTo(42);
        assertThat(result.get("f1")).isEqualTo(43);
    }
    
    @Test
    @DisplayName("(lambda x: x+1)(5) - direct lambda invocation")
    void directLambdaCall() throws Exception {
        String template = """
            def transform(INPUT):
                result = (lambda x: x + 1)(5)
                OUTPUT = {}
                OUTPUT["result"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("result")).isEqualTo(6);
    }
    
    @Test
    @DisplayName("lambda a, b: a+b - multiple parameters")
    void lambdaMultipleParams() throws Exception {
        String template = """
            def transform(INPUT):
                add = lambda a, b: a + b
                OUTPUT = {}
                OUTPUT["sum"] = add(3, 7)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        assertThat(result.get("sum")).isEqualTo(10);
    }
    
    @Test
    @DisplayName("list.sort(key=lambda pair: pair[1]) - sort by second element")
    void sortWithKey() throws Exception {
        String template = """
            def transform(INPUT):
                pairs = [[1, "one"], [2, "two"], [3, "three"], [4, "four"]]
                pairs.sort(key=lambda pair: pair[1])
                OUTPUT = {}
                OUTPUT["pairs"] = pairs
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<List<Object>> pairs = (List<List<Object>>) result.get("pairs");
        assertThat(pairs).hasSize(4);
        // Sorted by string: four, one, three, two
        assertThat(pairs.get(0)).containsExactly(4, "four");
        assertThat(pairs.get(1)).containsExactly(1, "one");
        assertThat(pairs.get(2)).containsExactly(3, "three");
        assertThat(pairs.get(3)).containsExactly(2, "two");
    }
    
    @Test
    @DisplayName("sorted(iterable, key=lambda x: x) - builtin sorted with key")
    void sortedWithKey() throws Exception {
        String template = """
            def transform(INPUT):
                pairs = [[1, "one"], [2, "two"], [3, "three"], [4, "four"]]
                result = sorted(pairs, key=lambda pair: pair[1])
                OUTPUT = {}
                OUTPUT["result"] = result
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<List<Object>> sorted = (List<List<Object>>) result.get("result");
        assertThat(sorted).hasSize(4);
        assertThat(sorted.get(0)).containsExactly(4, "four");
        assertThat(sorted.get(1)).containsExactly(1, "one");
        assertThat(sorted.get(2)).containsExactly(3, "three");
        assertThat(sorted.get(3)).containsExactly(2, "two");
    }
}
