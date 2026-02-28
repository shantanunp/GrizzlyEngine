package com.grizzly.core.parser.ast;

import java.util.List;

public record FunctionCall(String functionName, List<Expression> args, int lineNumber) implements Statement {
}
