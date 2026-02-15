package com.grizzly.parser.ast;
public record BinaryOp(Expression left, String operator, Expression right) implements Expression {}
