package com.grizzly.parser.ast;
public record DictLiteral() implements Expression {
    @Override
    public String toString() { return "{}"; }
}
