package com.grizzly.core.parser.ast;
public record AttrAccess(Expression object, String attr) implements Expression {}
