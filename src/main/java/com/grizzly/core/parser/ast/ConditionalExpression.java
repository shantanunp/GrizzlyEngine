package com.grizzly.core.parser.ast;

/**
 * Python-style conditional (ternary) expression: {@code then_expr if condition else else_expr}.
 * Example: {@code x if cond else y}
 */
public record ConditionalExpression(Expression thenExpr, Expression condition, Expression elseExpr) implements Expression {}
