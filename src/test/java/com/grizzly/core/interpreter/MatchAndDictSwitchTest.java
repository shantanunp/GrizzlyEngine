package com.grizzly.core.interpreter;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Python-style patterns: match/case _, dict.get as switch, comprehensions (ex2, ex3, ex4).
 */
class MatchAndDictSwitchTest {

    private GrizzlyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }

    @Test
    @DisplayName("ex2: dict.get(key, default) as switch")
    void dictGetAsSwitch() throws Exception {
        String template = """
            def get_status_message(status_code):
                status_map = {
                    200: "OK",
                    404: "Not Found",
                    500: "Internal Server Error"
                }
                return status_map.get(status_code, "Unknown Status")
            
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["msg"] = get_status_message(INPUT["code"])
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        assertThat(compiled.executeRaw(Map.of("code", 200)).get("msg")).isEqualTo("OK");
        assertThat(compiled.executeRaw(Map.of("code", 404)).get("msg")).isEqualTo("Not Found");
        assertThat(compiled.executeRaw(Map.of("code", 999)).get("msg")).isEqualTo("Unknown Status");
    }

    @Test
    @DisplayName("ex3: list comp with dict.get - [status_map.get(code, 'Unknown') for code in codes]")
    void listCompWithDictGet() throws Exception {
        String template = """
            def transform(INPUT):
                codes = [200, 404, 999, 500]
                status_map = {200: "OK", 404: "Not Found", 500: "Server Error"}
                messages = [status_map.get(code, "Unknown") for code in codes]
                OUTPUT = {}
                OUTPUT["messages"] = messages
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) compiled.executeRaw(Map.of()).get("messages");
        assertThat(messages).containsExactly("OK", "Not Found", "Unknown", "Server Error");
    }

    @Test
    @DisplayName("ex4: list comp with ternary - ['Even' if n % 2 == 0 else 'Odd' for n in numbers]")
    void listCompWithTernary() throws Exception {
        String template = """
            def transform(INPUT):
                numbers = [1, 2, 3, 4, 5]
                labels = ["Even" if n % 2 == 0 else "Odd" for n in numbers]
                OUTPUT = {}
                OUTPUT["labels"] = labels
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) compiled.executeRaw(Map.of()).get("labels");
        assertThat(labels).containsExactly("Odd", "Even", "Odd", "Even", "Odd");
    }
}
