package com.grizzly.core.types;

import com.grizzly.core.interpreter.CallableInvoker;
import com.grizzly.core.interpreter.GrizzlyInterpreter.BuiltinFunction;

import java.util.List;
import java.util.Map;

/**
 * Wraps a builtin function so it can be stored in context (e.g. from decimal import Decimal).
 * Python-compatible: allows calling imported callables like Decimal("1.5").
 */
public record CallableValue(BuiltinFunction fn) implements Value {

    @Override
    public String typeName() {
        return "callable";
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    public Value call(List<Value> args, Map<String, Value> keywordArgs, CallableInvoker invoker) {
        return fn.apply(args, keywordArgs != null ? keywordArgs : Map.of(), invoker);
    }
}
