package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Function call as statement: print(x), helper(INPUT)
 *
 * @param arguments List of CallArgument (Positional, Starred, Keyword, DoubleStarred)
 */
public record FunctionCall(
    String functionName,
    List<CallArgument> arguments,
    int lineNumber
) implements Statement {
}
