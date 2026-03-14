package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Match statement (Python 3.10+ style): match an expression against case values
 * and execute the first matching block. Uses standard Python syntax.
 *
 * <p><b>Syntax:</b>
 * <pre>{@code
 * match expr:
 *     case value1:
 *         statements
 *     case value2:
 *         statements
 *     case _:
 *         statements   # default/wildcard
 * }</pre>
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * match status_code:
 *     case 200:
 *         return "OK"
 *     case 404:
 *         return "Not Found"
 *     case _:
 *         return "Unknown"
 * }</pre>
 *
 * @param expression   Expression to match (e.g. variable or expression)
 * @param caseBranches List of case value + block pairs
 * @param defaultBlock Statements for {@code case _} (optional, can be null or empty)
 * @param lineNumber   Source line number
 */
public record SwitchStatement(
    Expression expression,
    List<CaseBranch> caseBranches,
    List<Statement> defaultBlock,
    int lineNumber
) implements Statement {

    /**
     * A single case: value to match and statements to run when matched.
     */
    public record CaseBranch(Expression value, List<Statement> statements) {
    }
}
