package com.grizzly;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

/**
 * Tests for Python Section 4 features:
 * - elif support ✅
 * - range() function ✅
 * - break/continue statements ✅
 */
class Section4FeaturesTest {
    
    private final GrizzlyEngine engine = new GrizzlyEngine();
    
    // ========== ELIF TESTS ==========
    
    @Test
    void testElifSimple() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                x = INPUT.value
                
                if x < 0:
                    OUTPUT["status"] = "negative"
                elif x == 0:
                    OUTPUT["status"] = "zero"
                else:
                    OUTPUT["status"] = "positive"
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        Map<String, Object> result1 = compiled.execute(Map.of("value", -5), Map.class);
        assertThat(result1.get("status")).isEqualTo("negative");
        
        Map<String, Object> result2 = compiled.execute(Map.of("value", 0), Map.class);
        assertThat(result2.get("status")).isEqualTo("zero");
        
        Map<String, Object> result3 = compiled.execute(Map.of("value", 5), Map.class);
        assertThat(result3.get("status")).isEqualTo("positive");
    }
    
    @Test
    void testElifMultiple() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                score = INPUT.score
                
                if score >= 90:
                    OUTPUT["grade"] = "A"
                elif score >= 80:
                    OUTPUT["grade"] = "B"
                elif score >= 70:
                    OUTPUT["grade"] = "C"
                elif score >= 60:
                    OUTPUT["grade"] = "D"
                else:
                    OUTPUT["grade"] = "F"
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        assertThat(compiled.execute(Map.of("score", 95), Map.class).get("grade")).isEqualTo("A");
        assertThat(compiled.execute(Map.of("score", 85), Map.class).get("grade")).isEqualTo("B");
        assertThat(compiled.execute(Map.of("score", 75), Map.class).get("grade")).isEqualTo("C");
        assertThat(compiled.execute(Map.of("score", 65), Map.class).get("grade")).isEqualTo("D");
        assertThat(compiled.execute(Map.of("score", 50), Map.class).get("grade")).isEqualTo("F");
    }
    
    @Test
    void testElifNoElse() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                x = INPUT.value
                
                if x < 0:
                    OUTPUT["result"] = "negative"
                elif x == 0:
                    OUTPUT["result"] = "zero"
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        Map<String, Object> result1 = compiled.execute(Map.of("value", -1), Map.class);
        assertThat(result1.get("result")).isEqualTo("negative");
        
        Map<String, Object> result2 = compiled.execute(Map.of("value", 0), Map.class);
        assertThat(result2.get("result")).isEqualTo("zero");
        
        Map<String, Object> result3 = compiled.execute(Map.of("value", 5), Map.class);
        assertThat(result3.get("result")).isNull();
    }
    
    // ========== RANGE TESTS ==========
    
    @Test
    void testRangeSingleArg() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["numbers"] = range(5)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> numbers = (List<Object>) result.get("numbers");
        assertThat(numbers).containsExactly(0, 1, 2, 3, 4);
    }
    
    @Test
    void testRangeTwoArgs() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["numbers"] = range(2, 7)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> numbers = (List<Object>) result.get("numbers");
        assertThat(numbers).containsExactly(2, 3, 4, 5, 6);
    }
    
    @Test
    void testRangeThreeArgs() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["evens"] = range(0, 10, 2)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> numbers = (List<Object>) result.get("evens");
        assertThat(numbers).containsExactly(0, 2, 4, 6, 8);
    }
    
    @Test
    void testRangeNegativeStep() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["countdown"] = range(10, 0, -2)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> numbers = (List<Object>) result.get("countdown");
        assertThat(numbers).containsExactly(10, 8, 6, 4, 2);
    }
    
    @Test
    void testRangeInForLoop() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["squares"] = []
                
                for i in range(5):
                    OUTPUT["squares"].append(i * i)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> squares = (List<Object>) result.get("squares");
        assertThat(squares).containsExactly(0, 1, 4, 9, 16);
    }
    
    // ========== BREAK TESTS ==========
    
    @Test
    void testBreakInFor() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["items"] = []
                
                for i in range(10):
                    if i == 5:
                        break
                    OUTPUT["items"].append(i)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) result.get("items");
        assertThat(items).containsExactly(0, 1, 2, 3, 4);
    }
    
    @Test
    void testBreakNested() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["pairs"] = []
                
                for i in range(3):
                    for j in range(5):
                        if j == 2:
                            break
                        OUTPUT["pairs"].append(i * 10 + j)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> pairs = (List<Object>) result.get("pairs");
        
        // i=0: j=0,1 → 0,1
        // i=1: j=0,1 → 10,11
        // i=2: j=0,1 → 20,21
        assertThat(pairs).containsExactly(0, 1, 10, 11, 20, 21);
    }
    
    // ========== CONTINUE TESTS ==========
    
    @Test
    void testContinueInFor() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["odds"] = []
                
                for i in range(10):
                    if i % 2 == 0:
                        continue
                    OUTPUT["odds"].append(i)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> odds = (List<Object>) result.get("odds");
        assertThat(odds).containsExactly(1, 3, 5, 7, 9);
    }
    
    @Test
    void testContinueNested() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["filtered"] = []
                
                for i in range(3):
                    for j in range(5):
                        if j == 2:
                            continue
                        OUTPUT["filtered"].append(i * 10 + j)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> filtered = (List<Object>) result.get("filtered");
        
        // i=0: j=0,1,3,4 → 0,1,3,4
        // i=1: j=0,1,3,4 → 10,11,13,14
        // i=2: j=0,1,3,4 → 20,21,23,24
        assertThat(filtered).containsExactly(0, 1, 3, 4, 10, 11, 13, 14, 20, 21, 23, 24);
    }
    
    @Test
    void testBreakAndContinueCombined() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["numbers"] = []
                
                for i in range(20):
                    if i % 2 == 0:
                        continue
                    if i > 10:
                        break
                    OUTPUT["numbers"].append(i)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.execute(Map.of(), Map.class);
        
        @SuppressWarnings("unchecked")
        List<Object> numbers = (List<Object>) result.get("numbers");
        assertThat(numbers).containsExactly(1, 3, 5, 7, 9);
    }
}
