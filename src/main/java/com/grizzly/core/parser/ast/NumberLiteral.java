package com.grizzly.core.parser.ast;

/**
 * Numeric literal expression.
 * 
 * <p>Represents integer or floating-point numbers in the source code.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * 42       → NumberLiteral(42)
 * 3.14     → NumberLiteral(3.14)
 * -5       → NumberLiteral(-5)
 * 0        → NumberLiteral(0)
 * }</pre>
 * 
 * @param value The numeric value (Integer or Double)
 */
public record NumberLiteral(Number value) implements Expression {
}
