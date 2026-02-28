package com.grizzly.core.parser.ast;

public record Assignment(Expression target, Expression value, int lineNumber) implements Statement {
}
