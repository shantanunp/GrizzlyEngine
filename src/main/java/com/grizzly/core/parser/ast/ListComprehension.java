package com.grizzly.core.parser.ast;

/**
 * List comprehension: [ expr for var in iterable ] or [ expr for var in iterable if condition ]
 * Example: [ x * 2 for x in items ], [ num for num in numbers if num % 2 == 0 ]
 */
public record ListComprehension(Expression element, String variable, Expression iterable, Expression condition) implements Expression {

    /** Condition may be null (no filter). */
    public ListComprehension(Expression element, String variable, Expression iterable) {
        this(element, variable, iterable, null);
    }
}
