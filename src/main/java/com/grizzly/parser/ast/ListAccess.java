package com.grizzly.parser.ast;

/**
 * List access expression: items[0] or items[i]
 * 
 * Example:
 *   firstItem = items[0]
 *   item = items[index]
 */
public record ListAccess(Expression list, Expression index) implements Expression {
}
