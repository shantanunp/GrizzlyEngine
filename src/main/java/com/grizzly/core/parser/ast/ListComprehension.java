package com.grizzly.core.parser.ast;

/**
 * List comprehension: [ expr for var in iterable ]
 * Example: [ x * 2 for x in items ]
 */
public record ListComprehension(Expression element, String variable, Expression iterable) implements Expression {}
