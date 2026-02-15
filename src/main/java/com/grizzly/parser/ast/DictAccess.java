package com.grizzly.parser.ast;
public record DictAccess(Expression object, Expression key) implements Expression {}
