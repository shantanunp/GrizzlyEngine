package com.grizzly.core.parser.ast;

/**
 * Expression statement: an expression used as a statement
 * 
 * Example:
 *   items.append(value)  ← MethodCall used as a statement
 *   someFunction()       ← FunctionCall used as a statement
 */
/**
 * Expression statement: an expression used as a statement.
 * 
 * <p>Allows expressions (like method calls) to be used as standalone statements.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * items.append(value)      // MethodCall used as statement
 * someFunction()           // FunctionCall used as statement (though FunctionCall is already a statement)
 * }</pre>
 * 
 * <p>The expression is evaluated for its side effects; its return value is discarded.
 * 
 * @param expression The expression to evaluate
 * @param lineNumber Source line number for error reporting
 */
public record ExpressionStatement(Expression expression, int lineNumber) implements Statement {
}
