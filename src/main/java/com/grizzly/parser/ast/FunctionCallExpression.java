package com.grizzly.parser.ast;

import java.util.List;

/**
 * Function call as an expression: len(items), helper(x)
 * 
 * This is different from FunctionCall which is a statement.
 * This allows function calls to be used in expressions like:
 *   count = len(items)
 *   result = helper(INPUT)
 */
/**
 * AST node representing a function call used as an expression.
 * 
 * <p>This differs from {@link FunctionCall} (statement) in that it appears in
 * expression contexts like assignments, method arguments, or conditions.
 * 
 * <p><b>Usage contexts:</b>
 * <ul>
 *   <li>Assignment: {@code result = len(items)}</li>
 *   <li>Method argument: {@code items.extend(helper(INPUT))}</li>
 *   <li>List element: {@code sizes = [len(a), len(b)]}</li>
 *   <li>Condition: {@code if len(items) > 0:}</li>
 * </ul>
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * count = len(INPUT.customers)        // Built-in function
 * result = helper(INPUT)              // User-defined function
 * total = len(process(INPUT.data))   // Nested calls
 * }</pre>
 * 
 * @param functionName Name of the function to call (built-in or user-defined)
 * @param args List of argument expressions to evaluate and pass
 */
public record FunctionCallExpression(String functionName, List<Expression> args) implements Expression {
}
