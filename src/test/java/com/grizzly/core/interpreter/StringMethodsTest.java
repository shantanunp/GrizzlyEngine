package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for StringMethods - new string method implementations.
 */
class StringMethodsTest {
    
    // ==================== Case Transformation ====================
    
    @Test
    @DisplayName("capitalize() capitalizes first letter")
    void capitalize() {
        assertThat(evaluate("hello", "capitalize")).isEqualTo("Hello");
        assertThat(evaluate("HELLO", "capitalize")).isEqualTo("Hello");
        assertThat(evaluate("", "capitalize")).isEqualTo("");
    }
    
    @Test
    @DisplayName("title() title-cases words")
    void title() {
        assertThat(evaluate("hello world", "title")).isEqualTo("Hello World");
        assertThat(evaluate("HELLO WORLD", "title")).isEqualTo("Hello World");
    }
    
    @Test
    @DisplayName("swapcase() swaps case")
    void swapcase() {
        assertThat(evaluate("Hello World", "swapcase")).isEqualTo("hELLO wORLD");
    }
    
    @Test
    @DisplayName("islower() checks if all lowercase")
    void islower() {
        assertThat(evaluateBool("hello", "islower")).isTrue();
        assertThat(evaluateBool("Hello", "islower")).isFalse();
        assertThat(evaluateBool("hello123", "islower")).isTrue();
    }
    
    @Test
    @DisplayName("isupper() checks if all uppercase")
    void isupper() {
        assertThat(evaluateBool("HELLO", "isupper")).isTrue();
        assertThat(evaluateBool("Hello", "isupper")).isFalse();
    }
    
    @Test
    @DisplayName("isnumeric() checks if all numeric")
    void isnumeric() {
        assertThat(evaluateBool("12345", "isnumeric")).isTrue();
        assertThat(evaluateBool("123.45", "isnumeric")).isFalse();
        assertThat(evaluateBool("", "isnumeric")).isFalse();
    }
    
    // ==================== Splitting ====================
    
    @Test
    @DisplayName("splitlines() splits on line breaks")
    void splitlines() {
        Value result = StringMethods.evaluate(
            new StringValue("line1\nline2\nline3"), 
            "splitlines", 
            List.of()
        );
        assertThat(result).isInstanceOf(ListValue.class);
        ListValue list = (ListValue) result;
        assertThat(list.size()).isEqualTo(3);
        assertThat(((StringValue) list.get(0)).value()).isEqualTo("line1");
    }
    
    // ==================== Search ====================
    
    @Test
    @DisplayName("rfind() finds last occurrence")
    void rfind() {
        Value result = StringMethods.evaluate(
            new StringValue("hello hello"), 
            "rfind", 
            List.of(new StringValue("hello"))
        );
        assertThat(((NumberValue) result).asInt()).isEqualTo(6);
    }
    
    @Test
    @DisplayName("find() with start parameter")
    void findWithStart() {
        Value result = StringMethods.evaluate(
            new StringValue("hello hello"), 
            "find", 
            List.of(new StringValue("hello"), NumberValue.of(1))
        );
        assertThat(((NumberValue) result).asInt()).isEqualTo(6);
    }
    
    @Test
    @DisplayName("index() throws when not found")
    void indexNotFound() {
        assertThatThrownBy(() -> StringMethods.evaluate(
            new StringValue("hello"), 
            "index", 
            List.of(new StringValue("xyz"))
        )).isInstanceOf(GrizzlyExecutionException.class)
          .hasMessageContaining("not found");
    }
    
    // ==================== Padding ====================
    
    @Test
    @DisplayName("ljust() left-justifies with padding")
    void ljust() {
        assertThat(evaluate("hi", "ljust", NumberValue.of(5))).isEqualTo("hi   ");
        assertThat(evaluate("hello", "ljust", NumberValue.of(3))).isEqualTo("hello");
    }
    
    @Test
    @DisplayName("rjust() right-justifies with padding")
    void rjust() {
        assertThat(evaluate("hi", "rjust", NumberValue.of(5))).isEqualTo("   hi");
    }
    
    @Test
    @DisplayName("center() centers with padding")
    void center() {
        assertThat(evaluate("hi", "center", NumberValue.of(6))).isEqualTo("  hi  ");
    }
    
    @Test
    @DisplayName("ljust() with custom fill character")
    void ljustWithFill() {
        Value result = StringMethods.evaluate(
            new StringValue("hi"), 
            "ljust", 
            List.of(NumberValue.of(5), new StringValue("-"))
        );
        assertThat(((StringValue) result).value()).isEqualTo("hi---");
    }
    
    // ==================== Replace with count ====================
    
    @Test
    @DisplayName("replace() with max count")
    void replaceWithCount() {
        Value result = StringMethods.evaluate(
            new StringValue("aaa"), 
            "replace", 
            List.of(new StringValue("a"), new StringValue("b"), NumberValue.of(2))
        );
        assertThat(((StringValue) result).value()).isEqualTo("bba");
    }
    
    // ==================== startswith/endswith with range ====================
    
    @Test
    @DisplayName("startswith() with start parameter")
    void startswithWithStart() {
        Value result = StringMethods.evaluate(
            new StringValue("hello world"), 
            "startswith", 
            List.of(new StringValue("world"), NumberValue.of(6))
        );
        assertThat(((BoolValue) result).value()).isTrue();
    }
    
    @Test
    @DisplayName("endswith() with start/end parameters")
    void endswithWithRange() {
        Value result = StringMethods.evaluate(
            new StringValue("hello world"), 
            "endswith", 
            List.of(new StringValue("hello"), NumberValue.of(0), NumberValue.of(5))
        );
        assertThat(((BoolValue) result).value()).isTrue();
    }
    
    // ==================== Helper Methods ====================
    
    private String evaluate(String input, String method) {
        Value result = StringMethods.evaluate(new StringValue(input), method, List.of());
        return ((StringValue) result).value();
    }
    
    private String evaluate(String input, String method, Value... args) {
        Value result = StringMethods.evaluate(new StringValue(input), method, List.of(args));
        return ((StringValue) result).value();
    }
    
    private boolean evaluateBool(String input, String method) {
        Value result = StringMethods.evaluate(new StringValue(input), method, List.of());
        return ((BoolValue) result).value();
    }
}
