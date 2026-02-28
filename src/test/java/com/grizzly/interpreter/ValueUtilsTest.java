package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ValueUtils utility methods.
 */
class ValueUtilsTest {
    
    // ==================== asString() Tests ====================
    
    @Test
    @DisplayName("asString converts StringValue to string")
    void asString_StringValue() {
        assertThat(ValueUtils.asString(new StringValue("hello"))).isEqualTo("hello");
    }
    
    @Test
    @DisplayName("asString converts NumberValue to string")
    void asString_NumberValue() {
        assertThat(ValueUtils.asString(NumberValue.of(42))).isEqualTo("42");
        assertThat(ValueUtils.asString(NumberValue.of(3.14))).isEqualTo("3.14");
    }
    
    @Test
    @DisplayName("asString converts BoolValue to string")
    void asString_BoolValue() {
        assertThat(ValueUtils.asString(BoolValue.TRUE)).isEqualTo("True");
        assertThat(ValueUtils.asString(BoolValue.FALSE)).isEqualTo("False");
    }
    
    @Test
    @DisplayName("asString converts NullValue to 'None'")
    void asString_NullValue() {
        assertThat(ValueUtils.asString(NullValue.INSTANCE)).isEqualTo("None");
    }
    
    // ==================== toInt() Tests ====================
    
    @Test
    @DisplayName("toInt converts NumberValue to int")
    void toInt_NumberValue() {
        assertThat(ValueUtils.toInt(NumberValue.of(42))).isEqualTo(42);
        assertThat(ValueUtils.toInt(NumberValue.of(3.9))).isEqualTo(3);
    }
    
    @Test
    @DisplayName("toInt converts StringValue to int")
    void toInt_StringValue() {
        assertThat(ValueUtils.toInt(new StringValue("42"))).isEqualTo(42);
        assertThat(ValueUtils.toInt(new StringValue("3.9"))).isEqualTo(3);
    }
    
    @Test
    @DisplayName("toInt throws for non-numeric string")
    void toInt_InvalidString() {
        assertThatThrownBy(() -> ValueUtils.toInt(new StringValue("abc")))
            .isInstanceOf(GrizzlyExecutionException.class)
            .hasMessageContaining("Cannot convert");
    }
    
    // ==================== toDouble() Tests ====================
    
    @Test
    @DisplayName("toDouble converts NumberValue to double")
    void toDouble_NumberValue() {
        assertThat(ValueUtils.toDouble(NumberValue.of(42))).isEqualTo(42.0);
        assertThat(ValueUtils.toDouble(NumberValue.of(3.14))).isEqualTo(3.14);
    }
    
    @Test
    @DisplayName("toDouble converts StringValue to double")
    void toDouble_StringValue() {
        assertThat(ValueUtils.toDouble(new StringValue("3.14"))).isEqualTo(3.14);
    }
    
    @Test
    @DisplayName("toDouble converts DecimalValue to double")
    void toDouble_DecimalValue() {
        assertThat(ValueUtils.toDouble(new DecimalValue("3.14159"))).isCloseTo(3.14159, within(0.00001));
    }
    
    // ==================== toLong() Tests ====================
    
    @Test
    @DisplayName("toLong converts NumberValue to long")
    void toLong_NumberValue() {
        assertThat(ValueUtils.toLong(NumberValue.of(1234567890123L))).isEqualTo(1234567890123L);
    }
    
    // ==================== areEqual() Tests ====================
    
    @Test
    @DisplayName("areEqual compares numbers with tolerance")
    void areEqual_Numbers() {
        assertThat(ValueUtils.areEqual(NumberValue.of(1), NumberValue.of(1))).isTrue();
        assertThat(ValueUtils.areEqual(NumberValue.of(1.0), NumberValue.of(1))).isTrue();
        assertThat(ValueUtils.areEqual(NumberValue.of(1), NumberValue.of(2))).isFalse();
    }
    
