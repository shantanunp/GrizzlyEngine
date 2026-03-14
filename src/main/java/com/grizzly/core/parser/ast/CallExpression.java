package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Call expression where callee is any expression: (lambda x: x+1)(5), f(1) when f is a variable.
 * Used for expr(args) when expr is not a bare identifier (which uses FunctionCallExpression).
 */
public record CallExpression(Expression callee, List<CallArgument> arguments) implements Expression {}
