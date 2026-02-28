package com.grizzly.parser.ast;

/**
 * Boolean literal: true or false
 * 
 * <p>Represents boolean values in the source code.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * OUTPUT["valid"] = true
 * OUTPUT["invalid"] = false
 * if true:
 *     ...
 * }</pre>
 * 
 * @param value The boolean value (true or false)
 */
public record BooleanLiteral(boolean value) implements Expression {
}
