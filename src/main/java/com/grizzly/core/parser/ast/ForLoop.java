package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * For loop statement: for item in items:
 * 
 * Example:
 *   for customer in INPUT.customers:
 *       user = {}
 *       user["id"] = customer.id
 *       OUTPUT["users"].append(user)
 */
/**
 * AST node representing a for loop statement.
 * 
 * <p>A for loop iterates over an iterable expression (typically a list) and executes
 * a block of statements for each element.
 * 
 * <p><b>Syntax:</b>
 * <pre>{@code
 * for <variable> in <iterable>:
 *     <body>
 * }</pre>
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * // Simple iteration
 * for customer in INPUT.customers:
 *     OUTPUT["names"].append(customer.name)
 * 
 * // Nested loop
 * for dept in INPUT.departments:
 *     for emp in dept.employees:
 *         OUTPUT["all"].append(emp.name)
 * }</pre>
 * 
 * @param variable Name of the loop variable (e.g., "customer")
 * @param iterable Expression that produces a list to iterate over
 * @param body Statements to execute for each iteration
 * @param lineNumber Source line number for error reporting
 */
public record ForLoop(
    String variable,           // Loop variable name (e.g., "customer")
    Expression iterable,       // Expression to iterate over (e.g., INPUT.customers)
    List<Statement> body,      // Loop body statements
    int lineNumber
) implements Statement {
}
