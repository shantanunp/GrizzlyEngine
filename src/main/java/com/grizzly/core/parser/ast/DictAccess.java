package com.grizzly.core.parser.ast;
public record DictAccess(Expression object, Expression key) implements Expression {}
