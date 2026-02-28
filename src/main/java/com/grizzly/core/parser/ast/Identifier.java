package com.grizzly.core.parser.ast;
public record Identifier(String name) implements Expression {
    @Override
    public String toString() { return name; }
}
