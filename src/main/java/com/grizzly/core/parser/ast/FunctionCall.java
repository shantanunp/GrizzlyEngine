package com.grizzly.core.parser.ast;

import java.util.List;
import java.util.Map;

public record FunctionCall(
    String functionName,
    List<Expression> args,
    Map<String, Expression> keywordArgs,
    int lineNumber
) implements Statement {
}
