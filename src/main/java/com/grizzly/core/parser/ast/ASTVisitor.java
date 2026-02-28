package com.grizzly.core.parser.ast;

public interface ASTVisitor<T> {
    T visit(ASTNode node);
}
