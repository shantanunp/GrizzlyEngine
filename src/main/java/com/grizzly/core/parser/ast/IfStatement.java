package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * If/elif/else statement with support for multiple elif branches.
 * 
 * <p><b>Simple if:</b>
 * <pre>{@code
 * if x > 0:
 *     status = "positive"
 * }</pre>
 * 
 * <p><b>If/else:</b>
 * <pre>{@code
 * if x > 0:
 *     status = "positive"
 * else:
 *     status = "negative"
 * }</pre>
 * 
 * <p><b>If/elif/else chain (NEW!):</b>
 * <pre>{@code
 * if x < 0:
 *     status = "negative"
 * elif x == 0:
 *     status = "zero"
 * elif x == 1:
 *     status = "one"
 * else:
 *     status = "many"
 * }</pre>
 * 
 * @param condition Main if condition
 * @param thenBlock Statements to execute if condition is true
 * @param elifBranches List of elif branches (condition + statements pairs)
 * @param elseBlock Statements to execute if all conditions are false (can be null)
 * @param lineNumber Source line number
 */
public record IfStatement(
    Expression condition, 
    List<Statement> thenBlock, 
    List<ElifBranch> elifBranches,
    List<Statement> elseBlock, 
    int lineNumber
) implements Statement {
    
    /**
     * Represents an elif branch (condition + statements).
     */
    public record ElifBranch(Expression condition, List<Statement> statements) {
    }
}
