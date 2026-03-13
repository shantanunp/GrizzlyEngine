package com.grizzly.core.interpreter;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for match/case statement (Python 3.10+ style).
 */
class SwitchStatementTest {

    private GrizzlyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }

    @Test
    @DisplayName("match matches first case")
    void switchMatchesFirstCase() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                status = INPUT["status"]
                match status:
                    case "active":
                        OUTPUT["code"] = 1
                    case "pending":
                        OUTPUT["code"] = 2
                    case _:
                        OUTPUT["code"] = 0
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> input = Map.of("status", "active");
        Map<String, Object> result = compiled.executeRaw(input);
        assertThat(result.get("code")).isEqualTo(1);
    }

    @Test
    @DisplayName("match matches second case")
    void switchMatchesSecondCase() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                match INPUT["status"]:
                    case "active":
                        OUTPUT["code"] = 1
                    case "pending":
                        OUTPUT["code"] = 2
                    case _:
                        OUTPUT["code"] = 0
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> input = Map.of("status", "pending");
        Map<String, Object> result = compiled.executeRaw(input);
        assertThat(result.get("code")).isEqualTo(2);
    }

    @Test
    @DisplayName("match falls through to case _ when no case matches")
    void switchDefault() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                match INPUT["status"]:
                    case "active":
                        OUTPUT["code"] = 1
                    case "pending":
                        OUTPUT["code"] = 2
                    case _:
                        OUTPUT["code"] = 0
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> input = Map.of("status", "unknown");
        Map<String, Object> result = compiled.executeRaw(input);
        assertThat(result.get("code")).isEqualTo(0);
    }

    @Test
    @DisplayName("match with numeric cases")
    void switchNumericCases() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                x = INPUT["x"]
                match x:
                    case 1:
                        OUTPUT["label"] = "one"
                    case 2:
                        OUTPUT["label"] = "two"
                    case _:
                        OUTPUT["label"] = "other"
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        assertThat(compiled.executeRaw(Map.of("x", 1)).get("label")).isEqualTo("one");
        assertThat(compiled.executeRaw(Map.of("x", 2)).get("label")).isEqualTo("two");
        assertThat(compiled.executeRaw(Map.of("x", 99)).get("label")).isEqualTo("other");
    }

    @Test
    @DisplayName("match without case _ does nothing when no match")
    void switchNoDefault() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["code"] = -1
                match INPUT["k"]:
                    case "a":
                        OUTPUT["code"] = 1
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(Map.of("k", "b"));
        assertThat(result.get("code")).isEqualTo(-1);
    }

    // --- match / case _ (Python 3.10+ style) ---

    @Test
    @DisplayName("match with case _ as default (ex1 style)")
    void matchCaseUnderscoreDefault() throws Exception {
        String template = """
            def get_status_message(status_code):
                match status_code:
                    case 200:
                        return "OK"
                    case 404:
                        return "Not Found"
                    case 500:
                        return "Internal Server Error"
                    case _:
                        return "Unknown Status"
            
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["msg"] = get_status_message(INPUT["code"])
                return OUTPUT
            """;
        GrizzlyTemplate compiled = engine.compile(template);
        assertThat(compiled.executeRaw(Map.of("code", 200)).get("msg")).isEqualTo("OK");
        assertThat(compiled.executeRaw(Map.of("code", 404)).get("msg")).isEqualTo("Not Found");
        assertThat(compiled.executeRaw(Map.of("code", 500)).get("msg")).isEqualTo("Internal Server Error");
        assertThat(compiled.executeRaw(Map.of("code", 999)).get("msg")).isEqualTo("Unknown Status");
    }
}
