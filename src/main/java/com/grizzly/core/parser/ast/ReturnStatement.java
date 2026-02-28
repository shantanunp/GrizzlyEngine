package com.grizzly.core.parser.ast;

public record ReturnStatement(Expression value, int lineNumber) implements Statement {
}
