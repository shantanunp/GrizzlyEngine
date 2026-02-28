package com.grizzly.core.parser.ast;

public record Assignment(Expression target, Expression value, int lineNumber) implements Statement {
    // Record automatically provides lineNumber() accessor which satisfies the interface
}
