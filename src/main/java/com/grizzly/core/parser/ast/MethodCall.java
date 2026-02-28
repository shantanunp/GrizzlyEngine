package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Method call expression: object.method(args)
 * 
 * Example:
 *   items.append(value)
 *   text.upper()
 */
/**
 * Method call expression: {@code object.method(args)}
 * 
 * <p>Represents calling a method on an object.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * items.append(value)              // Call append on list
 * text.upper()                     // Call upper on string
 * OUTPUT["items"].append(user)     // Chained access + method call
 * }</pre>
 * 
 * <p><b>Supported types:</b>
 * <ul>
 *   <li>Lists: {@code append(value)}, {@code extend(list)}</li>
 *   <li>Strings: {@code upper()}, {@code lower()}, {@code strip()}</li>
 * </ul>
 * 
 * @param object Expression that evaluates to the object
 * @param methodName Name of the method to call
 * @param arguments List of argument expressions
 */
public record MethodCall(Expression object, String methodName, List<Expression> arguments) implements Expression {
}
