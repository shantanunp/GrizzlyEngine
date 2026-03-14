package com.grizzly.core.interpreter;

import com.grizzly.core.types.Value;

import java.util.List;
import java.util.Map;

/**
 * Functional interface for invoking a callable (function or lambda) with arguments.
 * Used by list.sort(key=...) and sorted(key=...) to apply the key function.
 */
@FunctionalInterface
public interface CallableInvoker {
    Value invoke(Value callable, List<Value> args, Map<String, Value> keywordArgs);
}