    @Test
    @DisplayName("areEqual compares string to number with coercion")
    void areEqual_StringToNumber() {
        assertThat(ValueUtils.areEqual(new StringValue("42"), NumberValue.of(42))).isTrue();
        assertThat(ValueUtils.areEqual(NumberValue.of(42), new StringValue("42"))).isTrue();
        assertThat(ValueUtils.areEqual(new StringValue("abc"), NumberValue.of(42))).isFalse();
    }
    
    @Test
    @DisplayName("areEqual compares strings")
    void areEqual_Strings() {
        assertThat(ValueUtils.areEqual(new StringValue("hello"), new StringValue("hello"))).isTrue();
        assertThat(ValueUtils.areEqual(new StringValue("hello"), new StringValue("world"))).isFalse();
    }
    
    @Test
    @DisplayName("areEqual compares booleans")
    void areEqual_Booleans() {
        assertThat(ValueUtils.areEqual(BoolValue.TRUE, BoolValue.TRUE)).isTrue();
        assertThat(ValueUtils.areEqual(BoolValue.TRUE, BoolValue.FALSE)).isFalse();
    }
    
    // ==================== Argument Validation Tests ====================
    
    @Test
    @DisplayName("requireArgCount passes for correct count")
    void requireArgCount_Correct() {
        List<Value> args = List.of(new StringValue("a"), new StringValue("b"));
        assertThatCode(() -> ValueUtils.requireArgCount("test", args, 2))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("requireArgCount throws for incorrect count")
    void requireArgCount_Incorrect() {
        List<Value> args = List.of(new StringValue("a"));
        assertThatThrownBy(() -> ValueUtils.requireArgCount("test", args, 2))
            .isInstanceOf(GrizzlyExecutionException.class)
            .hasMessageContaining("takes exactly 2 argument(s), got 1");
    }
    
    @Test
    @DisplayName("requireArgCountRange passes for valid range")
    void requireArgCountRange_Valid() {
        List<Value> args = List.of(new StringValue("a"), new StringValue("b"));
        assertThatCode(() -> ValueUtils.requireArgCountRange("test", args, 1, 3))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("requireArgCountRange throws for out of range")
    void requireArgCountRange_Invalid() {
        List<Value> args = List.of(new StringValue("a"));
        assertThatThrownBy(() -> ValueUtils.requireArgCountRange("test", args, 2, 3))
            .isInstanceOf(GrizzlyExecutionException.class)
            .hasMessageContaining("takes 2-3 arguments");
    }
    
    @Test
    @DisplayName("requireType passes and returns value for correct type")
    void requireType_Correct() {
        StringValue str = new StringValue("test");
        StringValue result = ValueUtils.requireType("func", str, StringValue.class, "arg");
        assertThat(result).isSameAs(str);
    }
    
    @Test
    @DisplayName("requireType throws for incorrect type")
    void requireType_Incorrect() {
        assertThatThrownBy(() -> 
            ValueUtils.requireType("func", NumberValue.of(1), StringValue.class, "arg"))
            .isInstanceOf(GrizzlyExecutionException.class)
            .hasMessageContaining("must be a string, got: number");
    }
    
    // ==================== getTypeName() Tests ====================
    
    @Test
    @DisplayName("getTypeName returns correct type names")
    void getTypeName_AllTypes() {
        assertThat(ValueUtils.getTypeName(StringValue.class)).isEqualTo("string");
        assertThat(ValueUtils.getTypeName(NumberValue.class)).isEqualTo("number");
        assertThat(ValueUtils.getTypeName(BoolValue.class)).isEqualTo("bool");
        assertThat(ValueUtils.getTypeName(ListValue.class)).isEqualTo("list");
        assertThat(ValueUtils.getTypeName(DictValue.class)).isEqualTo("dict");
        assertThat(ValueUtils.getTypeName(NullValue.class)).isEqualTo("null");
    }
}
