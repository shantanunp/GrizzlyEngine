package com.grizzly.core.parser.ast;

/**
 * Base interface for all statements
 * Using record-style accessor (lineNumber instead of getLineNumber)
 */
public interface Statement extends ASTNode {
    int lineNumber();
}
