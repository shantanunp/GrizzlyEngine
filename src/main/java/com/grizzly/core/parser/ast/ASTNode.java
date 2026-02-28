package com.grizzly.core.parser.ast;

public interface ASTNode {
    default <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
