package com.grizzly.core.parser.ast;

/**
 * Continue statement - skips to next iteration of the innermost loop.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * for num in range(10):
 *     if num % 2 == 0:
 *         continue  // Skip even numbers
 *     OUTPUT["odds"].append(num)
 * // Result: [1, 3, 5, 7, 9]
 * }</pre>
 * 
 * <p><b>How it works:</b>
 * <ol>
 *   <li>Encounter continue</li>
 *   <li>Skip remaining statements in current iteration</li>
 *   <li>Jump to next item in loop</li>
 * </ol>
 * 
 * @param lineNumber Source line number for error reporting
 */
public record ContinueStatement(int lineNumber) implements Statement {
}
