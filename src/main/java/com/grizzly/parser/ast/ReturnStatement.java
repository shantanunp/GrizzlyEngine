package com.grizzly.parser.ast;

public record ReturnStatement(Expression value, int lineNumber) implements Statement {
    // Record automatically provides lineNumber() accessor
}
