package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * List literal expression: [1, 2, 3] or []
 * 
 * Example:
 *   items = []
 *   items = [1, 2, 3]
 *   items = [INPUT.x, INPUT.y]
 */
/**
 * AST node representing a list literal expression.
 * 
 * <p>List literals create new list objects with zero or more elements.
 * 
 * <p><b>Syntax:</b>
 * <pre>{@code
 * []                    // Empty list
 * [expr1, expr2, ...]   // List with elements
 * }</pre>
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * items = []                      // Empty list
 * numbers = [1, 2, 3]            // List with literals
 * ids = [INPUT.id1, INPUT.id2]   // List with expressions
 * mixed = [1, "hello", INPUT.x]  // Mixed types
 * }</pre>
 * 
 * <p>At runtime, creates a new mutable {@code ArrayList<Object>}.
 * 
 * @param elements List of expressions that will be evaluated to produce list elements
 */
public record ListLiteral(List<Expression> elements) implements Expression {
}
