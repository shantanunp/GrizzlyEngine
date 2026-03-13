package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Function definition with optional default argument values.
 * Python-compliant: params with defaults must follow params without defaults.
 *
 * @param name Function name
 * @param params Parameter names (same order as defaultExprs)
 * @param defaultExprs Default expressions; null at index i means param i has no default
 * @param body Function body
 * @param lineNumber Line number in source
 */
public record FunctionDef(String name, List<String> params, List<Expression> defaultExprs,
                         List<Statement> body, int lineNumber) implements ASTNode {}
