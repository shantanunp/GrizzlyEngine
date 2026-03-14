package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Function definition with optional default values, *args, and keyword-only params.
 * Python-compliant:
 * - params with defaults must follow params without defaults
 * - *param collects remaining positional args into a list
 * - params after *param are keyword-only (must be passed by name)
 *
 * @param name Function name
 * @param params Parameter names (same order as defaultExprs)
 * @param defaultExprs Default expressions; null at index i means param i has no default
 * @param starParamIndex Index of *args param, or -1 if none
 * @param body Function body
 * @param lineNumber Line number in source
 */
public record FunctionDef(String name, List<String> params, List<Expression> defaultExprs,
                         int starParamIndex, List<Statement> body, int lineNumber) implements ASTNode {}
