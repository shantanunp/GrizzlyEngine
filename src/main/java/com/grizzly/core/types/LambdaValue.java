package com.grizzly.core.types;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.interpreter.ExecutionContext;
import com.grizzly.core.parser.ast.Expression;
import com.grizzly.core.parser.ast.LambdaExpression;

import java.util.List;
import java.util.Map;

/**
 * Runtime value for a lambda expression.
 * Captures closure (defining scope) and executes body with params bound.
 */
public record LambdaValue(LambdaExpression lambda, ExecutionContext closure) implements Value {

    @Override
    public String typeName() {
        return "function";
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    /**
     * Call the lambda with given arguments.
     * Python-compliant: positional first, then keyword.
     */
    public Value call(List<Value> args, Map<String, Value> keywordArgs,
                     com.grizzly.core.interpreter.LambdaInvoker invoker) {
        List<String> params = lambda.params();
        Map<String, Value> kw = keywordArgs != null ? keywordArgs : Map.of();
        ExecutionContext callContext = closure.createChild();
        java.util.Map<String, Value> bound = new java.util.HashMap<>();
        for (int i = 0; i < args.size(); i++) {
            if (i >= params.size()) {
                throw new GrizzlyExecutionException(
                    "lambda got " + args.size() + " positional arguments but takes " + params.size()
                );
            }
            bound.put(params.get(i), args.get(i));
        }
        for (java.util.Map.Entry<String, Value> e : kw.entrySet()) {
            if (!params.contains(e.getKey())) {
                throw new GrizzlyExecutionException(
                    "lambda got an unexpected keyword argument '" + e.getKey() + "'"
                );
            }
            if (bound.containsKey(e.getKey())) {
                throw new GrizzlyExecutionException(
                    "lambda got multiple values for argument '" + e.getKey() + "'"
                );
            }
            bound.put(e.getKey(), e.getValue());
        }
        for (String p : params) {
            if (!bound.containsKey(p)) {
                throw new GrizzlyExecutionException(
                    "lambda missing required argument: '" + p + "'"
                );
            }
            callContext.set(p, bound.get(p));
        }
        return invoker.evaluate(lambda.body(), callContext);
    }
}
