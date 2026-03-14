package com.grizzly.core.interpreter;

import com.grizzly.core.parser.ast.Expression;
import com.grizzly.core.types.Value;

/**
 * Interface for evaluating lambda body expressions.
 * Allows LambdaValue to delegate back to the interpreter.
 */
@FunctionalInterface
public interface LambdaInvoker {
    Value evaluate(Expression body, ExecutionContext context);
}
