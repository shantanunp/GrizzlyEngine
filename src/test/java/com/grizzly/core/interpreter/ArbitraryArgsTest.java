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
 * Tests for *args (arbitrary argument lists) and * / ** unpacking.
 */
class ArbitraryArgsTest {

    private GrizzlyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GrizzlyEngine();
    }

    @Test
    @DisplayName("def concat(*args, sep='/') - variadic with keyword-only default")
    void concatStarArgsWithSep() throws Exception {
        String template = """
            def concat(*args, sep="/"):
                return sep.join(args)
            
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["a"] = concat("earth", "mars", "venus")
                OUTPUT["b"] = concat("earth", "mars", "venus", sep=".")
                return OUTPUT
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("a")).isEqualTo("earth/mars/venus");
        assertThat(result.get("b")).isEqualTo("earth.mars.venus");
    }

    @Test
    @DisplayName("def f(a, *args) - regular param then variadic")
    void regularThenStarArgs() throws Exception {
        String template = """
            def f(prefix, *rest):
                result = [prefix]
                for x in rest:
                    result.append(x)
                return result
            
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["r"] = f("a", "b", "c")
                return OUTPUT
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        @SuppressWarnings("unchecked")
        List<String> r = (List<String>) result.get("r");
        assertThat(r).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("range(*args) - unpack list to positional")
    void rangeUnpack() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                args = [3, 6]
                OUTPUT["r"] = list(range(*args))
                return OUTPUT
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        @SuppressWarnings("unchecked")
        List<Integer> r = (List<Integer>) result.get("r");
        assertThat(r).containsExactly(3, 4, 5);
    }

    @Test
    @DisplayName("parrot(**d) - unpack dict to keyword args")
    void dictUnpack() throws Exception {
        String template = """
            def parrot(voltage, state="a stiff", action="voom"):
                return voltage + "-" + state + "-" + action
            
            def transform(INPUT):
                OUTPUT = {}
                d = {"voltage": "four million", "state": "bleedin demised", "action": "VOOM"}
                OUTPUT["r"] = parrot(**d)
                return OUTPUT
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("r")).isEqualTo("four million-bleedin demised-VOOM");
    }

    @Test
    @DisplayName("f(1, *[2,3], 4) - mixed positional and unpack")
    void mixedPositionalAndUnpack() throws Exception {
        String template = """
            def add_all(*args):
                total = 0
                for x in args:
                    total = total + x
                return total
            
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["r"] = add_all(1, *[2, 3], 4)
                return OUTPUT
            """;

        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(new HashMap<>());

        assertThat(result.get("r")).isEqualTo(10);
    }
}
