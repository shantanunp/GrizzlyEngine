package com.grizzly.parser.ast;

public interface ASTVisitor<T> {
    T visit(ASTNode node);
}
