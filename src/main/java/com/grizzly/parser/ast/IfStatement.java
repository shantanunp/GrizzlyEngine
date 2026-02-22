package com.grizzly.parser.ast;

import java.util.List;

public record IfStatement(Expression condition, List<Statement> thenBlock, List<Statement> elseBlock, int lineNumber) implements Statement {
}
