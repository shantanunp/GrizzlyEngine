package com.grizzly.parser.ast;
public record StringLiteral(String value) implements Expression {
    @Override
    public String toString() { return "\"" + value + "\""; }
}
