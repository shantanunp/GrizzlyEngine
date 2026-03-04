package com.grizzly.core.parser.ast;

/**
 * Dict comprehension: { key_expr: value_expr for var in iterable } or with optional "if condition".
 * Example: { name: len(name) for name in names }
 */
public record DictComprehension(Expression keyExpr, Expression valueExpr, String variable, Expression iterable, Expression condition) implements Expression {

    /** Condition may be null (no filter). */
    public DictComprehension(Expression keyExpr, Expression valueExpr, String variable, Expression iterable) {
        this(keyExpr, valueExpr, variable, iterable, null);
    }
}
