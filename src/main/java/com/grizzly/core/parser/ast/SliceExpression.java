package com.grizzly.core.parser.ast;

/**
 * Python slice expression: {@code obj[start:end]} or {@code obj[start:end:step]}
 *
 * <p>Examples:
 * <pre>{@code
 * s[1:3]      // start=1, end=3, step=null
 * s[:3]       // start=null, end=3, step=null
 * s[1:]       // start=1, end=null, step=null
 * s[::2]      // start=null, end=null, step=2
 * s[1:5:2]    // start=1, end=5, step=2
 * }</pre>
 *
 * @param object The object being sliced (string or list)
 * @param start  Start index (null = 0 for positive step, len-1 for negative step)
 * @param end    End index (null = len for positive step, -len-1 for negative step)
 * @param step   Step (null = 1)
 * @param safe   True if using safe navigation (?[)
 */
public record SliceExpression(
    Expression object,
    Expression start,
    Expression end,
    Expression step,
    boolean safe
) implements Expression {

    public SliceExpression(Expression object, Expression start, Expression end, Expression step) {
        this(object, start, end, step, false);
    }
}
