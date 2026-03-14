package com.grizzly.core.interpreter;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for default argument values in function definitions.
 * Python-compliant: def f(prompt, retries=4, reminder='Please try again!')
 */
class DefaultArgumentsTest {

    private GrizzlyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }

    @Test
    @DisplayName("function with default args uses defaults when not provided")
    void usesDefaultsWhenNotProvided() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                result = greet("Alice")
                OUTPUT["result"] = result
                return OUTPUT

            def greet(name, punctuation="!"):
                return name + punctuation
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("result")).isEqualTo("Alice!");
    }

    @Test
    @DisplayName("function with default args overrides with positional")
    void overridesWithPositional() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                result = greet("Alice", "?")
                OUTPUT["result"] = result
                return OUTPUT

            def greet(name, punctuation="!"):
                return name + punctuation
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("result")).isEqualTo("Alice?");
    }

    @Test
    @DisplayName("function with default args overrides with keyword")
    void overridesWithKeyword() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                result = greet("Alice", punctuation="?")
                OUTPUT["result"] = result
                return OUTPUT

            def greet(name, punctuation="!"):
                return name + punctuation
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("result")).isEqualTo("Alice?");
    }

    @Test
    @DisplayName("multiple default args - all use defaults")
    void multipleDefaultsAllUsed() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                result = calc(10)
                OUTPUT["result"] = result
                return OUTPUT

            def calc(a, b=2, c=3):
                return a + b + c
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("result")).isEqualTo(15);  // 10 + 2 + 3
    }

    @Test
    @DisplayName("multiple default args - partial override")
    void multipleDefaultsPartialOverride() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                result = calc(10, 5)
                OUTPUT["result"] = result
                return OUTPUT

            def calc(a, b=2, c=3):
                return a + b + c
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("result")).isEqualTo(18);  // 10 + 5 + 3
    }

    @Test
    @DisplayName("default with string literal")
    void defaultWithStringLiteral() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                result = msg()
                OUTPUT["result"] = result
                return OUTPUT

            def msg(text="Hello!"):
                return text
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("result")).isEqualTo("Hello!");
    }

    @Test
    @DisplayName("default with numeric literal")
    void defaultWithNumericLiteral() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                result = double()
                OUTPUT["result"] = result
                return OUTPUT

            def double(n=21):
                return n * 2
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("result")).isEqualTo(42);
    }
}
