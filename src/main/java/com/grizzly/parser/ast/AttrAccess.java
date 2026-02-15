package com.grizzly.parser.ast;
public record AttrAccess(Expression object, String attr) implements Expression {}
