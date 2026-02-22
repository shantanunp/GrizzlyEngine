package com.grizzly.parser.ast;

public record Assignment(Expression target, Expression value, int lineNumber) implements Statement {
}
