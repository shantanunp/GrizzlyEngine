package com.grizzly.parser.ast;

/**
 * Break statement - exits the innermost enclosing loop.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * for item in items:
 *     if item.id == searchId:
 *         break  // Exit loop immediately
 * }</pre>
 * 
 * <p><b>Nested loops - only breaks inner loop:</b>
 * <pre>{@code
 * for dept in departments:
 *     for emp in dept.employees:
 *         if emp.salary > 100000:
 *             break  // Only exits inner loop (employees)
 *     // Continues here with next department
 * }</pre>
 * 
 * @param lineNumber Source line number for error reporting
 */
public record BreakStatement(int lineNumber) implements Statement {
}
