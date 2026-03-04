package com.grizzly;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for newer syntax: ternary, list comprehension, f-strings, grouped expressions.
 * Uses distinct examples (quiz scores, products, greetings, arithmetic).
 */
class ComprehensionsTest {

    private GrizzlyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }

    @Nested
    @DisplayName("Ternary (conditional expression)")
    class TernaryTests {

        @Test
        @DisplayName("simple ternary chooses then-branch when condition is true")
        void ternaryThenBranch() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["result"] = "pass" if INPUT.score >= 60 else "fail"
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = new HashMap<>();
            input.put("score", 70);
            Map<String, Object> result = compiled.executeRaw(input);
            assertThat(result).containsEntry("result", "pass");
        }

        @Test
        @DisplayName("simple ternary chooses else-branch when condition is false")
        void ternaryElseBranch() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["result"] = "pass" if INPUT.score >= 60 else "fail"
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = new HashMap<>();
            input.put("score", 50);
            Map<String, Object> result = compiled.executeRaw(input);
            assertThat(result).containsEntry("result", "fail");
        }

        @Test
        @DisplayName("nested ternary")
        void nestedTernary() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["grade"] = "A" if INPUT.points >= 90 else "B" if INPUT.points >= 70 else "C"
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> aInput = Map.of("points", 95);
            assertThat(compiled.executeRaw(aInput)).containsEntry("grade", "A");
            Map<String, Object> bInput = Map.of("points", 75);
            assertThat(compiled.executeRaw(bInput)).containsEntry("grade", "B");
            Map<String, Object> cInput = Map.of("points", 60);
            assertThat(compiled.executeRaw(cInput)).containsEntry("grade", "C");
        }
    }

    @Nested
    @DisplayName("List comprehension")
    class ListComprehensionTests {

        @Test
        @DisplayName("list comprehension doubles each number")
        void listCompDouble() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["doubled"] = [n * 2 for n in INPUT.nums]
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = new HashMap<>();
            input.put("nums", List.of(1, 2, 3));
            Map<String, Object> result = compiled.executeRaw(input);
            @SuppressWarnings("unchecked")
            List<Object> doubled = (List<Object>) result.get("doubled");
            assertThat(doubled).containsExactly(2, 4, 6);
        }

        @Test
        @DisplayName("list comprehension uppercases strings")
        void listCompUpper() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["tags"] = [s.upper() for s in INPUT.words]
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = new HashMap<>();
            input.put("words", List.of("alpha", "beta"));
            Map<String, Object> result = compiled.executeRaw(input);
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) result.get("tags");
            assertThat(tags).containsExactly("ALPHA", "BETA");
        }

        @Test
        @DisplayName("list comprehension over optional list uses empty when null")
        void listCompOptionalList() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["doubled"] = [x * 2 for x in (INPUT.nums or [])]
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> withNums = new HashMap<>();
            withNums.put("nums", List.of(5, 10));
            List<?> doubledWith = (List<?>) compiled.executeRaw(withNums).get("doubled");
            assertThat(doubledWith).hasSize(2);
            assertThat(((Number) doubledWith.get(0)).intValue()).isEqualTo(10);
            assertThat(((Number) doubledWith.get(1)).intValue()).isEqualTo(20);
            Map<String, Object> noNums = new HashMap<>();
            noNums.put("nums", null);
            assertThat(((List<?>) compiled.executeRaw(noNums).get("doubled"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("F-strings")
    class FStringTests {

        @Test
        @DisplayName("f-string interpolates single expression")
        void fStringSingle() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["greeting"] = f"Score: {INPUT.points} points"
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = Map.of("points", 100);
            Map<String, Object> result = compiled.executeRaw(input);
            assertThat(result).containsEntry("greeting", "Score: 100 points");
        }

        @Test
        @DisplayName("f-string interpolates number and literal")
        void fStringNumber() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["message"] = f"You have {INPUT.count} items"
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = Map.of("count", 3);
            Map<String, Object> result = compiled.executeRaw(input);
            assertThat(result).containsEntry("message", "You have 3 items");
        }

        @Test
        @DisplayName("f-string with multiple placeholders")
        void fStringMultiple() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["line"] = f"{INPUT.a}-{INPUT.b}-{INPUT.c}"
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = new HashMap<>();
            input.put("a", 10);
            input.put("b", 20);
            input.put("c", 30);
            Map<String, Object> result = compiled.executeRaw(input);
            assertThat(result).containsEntry("line", "10-20-30");
        }
    }

    @Nested
    @DisplayName("Grouped expressions")
    class GroupedExpressionTests {

        @Test
        @DisplayName("parentheses change arithmetic order")
        void groupedArithmetic() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["value"] = (INPUT.x + INPUT.y) * INPUT.z
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            Map<String, Object> input = new HashMap<>();
            input.put("x", 1);
            input.put("y", 2);
            input.put("z", 3);
            Map<String, Object> result = compiled.executeRaw(input);
            assertThat(result).containsEntry("value", 9);
        }

        @Test
        @DisplayName("grouped expression in condition")
        void groupedInCondition() throws Exception {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["ok"] = "yes" if (INPUT.a and INPUT.b) else "no"
                    return OUTPUT
                """;
            GrizzlyTemplate compiled = engine.compile(template);
            assertThat(compiled.executeRaw(Map.of("a", true, "b", true))).containsEntry("ok", "yes");
            assertThat(compiled.executeRaw(Map.of("a", true, "b", false))).containsEntry("ok", "no");
        }
    }
}
