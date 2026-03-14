package com.grizzly.core.parser.ast;

/**
 * Represents a single argument in a function/method call.
 * Supports Python's positional, *unpack, keyword, and **unpack.
 *
 * <p>Order in call: positional (including *x), keyword, **dict
 */
public sealed interface CallArgument permits
    CallArgument.Positional,
    CallArgument.Starred,
    CallArgument.Keyword,
    CallArgument.DoubleStarred {

    /** Normal positional argument: f(expr) */
    record Positional(Expression expr) implements CallArgument {}

    /** Unpack iterable as positionals: f(*list) */
    record Starred(Expression expr) implements CallArgument {}

    /** Keyword argument: f(name=expr) */
    record Keyword(String name, Expression expr) implements CallArgument {}

    /** Unpack dict as keywords: f(**dict) */
    record DoubleStarred(Expression expr) implements CallArgument {}
}
