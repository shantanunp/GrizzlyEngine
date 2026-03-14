package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Lambda expression: lambda params: expr
 * Python-compliant: single expression body, no defaults, no *args.
 *
 * @param params Parameter names
 * @param body Single expression (the return value)
 */
public record LambdaExpression(List<String> params, Expression body) implements Expression {}
