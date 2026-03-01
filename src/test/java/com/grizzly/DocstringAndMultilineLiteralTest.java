package com.grizzly;

import com.grizzly.format.json.JsonTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for parser fixes: expression statements (docstrings) and multi-line dict/list literals.
 */
@DisplayName("Docstring and multi-line literal parsing")
class DocstringAndMultilineLiteralTest {

    @Nested
    @DisplayName("Expression statements (docstrings)")
    class Docstrings {

        @Test
        @DisplayName("function with docstring compiles and runs")
        void functionWithDocstring() {
            String template = """
                def transform(INPUT):
                    \"\"\"Main transformation\"\"\"
                    OUTPUT = {}
                    OUTPUT["id"] = INPUT.get("id", 0)
                    return OUTPUT
                """;
            JsonTemplate compiled = JsonTemplate.compile(template);
            String out = compiled.transform("{\"id\": 42}");
            assertThat(out).contains("\"id\"");
            assertThat(out).contains("42");
        }

        @Test
        @DisplayName("multiple functions with docstrings")
        void multipleFunctionsWithDocstrings() {
            String template = """
                def helper(INPUT, OUTPUT):
                    \"\"\"helper docstring\"\"\"
                    OUTPUT["fromHelper"] = True

                def transform(INPUT):
                    \"\"\"Main\"\"\"
                    OUTPUT = {}
                    helper(INPUT, OUTPUT)
                    return OUTPUT
                """;
            JsonTemplate compiled = JsonTemplate.compile(template);
            String out = compiled.transform("{}");
            assertThat(out).contains("\"fromHelper\"");
            assertThat(out).contains("true");
        }

        @Test
        @DisplayName("bare string literal as statement is accepted")
        void bareStringLiteral() {
            String template = """
                def transform(INPUT):
                    "just a docstring"
                    OUTPUT = {}
                    OUTPUT["ok"] = True
                    return OUTPUT
                """;
            JsonTemplate compiled = JsonTemplate.compile(template);
            String out = compiled.transform("{}");
            assertThat(out).contains("\"ok\"");
            assertThat(out).contains("true");
        }
    }

    @Nested
    @DisplayName("Multi-line dict literal")
    class MultilineDict {

        @Test
        @DisplayName("dict with newlines and indent parses and runs")
        void multilineDictLiteral() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    version = {
                        "version": "1.0.0",
                        "description": "Initial release",
                        "meta": {
                            "created": "2026-01-01"
                        }
                    }
                    OUTPUT["aboutVersions"] = version
                    return OUTPUT
                """;
            JsonTemplate compiled = JsonTemplate.compile(template);
            String out = compiled.transform("{}");
            assertThat(out).contains("\"aboutVersions\"");
            assertThat(out).contains("\"version\"");
            assertThat(out).contains("1.0.0");
            assertThat(out).contains("\"description\"");
            assertThat(out).contains("Initial release");
            assertThat(out).contains("\"meta\"");
            assertThat(out).contains("\"created\"");
            assertThat(out).contains("2026-01-01");
        }

        @Test
        @DisplayName("nested multi-line dict with trailing comma-style newline")
        void nestedMultilineDict() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {
                        "a": 1,
                        "b": {
                            "x": 10,
                            "y": 20
                        }
                    }
                    return OUTPUT
                """;
            JsonTemplate compiled = JsonTemplate.compile(template);
            String out = compiled.transform("{}");
            assertThat(out).contains("\"a\"");
            assertThat(out).contains("1");
            assertThat(out).contains("\"b\"");
            assertThat(out).contains("\"x\"");
            assertThat(out).contains("10");
            assertThat(out).contains("\"y\"");
            assertThat(out).contains("20");
        }
    }

    @Nested
    @DisplayName("Multi-line list literal")
    class MultilineList {

        @Test
        @DisplayName("list with newlines and indent parses and runs")
        void multilineListLiteral() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    items = [
                        1,
                        2,
                        3
                    ]
                    OUTPUT["count"] = len(items)
                    OUTPUT["first"] = items[0]
                    return OUTPUT
                """;
            JsonTemplate compiled = JsonTemplate.compile(template);
            String out = compiled.transform("{}");
            assertThat(out).contains("\"count\"");
            assertThat(out).contains("3");
            assertThat(out).contains("\"first\"");
            assertThat(out).contains("1");
        }
    }

    @Nested
    @DisplayName("Combined: docstrings + multi-line dict (like examples/transform.py)")
    class Combined {

        @Test
        @DisplayName("template with docstrings and multi-line dict compiles and runs")
        void docstringsAndMultilineDict() {
            String template = """
                def map_module_1(INPUT, OUTPUT):
                    \"\"\"module_1 mappings\"\"\"
                    OUTPUT["profile"] = {}
                    OUTPUT["profile"]["fullName"] = INPUT.get("name", "")

                def transform(INPUT):
                    \"\"\"Main transformation\"\"\"
                    OUTPUT = {}
                    OUTPUT["clientReference"] = INPUT.get("customerId", "")
                    map_module_1(INPUT, OUTPUT)
                    version = {
                        "version": "1.0.0",
                        "description": "Initial release",
                        "createDatetime": {
                            "datetime": "2026-01-01T00:00:00"
                        }
                    }
                    OUTPUT["aboutVersions"] = version
                    return OUTPUT
                """;
            JsonTemplate compiled = JsonTemplate.compile(template);
            String out = compiled.transform("{\"name\": \"Jane\", \"customerId\": \"C001\"}");
            assertThat(out).contains("\"clientReference\"");
            assertThat(out).contains("C001");
            assertThat(out).contains("\"profile\"");
            assertThat(out).contains("\"fullName\"");
            assertThat(out).contains("Jane");
            assertThat(out).contains("\"aboutVersions\"");
            assertThat(out).contains("\"version\"");
            assertThat(out).contains("1.0.0");
            assertThat(out).contains("\"createDatetime\"");
        }
    }
}
