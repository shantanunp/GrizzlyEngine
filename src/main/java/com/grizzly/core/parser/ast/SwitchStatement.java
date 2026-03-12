package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Switch statement: match an expression against case values and execute the first matching block.
 *
 * <p><b>Syntax:</b>
 * <pre>{@code
 * switch expr:
 *     case value1:
 *         statements
 *     case value2:
 *         statements
 *     default:
 *         statements
 * }</pre>
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * switch status:
 *     case "active":
 *         OUTPUT["code"] = 1
 *     case "pending":
 *         OUTPUT["code"] = 2
 *     default:
 *         OUTPUT["code"] = 0
 * }</pre>
 *
 * @param expression   Expression to match (e.g. variable or expression)
 * @param caseBranches List of case value + block pairs
 * @param defaultBlock Statements for default (optional, can be null or empty)
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
